package net.enfau.berry

import org.briarproject.bramble.system.DefaultTaskSchedulerModule

interface BotEagerSingletons {
    fun inject(init: DefaultTaskSchedulerModule.EagerSingletons?)

    object Helper {
        fun injectEagerSingletons(c: BotEagerSingletons) {
            c.inject(DefaultTaskSchedulerModule.EagerSingletons())
        }
    }
}
