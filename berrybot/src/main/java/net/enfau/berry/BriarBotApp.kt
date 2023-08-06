package net.enfau.berry

import dagger.Component
import net.enfau.berry.commands.CommandsModule
import net.enfau.berry.handlers.EventHandlers
import org.briarproject.bramble.BrambleCoreEagerSingletons
import org.briarproject.bramble.BrambleCoreModule
import org.briarproject.bramble.BrambleJavaEagerSingletons
import org.briarproject.bramble.BrambleJavaModule
import org.briarproject.briar.BriarCoreEagerSingletons
import org.briarproject.briar.BriarCoreModule
import javax.inject.Singleton

@Component(
    modules = [
        BrambleCoreModule::class,
        BrambleJavaModule::class,
        BriarCoreModule::class,
        BotModule::class
    ]
)
@Singleton
internal interface BriarBotApp : BrambleCoreEagerSingletons, BriarCoreEagerSingletons,
    BrambleJavaEagerSingletons, BotEagerSingletons {
    fun getBot(): Bot

    fun getRouter(): Router

    fun getCommands(): CommandsModule

    fun getEventHandlers(): EventHandlers
}
