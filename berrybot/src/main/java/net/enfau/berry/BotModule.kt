package net.enfau.berry

import dagger.Module
import dagger.Provides
import net.enfau.berry.dao.DaoModule
import net.enfau.berry.web.BotContactModule
import org.briarproject.bramble.account.AccountModule
import org.briarproject.bramble.api.FeatureFlags
import org.briarproject.bramble.api.db.DatabaseConfig
import org.briarproject.bramble.api.mailbox.MailboxDirectory
import org.briarproject.bramble.api.plugin.*
import org.briarproject.bramble.api.plugin.duplex.DuplexPluginFactory
import org.briarproject.bramble.api.plugin.simplex.SimplexPluginFactory
import org.briarproject.bramble.battery.DefaultBatteryManagerModule
import org.briarproject.bramble.event.DefaultEventExecutorModule
import org.briarproject.bramble.plugin.tor.MacTorPluginFactory
import org.briarproject.bramble.plugin.tor.UnixTorPluginFactory
import org.briarproject.bramble.plugin.tor.WindowsTorPluginFactory
import org.briarproject.bramble.system.*
import org.briarproject.bramble.util.OsUtils.*
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlite3.SQLitePlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Collections.emptyList
import javax.inject.Singleton

@Module(
    includes = [
        AccountModule::class,
        ClockModule::class,
        DefaultBatteryManagerModule::class,
        DefaultEventExecutorModule::class,
        DefaultTaskSchedulerModule::class,
        DefaultWakefulIoExecutorModule::class,
        DefaultThreadFactoryModule::class,
        DesktopSecureRandomModule::class,
        DaoModule::class,
        BotContactModule::class
    ]
)
internal class BotModule(private val appDir: File) {

    @Provides
    @Singleton
    internal fun provideBriarService(briarService: BriarServiceImpl): BriarService = briarService

    @Provides
    @Singleton
    internal fun provideDatabaseConfig(): DatabaseConfig {
        val dbDir = File(appDir, "db")
        val keyDir = File(appDir, "key")
        return BotDatabaseConfig(dbDir, keyDir)
    }

    @Provides
    @Singleton
    internal fun provideJdbi(): Jdbi {
        val dbfile = File(appDir, "berrybot.sqlite3")
        val jdbi = Jdbi.create("jdbc:sqlite:${dbfile.path}")
        jdbi.installPlugin(KotlinSqlObjectPlugin())
        jdbi.installPlugin(SQLitePlugin())
        return jdbi
    }

    @Provides
    @MailboxDirectory
    internal fun provideMailboxDirectory(): File {
        return File(appDir, "mailbox")
    }

    @Provides
    @TorDirectory
    internal fun provideTorDirectory(): File {
        return File(appDir, "tor")
    }

    @Provides
    internal fun provideLogger(): Logger {
        val logger = LoggerFactory.getLogger("")

        return logger
    }

    @Provides
    @TorSocksPort
    internal fun provideTorSocksPort(): Int = 29050

    @Provides
    @TorControlPort
    internal fun provideTorControlPort(): Int = 29051

    @Provides
    @Singleton
    internal fun providePluginConfig(
        unixTor: UnixTorPluginFactory,
        macTor: MacTorPluginFactory,
        winTor: WindowsTorPluginFactory
    ): PluginConfig {
        val duplex: List<DuplexPluginFactory> = when {
            isLinux() -> listOf(unixTor)
            isMac() -> listOf(macTor)
            isWindows() -> listOf(winTor)
            else -> emptyList()
        }
        return object : PluginConfig {
            override fun getDuplexFactories(): Collection<DuplexPluginFactory> = duplex
            override fun getSimplexFactories(): Collection<SimplexPluginFactory> = emptyList()
            override fun shouldPoll(): Boolean = true
            override fun getTransportPreferences(): Map<TransportId, List<TransportId>> = emptyMap()
        }
    }

    @Provides
    internal fun provideFeatureFlags() = object : FeatureFlags {
        override fun shouldEnableImageAttachments() = false
        override fun shouldEnableProfilePictures() = false
        override fun shouldEnableDisappearingMessages() = false
        override fun shouldEnablePrivateGroupsInCore() = false
        override fun shouldEnableForumsInCore() = true
        override fun shouldEnableBlogsInCore() = false
    }
}

