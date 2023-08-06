package net.enfau.berry.commands

import org.briarproject.bramble.api.event.EventBus
import java.util.logging.Logger
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton


@Immutable
@Singleton
internal class CommandsModule
@Inject
constructor(
    private val eventBus: EventBus,
    private val forumCommand: ForumCommand,
    private val identityCommand: IdentityCommand,
    private val utilityCommand: UtilityCommand,
    private val helpCommand: HelpCommand,
    private val permissionCommand: PermissionCommand,
    private val manageCommand: ManageCommand,
    private val helpRegisterer: HelpCommand
) {
    private val logger = Logger.getLogger(this::javaClass.name)
    internal fun register() {
        logger.info("Registering commands")
        eventBus.addListener(forumCommand)
        eventBus.addListener(identityCommand)
        eventBus.addListener(utilityCommand)
        eventBus.addListener(helpCommand)
        eventBus.addListener(permissionCommand)
        eventBus.addListener(manageCommand)
        helpRegisterer.register(forumCommand::class.java)
        helpRegisterer.register(identityCommand::class.java)
        helpRegisterer.register(utilityCommand::class.java)
        helpRegisterer.register(helpCommand::class.java)
        helpRegisterer.register(permissionCommand::class.java)
        helpRegisterer.register(manageCommand::class.java)
    }
}
