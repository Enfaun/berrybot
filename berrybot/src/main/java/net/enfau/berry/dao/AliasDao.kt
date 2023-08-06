package net.enfau.berry.dao

import org.briarproject.bramble.api.identity.AuthorId
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

import net.enfau.berry.utils.toBase32

interface AliasDao {
    @SqlUpdate("""
        UPDATE aliases 
        SET alias = :alias
        WHERE contact_sid in (SELECT sid FROM contacts WHERE author_id = :authorId)
    """)
    fun update(@Bind("authorId") authorId: String, @Bind("alias") alias: String): Int


    fun update(authorId: AuthorId, @Bind("alias") alias: String) = update(authorId.toBase32(), alias)

    @SqlUpdate("INSERT INTO aliases (contact_sid, alias) SELECT sid, :alias FROM contacts WHERE author_id = :authorId")
    fun insert(@Bind("authorId") authorId: String, @Bind("alias") alias: String): Int

    fun insert(authorId: AuthorId, @Bind("alias") alias: String) = insert(authorId.toBase32(), alias)

    @RegisterBeanMapper(Alias::class)
    @SqlQuery("SELECT * FROM aliases WHERE alias = ? INNER JOIN contacts ON aliases.contact_sid = contacts.sid")
    fun findAuthor(alias: String): Alias?

    @SqlQuery("SELECT alias FROM aliases INNER JOIN contacts ON aliases.contact_sid = contacts.sid WHERE contacts.author_id = :authorId")
    fun getAlias(@Bind("authorId") authorId: String): String?

    fun getAlias(authorId: AuthorId) = getAlias(authorId.toBase32())
}

