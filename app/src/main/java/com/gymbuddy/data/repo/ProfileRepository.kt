package com.gymbuddy.data.repo

import com.gymbuddy.data.local.dao.ProfileDao
import com.gymbuddy.data.local.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val dao: ProfileDao) {
    fun observe(): Flow<UserProfileEntity?> = dao.observe()
    suspend fun get(): UserProfileEntity = dao.get() ?: UserProfileEntity()
    suspend fun save(profile: UserProfileEntity) = dao.upsert(profile.copy(id = 1))
}
