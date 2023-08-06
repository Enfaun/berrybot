package net.enfau.berry.web

import io.javalin.http.Context
import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.FormatException
import org.briarproject.bramble.api.connection.ConnectionRegistry
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.contact.event.PendingContactAddedEvent
import org.briarproject.bramble.api.contact.event.PendingContactStateChangedEvent
import org.briarproject.bramble.api.db.ContactExistsException
import org.briarproject.bramble.api.db.PendingContactExistsException
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.briar.api.conversation.ConversationManager
import org.eclipse.jetty.http.HttpStatus.*
import java.security.GeneralSecurityException
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
internal class ContactControllerImpl
@Inject
constructor(
    private val contactManager: ContactManager,
    private val conversationManager: ConversationManager,
    private val connectionRegistry: ConnectionRegistry
): EventListener, ContactController, WithLogger {
    val logger = logger()

    override fun eventOccurred(e: Event) = when (e) {
        is PendingContactAddedEvent -> {
            logger.debug("New pending contact: ${e.pendingContact.id} ${e.toString()}")

        }

        is PendingContactStateChangedEvent -> {
            logger.debug("Pending contact state changed: ${e.pendingContactState.name} ${e.toString()}")
        }

        else -> {}
    }

    override fun addContact(ctx : Context, token: String) {
        val link = ctx.formParam("link")
        if(ctx.header("Authorization")!="Bearer $token") {
            ctx.status(UNAUTHORIZED_401)
            ctx.html("Unauthorized")
            return
        }
        val pendingContact = try {
            contactManager.addPendingContact(link, link.hashCode().toString())
        } catch (e: FormatException) {
            ctx.status(BAD_REQUEST_400)
            ctx.html( "Invalid link")
            return
        } catch (e: GeneralSecurityException) {
            ctx.status(BAD_REQUEST_400)
            ctx.html( "Invalid link")
            return
        } catch (e: ContactExistsException) {
            ctx.status(FORBIDDEN_403)
            ctx.html("Contact already added")
            return
        } catch (e: PendingContactExistsException) {
            contactManager.removePendingContact(e.pendingContact.id)
            ctx.status(GONE_410)
            ctx.html("Pending contact removed")
            return
        } catch (e: Exception) {
            logger.error("Error on adding contact", e)
            ctx.status(INTERNAL_SERVER_ERROR_500)
            ctx.html("Internal server error")
            return

        ctx.status(CREATED_201)}
        ctx.html("Contact added")
    }

    override fun getLink(ctx: Context) {
        ctx.html(contactManager.handshakeLink)
    }
}
