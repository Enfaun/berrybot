package net.enfau.berry.web

import io.javalin.http.Context

interface ContactController {
    fun addContact(ctx : Context, token: String)
    fun getLink(ctx: Context)
}