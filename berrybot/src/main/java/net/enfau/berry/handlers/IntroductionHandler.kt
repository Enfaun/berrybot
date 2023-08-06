package net.enfau.berry.handlers

import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.db.TransactionManager
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.sync.MessageId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.conversation.ConversationManager
import org.briarproject.briar.api.forum.ForumSharingManager
import org.briarproject.briar.api.introduction.IntroductionManager
import org.briarproject.briar.api.introduction.event.IntroductionRequestReceivedEvent
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
class IntroductionHandler
@Inject
constructor(
    private val conversationManager: ConversationManager,
    private val forumSharingManager: ForumSharingManager,
    private val contactManager: ContactManager,
    private val messagingManager: MessagingManager,
    private val privateMessageFactory: PrivateMessageFactory,
    private val introductionManager: IntroductionManager,
    private val transactionManager: TransactionManager,
    private val clock: Clock
): EventListener, WithLogger {
    private val logger = logger()

    override fun eventOccurred(e: Event) {
        when(e) {

            is IntroductionRequestReceivedEvent -> {
                val e: IntroductionRequestReceivedEvent = e
                val g = messagingManager.getConversationId(e.contactId)
                val set = HashSet<MessageId>()
                set.add(e.messageHeader.id)
//                transactionManager.transactionWithResult<DeletionResult, DbException>(false) {
//                    introductionManager.deleteMessages(it, e.contactId, set)
//                }
                introductionManager.respondToIntroduction(e.contactId, e.messageHeader.sessionId, false)
                val body = """
                    Introducing bot is not allowed, please share the bot link instead.
                """.trimIndent()
                val m = privateMessageFactory.createLegacyPrivateMessage(
                    g, clock.currentTimeMillis(), body)
                messagingManager.addLocalMessage(m)
            }
        }
    }
}