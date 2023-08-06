package net.enfau.berry.handlers

import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.plugin.event.ContactConnectedEvent
import org.briarproject.bramble.api.plugin.event.ContactDisconnectedEvent
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
class ContactConnectionHandler @Inject constructor() : EventListener, WithLogger {
    private val logger = logger()
    var connectedContacts = 0
    private set

    override fun eventOccurred(e: Event) {
        when(e) {
            is ContactConnectedEvent -> {
                connectedContacts++
                logger.debug("Connected contacts: $connectedContacts")

            }
            is ContactDisconnectedEvent -> {
                connectedContacts--
                logger.debug("Connected contacts: $connectedContacts")
            }
        }
    }
}