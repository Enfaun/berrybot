package net.enfau.berry.dao

import org.briarproject.bramble.api.identity.AuthorId
import net.enfau.berry.utils.*
import org.jdbi.v3.core.mapper.reflect.ColumnName

class Contact
constructor(
    @ColumnName("sid") val sid: Long,
    @ColumnName("author_id") val authorIdStr: String,
    @ColumnName("name") val name: String,
    @ColumnName("last_communicated_at") val lastCommunicatedAt: Long
    ) {
    val authorId: AuthorId = authorIdStr.base32Decode().toAuthorId()

}
