package net.enfau.berry

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.staticfiles.Location
import net.enfau.berry.web.ContactController
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import java.io.File
import java.lang.Runtime.getRuntime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger.getLogger
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess


@Immutable
@Singleton
internal class Router
@Inject
constructor(
    private val briarService: BriarService,
    private val contactController: ContactController
) {

    private val logger = getLogger(Router::javaClass.name)
    private val stopped = AtomicBoolean(false)

    internal fun resetAccount() {
        briarService.resetAccount()
    }

    private fun fileSessionHandler() = SessionHandler().apply {
        sessionCache = DefaultSessionCache(this).apply {
            sessionDataStore = FileSessionDataStore().apply {
                val baseDir = File(System.getProperty("java.io.tmpdir"))
                this.storeDir = File(baseDir, "javalin-session-store").apply { mkdir() }
            }
        }
        httpOnly = true
        // make additional changes to your SessionHandler here
    }

    internal fun start(port: Int, token: String, debug: Boolean): Javalin {
        getRuntime().addShutdownHook(Thread(this::stop))

        val app = Javalin.create {
            it.showJavalinBanner = true
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.jetty.sessionHandler { fileSessionHandler() }

        }.events { event ->
            event.serverStartFailed { serverStopped() }
            event.serverStopped { serverStopped() }
        }



        app.routes {
            path("/link") {
                get { ctx -> contactController.getLink(ctx) }
            }
            path("/add") {
                post { ctx ->
                    contactController.addContact(ctx, token)
                }
            }
        }
        return app.start(port)
    }
        private fun serverStopped() {
            stop()
            exitProcess(1)
        }

        internal fun stop() {
            if (!stopped.getAndSet(true)) {
                briarService.stop()
            }
        }

    }
