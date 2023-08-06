package net.enfau.berry.commands

interface HelpRegisterer {
    fun register(cls: Class<*>)
}