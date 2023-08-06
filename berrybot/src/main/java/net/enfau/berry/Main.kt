package net.enfau.berry

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import org.briarproject.bramble.BrambleCoreEagerSingletons
import org.briarproject.bramble.BrambleJavaEagerSingletons
import org.briarproject.bramble.util.OsUtils.isLinux
import org.briarproject.bramble.util.OsUtils.isMac
import org.briarproject.briar.BriarCoreEagerSingletons
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.lang.System.getProperty
import java.lang.System.setProperty
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.logging.Level.ALL
import java.util.logging.Level.INFO
import java.util.logging.Level.WARNING
import java.util.logging.LogManager

private val DEFAULT_DATA_DIR = getProperty("user.home") + separator + ".berrybot"

private class Main : CliktCommand(
    name = "briar-headless",
    help = "A Briar peer without GUI that exposes a REST and Websocket API"
) {
    private val debug by option("--debug", "-d", help = "Enable printing of debug messages").flag(
        default = false
    )
    private val javalinDebug by option("--javalin-debug", help = "Enable printing of Javalin debug messages").flag(
        default = false
    )
    private val briarLog by option("--briar-log-level", "-B", help = "Set verbose level of Briar logs").counted()
    private val resetAccount by option("--reset-account", help = "Reset account information").flag(
        default = false
    )
    private val verbosity by option(
        "--verbose",
        "-v",
        help = "Print verbose log messages"
    ).counted()
    private val port by option(
        "--port",
        help = "Port of the server",
        metavar = "WEB_PORT",
        envvar = "DEW_WEB_PORT"
    ).int().default(8332)
    private val token by option(
        "--token",
        help = "Token of the server",
        metavar = "WEB_TOKEN",
        envvar = "DEW_WEB_TOKEN"
    ).required()
    private val botName by option(
        "--name",
        help = "Name of the Bot",
        metavar = "BOT_NAME",
        envvar = "DEW_BOT_NAME"
    ).required()
    private val botPassword by option(
        "--password",
        help = "Password of the Bot",
        metavar = "BOT_PASSWORD",
        envvar = "DEW_BOT_PASSWORD"
    ).required()
    private val dataDir by option(
        "--data-dir",
        help = "The directory where Briar will store its files. Default: $DEFAULT_DATA_DIR",
        metavar = "PATH",
        envvar = "BRIAR_DATA_DIR"
    ).default(DEFAULT_DATA_DIR)

    override fun run() {
        // logging
        val levelSlf4j = if (debug) "DEBUG" else when (verbosity) {
            0 -> "WARN"
            1 -> "INFO"
            else -> "DEBUG"
        }
        val level = if (debug) ALL else when (verbosity) {
            0 -> WARNING
            1 -> INFO
            else -> ALL
        }
        val briarLevel = when (briarLog) {
            0 -> WARNING
            1 -> INFO
            else -> ALL
        }

        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
        LogManager.getLogManager().getLogger("").level = INFO
        if(debug) setProperty(DewConstants.DEW_DEBUG_KEY, "yes")


        val dataDir = getDataDir()
        val app =
            DaggerBriarBotApp.builder().botModule(BotModule(dataDir)).build()
        // We need to load the eager singletons directly after making the
        // dependency graphs
        BrambleCoreEagerSingletons.Helper.injectEagerSingletons(app)
        BrambleJavaEagerSingletons.Helper.injectEagerSingletons(app)
        BriarCoreEagerSingletons.Helper.injectEagerSingletons(app)
        BotEagerSingletons.Helper.injectEagerSingletons(app)
        if(resetAccount) app.getRouter().resetAccount()

        app.getCommands().register()
        app.getEventHandlers().register()
        app.getBot().start(botName, botPassword)
        app.getRouter().start(port, token, javalinDebug)

    }

    private fun getDataDir(): File {
        val file = File(dataDir)
        if (!file.exists() && !file.mkdirs()) {
            throw IOException("Could not create directory: ${file.absolutePath}")
        } else if (!file.isDirectory) {
            throw IOException("Data dir is not a directory: ${file.absolutePath}")
        }
        if (isLinux() || isMac()) {
            val perms = HashSet<PosixFilePermission>()
            perms.add(OWNER_READ)
            perms.add(OWNER_WRITE)
            perms.add(OWNER_EXECUTE)
            setPosixFilePermissions(file.toPath(), perms)
        }
        return file
    }

}

fun main(args: Array<String>) = Main().main(args)
