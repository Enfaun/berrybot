package net.enfau.berry

import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
internal class Bot
@Inject
constructor(private val briarService: BriarService) {
    internal fun start(name: String, password: String) {
        briarService.start(name, password);
    }

    internal fun resetAccount() {
        briarService.resetAccount()
    }
}