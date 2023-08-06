package net.enfau.berry.handlers

import net.enfau.berry.dao.VariableDao
import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.contact.event.ContactAddedEvent
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
class ContactAddedHandler
@Inject
constructor(
    private val messagingManager: MessagingManager,
    private val privateMessageFactory: PrivateMessageFactory,
    private val clock: Clock,
    private val variableDao: VariableDao
): EventListener, WithLogger {
    private val logger = logger()

    override fun eventOccurred(e: Event) {
        when(e) {
            is ContactAddedEvent -> {
                val e: ContactAddedEvent = e
                val groupId = messagingManager.getConversationId(e.contactId)
                val link = variableDao.getOrDefault("landingLink", "(link not set)")
                val msg = privateMessageFactory.createLegacyPrivateMessage(
                    groupId,
                    clock.currentTimeMillis(),
                    """
                        Thank you for adding BerryBot!
                        This bot is a forum indexer and user introduction agent for Briar.
                        
                        to find forums, you can list forums with /list, additionally with page count like /list 2
                        
                        to find people to add, you can run /lookup PUTALIASHERE
                        to set an alias, run /setalias and supply your desired alias, additional numbers will be appended.
                        to remove alias, run /rmalias
                        
                        More commands are available in /help
                        
                        Please note that this bot is a centralized sync point, where it might pose security risk for your safety.
                        To make bot disconnect from you, use the /disconnect command to do so.
                        You will have to associate your briar link again on $link
                    """.trimIndent()
                )
                messagingManager.addLocalMessage(msg)
            }
        }
    }
}
