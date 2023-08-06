package net.enfau.berry.handlers

import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.event.EventBus
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton


@Immutable
@Singleton
internal class EventHandlers
@Inject
constructor(
    private val eventBus: EventBus,
    private val contactAddedHandler: ContactAddedHandler,
    private val fallbackEventHandler: FallbackEventHandler,
    private val forumInvitationHandler: ForumInvitationHandler,
    private val forumInvitationResponseHandler: ForumInvitationResponseHandler,
    private val forumPostReceivedHandler: ForumPostReceivedHandler,
    private val introductionHandler: IntroductionHandler,
    private val contactConnectionHandler: ContactConnectionHandler
): WithLogger {
    private val logger = logger()
    internal fun register() {
        logger.info("Registering event handlers")
        eventBus.addListener(contactAddedHandler)
        eventBus.addListener(fallbackEventHandler)
        eventBus.addListener(forumInvitationHandler)
        eventBus.addListener(forumInvitationResponseHandler)
        eventBus.addListener(forumPostReceivedHandler)
        eventBus.addListener(introductionHandler)
        eventBus.addListener(contactConnectionHandler)
    }
}
