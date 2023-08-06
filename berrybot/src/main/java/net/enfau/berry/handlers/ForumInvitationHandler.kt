package net.enfau.berry.handlers

import net.enfau.berry.dao.PermissionDao
import net.enfau.berry.dao.PermissionFlag
import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.db.DatabaseComponent
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.identity.AuthorId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.conversation.ConversationManager
import org.briarproject.briar.api.forum.ForumSharingManager
import org.briarproject.briar.api.forum.event.ForumInvitationRequestReceivedEvent
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
class ForumInvitationHandler
@Inject
constructor(
    private val conversationManager: ConversationManager,
    private val forumSharingManager: ForumSharingManager,
    private val contactManager: ContactManager,
    private val messagingManager: MessagingManager,
    private val privateMessageFactory: PrivateMessageFactory,
    private val databaseComponent: DatabaseComponent,
    private val clock: Clock,
    private val permissionDao: PermissionDao
): EventListener, WithLogger {
    private val logger = logger()
    private val admins = ArrayList<AuthorId>()

    override fun eventOccurred(e: Event) {
        when(e) {
            is ForumInvitationRequestReceivedEvent -> {

                val e: ForumInvitationRequestReceivedEvent = e
                val author = contactManager.getContact(e.contactId).author.id
                var accept = permissionDao.get(author).has(PermissionFlag.ADD_FORUM)
                forumSharingManager.respondToInvitation(e.contactId, e.messageHeader.sessionId, accept)
                conversationManager.setReadFlag(e.messageHeader.groupId, e.messageHeader.id, true)
                if(!accept) {
                    val msg = privateMessageFactory.createLegacyPrivateMessage(
                        e.messageHeader.groupId,
                        clock.currentTimeMillis(),
                        "You are not authorized to invite this bot."
                    )
                    messagingManager.addLocalMessage(msg)
                }
            }
        }
    }
}
