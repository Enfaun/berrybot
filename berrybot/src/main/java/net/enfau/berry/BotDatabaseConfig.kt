package net.enfau.berry

import org.briarproject.bramble.api.crypto.KeyStrengthener
import org.briarproject.bramble.api.db.DatabaseConfig
import java.io.File

internal class BotDatabaseConfig(private val dbDir: File, private val keyDir: File) :
    DatabaseConfig {

    override fun getDatabaseDirectory() = dbDir

    override fun getDatabaseKeyDirectory() = keyDir

    override fun getKeyStrengthener(): KeyStrengthener? = null
}
