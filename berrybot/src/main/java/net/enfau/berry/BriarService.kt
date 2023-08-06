package net.enfau.berry

import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.TermUi.echo
import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.account.AccountManager
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.crypto.DecryptionException
import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator
import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator.QUITE_WEAK
import org.briarproject.bramble.api.lifecycle.LifecycleManager
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

interface BriarService {
    fun start(name: String, password: String)
    fun resetAccount()
    fun stop()
}

@Immutable
@Singleton
internal class BriarServiceImpl
@Inject
constructor(
    private val accountManager: AccountManager,
    private val lifecycleManager: LifecycleManager,
    private val contactManager: ContactManager,
    private val passwordStrengthEstimator: PasswordStrengthEstimator
) : BriarService, WithLogger {
    val logger = logger()

    override fun start(name: String, password: String) {
        if (!accountManager.accountExists()) {
            createAccount(name, password)
        } else {
            try {
                accountManager.signIn(password)
            } catch (e: DecryptionException) {
                echo("Error: Password invalid")
                exitProcess(1)
            }
        }
        val dbKey = accountManager.databaseKey ?: throw AssertionError()
        lifecycleManager.startServices(dbKey)
        lifecycleManager.waitForStartup()
        logger.info("Bot handshake URL: ${contactManager.handshakeLink}")
    }

    override fun resetAccount() {
        if(accountManager.accountExists())
            accountManager.deleteAccount()
    }

    override fun stop() {
        lifecycleManager.stopServices()
        lifecycleManager.waitForShutdown()
    }

    private fun createAccount(name: String, password: String) {
        if (passwordStrengthEstimator.estimateStrength(password) < QUITE_WEAK)
            throw UsageError("Please enter a stronger password!")
        accountManager.createAccount(name, password)
    }

}
