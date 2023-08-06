package net.enfau.berry.handlers

import io.javalin.plugin.bundled.RouteOverviewUtil.metaInfo
import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.contact.event.ContactAddedEvent
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.sync.Priority
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.client.MessageTracker
import org.briarproject.briar.api.conversation.ConversationManager
import org.briarproject.briar.api.forum.ForumManager
import org.briarproject.briar.api.forum.ForumSharingManager
import org.briarproject.briar.api.forum.event.ForumInvitationResponseReceivedEvent
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log

@Immutable
@Singleton
class ForumInvitationResponseHandler
@Inject
constructor(
    private val messagingManager: MessagingManager,
    private val conversationManager: ConversationManager,
    private val forumManager: ForumManager,
    private val forumSharingManager: ForumSharingManager,
    private val contactManager: ContactManager,
    private val messageTracker: MessageTracker,
    private val privateMessageFactory: PrivateMessageFactory,
    private val clock: Clock
): EventListener, WithLogger {
    private val logger = logger()

    override fun eventOccurred(e: Event) {
        when(e) {
            is ForumInvitationResponseReceivedEvent -> {
                val e: ForumInvitationResponseReceivedEvent = e
                if(!e.messageHeader.wasAccepted()) return
                val groupId = messagingManager.getConversationId(e.contactId)

                val msg = privateMessageFactory.createLegacyPrivateMessage(
                    groupId,
                    clock.currentTimeMillis(),
                    """
                        Tip: After joining a forum, try to ask people in the forum that are interested to add you as contact.
                        After that you can share the forum with them, making Briar truly decentralized!
                    """.trimIndent()
                )
                messagingManager.addLocalMessage(msg)
            }
        }
    }
}
