package com.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymbuddy.data.local.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}
