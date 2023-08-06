package net.enfau.berry.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface WithLogger {
    fun logger(): Logger {
        val l = LoggerFactory.getLogger(javaClass)
        
        return LoggerFactory.getLogger(javaClass)
    }
}
