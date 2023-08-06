package net.enfau.berry.commands

annotation class DewCommand(
    val name: String,
    val scope: DewCommandScope = DewCommandScope.Message,
    val shortHelp: String = "No help available",
    val longHelp: String = "",
    val privileged: Boolean = false
)

