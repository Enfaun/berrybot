package net.enfau.berry.dao

import dagger.Module
import dagger.Provides
import org.jdbi.v3.core.Jdbi

@Module
class DaoModule {
    @Provides
    internal fun provideAliasDao(jdbi: Jdbi) = jdbi.onDemand(AliasDao::class.java)
    @Provides
    internal fun provideContactsDao(jdbi: Jdbi) = jdbi.onDemand(ContactDao::class.java)
    @Provides
    internal fun providePermissionDao(jdbi: Jdbi) = jdbi.onDemand(PermissionDao::class.java)
    @Provides
    internal fun provideVariableDao(jdbi: Jdbi) = jdbi.onDemand(VariableDao::class.java)
}
