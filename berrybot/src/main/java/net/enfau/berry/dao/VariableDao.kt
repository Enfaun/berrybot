package net.enfau.berry.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface VariableDao {
    @SqlQuery("SELECT value FROM variables WHERE key = ?")
    fun get(key: String): String?

    fun getOrDefault(key: String, default: String): String {
        return get(key)?: default
    }

    @SqlUpdate("INSERT OR REPLACE INTO variables (key, value) VALUES (?, ?)")
    fun set(key: String, value: String?): Boolean
}
