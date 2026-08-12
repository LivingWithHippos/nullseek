package com.github.livingwithhippos.nullseek.core.data.repository

import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.TorboxUserCacheDao
import com.github.livingwithhippos.nullseek.core.data.database.model.TorboxUserCache
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.UserDataDto
import com.github.livingwithhippos.torbox.network.ApiResult

interface UserRepository {
    suspend fun getAll(): List<UserDataDto>

    suspend fun getUserData(): UserDataDto?

    suspend fun refreshUserData(): UserDataDto?

    suspend fun clearCache()
}

class UserRepositoryImpl(
    private val userCacheDao: TorboxUserCacheDao,
    private val providerDao: NullProviderDao,
    private val client: TorboxClient,
) : UserRepository {
    override suspend fun getAll(): List<UserDataDto> = userCacheDao.getAll().map { it.toUserDataDto() }

    override suspend fun getUserData(): UserDataDto? {
        // todo: add an expiration date and a refresh button to the User screen
        val activeId = providerDao.getActive()?.id ?: return null
        val cached = userCacheDao.getById(activeId)
        if (cached != null) {
            return cached.toUserDataDto()
        }
        return refreshUserData()
    }

    override suspend fun refreshUserData(): UserDataDto? {
        // skip the network request if we have no active provider, since it would fail
        val activeId = providerDao.getActive()?.id ?: return null
        val result = client.user.data()

        if (result is ApiResult.Success) {
            val userData = result.value.data ?: return null
            userCacheDao.upsert(TorboxUserCache.fromUserDataDto(userData, activeId))
            return userData
        }
        return null
    }

    override suspend fun clearCache() {
        userCacheDao.clear()
    }
}
