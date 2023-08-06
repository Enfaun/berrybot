package net.enfau.berry.dao

import net.enfau.berry.utils.toBase32
import org.briarproject.bramble.api.identity.AuthorId
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface ContactDao {
    @SqlQuery("SELECT COUNT(1) FROM contacts WHERE author_id = :authorId")
    fun exists(@Bind("authorId") authorId: String): Boolean

    fun exists(authorId: AuthorId) =
        exists(authorId.toBase32())

    @SqlUpdate("INSERT INTO contacts (author_id, name, last_communicated_at) VALUES (:authorId, :name, :lastCommunicatedAt)")
    fun insert(@Bind("authorId") authorId: String, @Bind("name") name: String, @Bind("lastCommunicatedAt") lastCommunicatedAt: Long)

    fun insert(authorId: AuthorId, name: String, lastCommunicatedAt: Long) =
        insert(authorId.toBase32(), name, lastCommunicatedAt)


    @SqlUpdate("UPDATE contacts SET last_communicated_at = :lastCommunicatedAt WHERE author_id = :authorId")
    fun updateLastCommunicatedAt(
        @Bind("authorId") authorId: String,
        @Bind("lastCommunicatedAt") lastCommunicatedAt: Long
    )


    fun updateLastCommunicatedAt(authorId: AuthorId, lastCommunicatedAt: Long) =
        updateLastCommunicatedAt(authorId.toBase32(), lastCommunicatedAt)


    @SqlQuery("SELECT last_communicated_at FROM contacts WHERE author_id = :authorId")
    fun getLastCommunicatedAt(@Bind("authorId") authorId: String): Long

    fun getLastCommunicatedAt(authorId: AuthorId) =
        getLastCommunicatedAt(authorId.toBase32())

    @SqlQuery("SELECT * FROM contacts WHERE name LIKE ? AND NAME != ''")
    @RegisterConstructorMapper(Contact::class)
    fun findContactsByName(name: String): List<Contact>

    @SqlQuery("SELECT * FROM contacts WHERE name LIKE ? AND NAME != ''")
    @RegisterConstructorMapper(Contact::class)
    fun findFirstContactByName(name: String): Contact?

    @SqlQuery("SELECT * FROM contacts WHERE author_id = ?")
    @RegisterConstructorMapper(Contact::class)
    fun get(authorId: String): Contact?

    fun get(authorId: AuthorId) =
        get(authorId.toBase32())

}

