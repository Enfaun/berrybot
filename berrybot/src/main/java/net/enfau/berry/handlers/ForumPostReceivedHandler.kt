package net.enfau.berry.handlers

import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.contact.event.ContactAddedEvent
import org.briarproject.bramble.api.db.DatabaseComponent
import org.briarproject.bramble.api.db.DbException
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.forum.ForumManager
import org.briarproject.briar.api.forum.ForumPostHeader
import org.briarproject.briar.api.forum.event.ForumPostReceivedEvent
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
class ForumPostReceivedHandler
@Inject
constructor(
    private val messagingManager: MessagingManager,
    private val privateMessageFactory: PrivateMessageFactory,
    private val databaseComponent: DatabaseComponent,
    private val forumManager: ForumManager,
    private val clock: Clock
): EventListener, WithLogger {
    private val logger = logger()

    override fun eventOccurred(e: Event) {
        when (e) {
            is ForumPostReceivedEvent -> {
                val e: ForumPostReceivedEvent = e
                cleanupForumMessages(e.groupId)
            }

        }
    }
        private fun cleanupForumMessages(groupId: GroupId) {

            var allMessages = databaseComponent.transactionWithResult<List<ForumPostHeader>, DbException>(true) { txn ->
                forumManager.getPostHeaders(
                    txn,
                    groupId
                )

            }
            allMessages = allMessages.sortedBy { h -> h.timestamp }
//        val forum = forumManager.getForum(groupId)


//
//        for((k,v) in allMessages) {
//
//            logger.debug("Message: ${messagingManager.getMessageText(k)}")
//
//            for((k2,v2) in v) {
//                logger.debug("$k2 = $v2 -> ${StringUtils.toHexString(v2)}")
//            }
//        }
//
            var counter = 10
            if (allMessages.size < 15) return
            allMessages = allMessages.subList(0, allMessages.size - counter)
            logger.info("Cleaning up ${allMessages.size} messages of $groupId")

            databaseComponent.transaction<DbException>(false) { txn ->
                for (msg in allMessages) {
                    databaseComponent.removeMessage(txn, msg.id)
                }
            }
        }

    }
