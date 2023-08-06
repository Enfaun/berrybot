package net.enfau.berry.web

import dagger.Module
import dagger.Provides
import org.briarproject.bramble.api.event.EventBus
import javax.inject.Singleton

@Module
class BotContactModule {

    @Provides
    @Singleton
    internal fun provideIContactController(
        eventBus: EventBus,
        contactController: ContactControllerImpl
    ): ContactController {
        eventBus.addListener(contactController)
        return contactController
    }

}
