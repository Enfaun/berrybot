package net.enfau.berry.dao

import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.util.*

enum class PermissionFlag(val value: Int) {
    NONE(0),
    MANAGE_PERMISSIONS(1),
    ADD_FORUM(2),
    REMOVE_FORUM(4),
    QUERY_USER(8),
    MODIFY_VARIABLE(16),
    ADD_PENDING_CONTACT(32),
    ADMIN(32768);

    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.value == value }
    }
}

class PermissionFlags(@ColumnName("permission_flags") private var permissionFlags: Int) {

    fun has(flag: PermissionFlag) = permissionFlags.and(flag.value) == flag.value

    fun on(flag: PermissionFlag) {
        permissionFlags = permissionFlags.or(flag.value)
    }

    fun off(flag: PermissionFlag) {
        permissionFlags = permissionFlags.and(flag.value.inv())
    }

    fun bits() = permissionFlags
}
