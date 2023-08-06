package net.enfau.berry.dao

import net.enfau.berry.utils.toBase32
import org.briarproject.bramble.api.identity.AuthorId
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface PermissionDao {
    @SqlQuery("SELECT permission_flags FROM permissions INNER JOIN contacts ON contacts.sid = permissions.contact_sid WHERE contacts.author_id = ?")
    @RegisterConstructorMapper(PermissionFlags::class)
    fun getByIdNullable(authorId: String): PermissionFlags?


    @SqlQuery("SELECT permission_flags FROM permissions WHERE sid = ?")
    @RegisterConstructorMapper(PermissionFlags::class)
    fun getNullable(sid: Long): PermissionFlags?

    fun get(authorId: AuthorId): PermissionFlags {
        val flags = getByIdNullable(authorId.toBase32())
        if (flags==null) {
            return PermissionFlags(0)
        } else {
            return  flags
        }
    }

    fun get(authorId: String): PermissionFlags {
        val flags = getByIdNullable(authorId)
        if (flags==null) {
            return PermissionFlags(0)
        } else {
            return  flags
        }
    }

    fun get(sid: Long): PermissionFlags {
        val flags = get(sid)
        if (flags==null) {
            return PermissionFlags(0)
        } else {
            return  flags
        }
    }


    @SqlUpdate("INSERT INTO permissions (contact_sid, permission_flags) SELECT sid, :permissionFlags FROM contacts WHERE author_id = :authorId")
    fun insertRaw(authorId: String, permissionFlags: Int): Boolean

    @SqlUpdate("""
        UPDATE permissions 
        SET permission_flags = :permissionFlags
        WHERE contact_sid in (SELECT sid FROM contacts WHERE author_id = :authorId)
    """)
    fun setRaw(authorId: String, permissionFlags: Int): Boolean

    fun set(authorId: String, permissionFlags: PermissionFlags): Boolean {
        if(getByIdNullable(authorId)==null) {
            return insertRaw(authorId, permissionFlags.bits())
        } else {
            return setRaw(authorId, permissionFlags.bits())
        }
    }

    fun set(authorId: AuthorId, permissionFlags: PermissionFlags) =
        set(authorId.toBase32(), permissionFlags)

}
