package com.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymbuddy.data.local.entities.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:q = '' OR name LIKE '%' || :q || '%' OR target LIKE '%' || :q || '%'
               OR muscleGroup LIKE '%' || :q || '%' OR equipment LIKE '%' || :q || '%')
        AND (:category IS NULL OR category = :category)
        AND (:equipment IS NULL OR equipment = :equipment)
        AND (:target IS NULL OR target = :target)
        AND (:favoritesOnly = 0 OR isFavorite = 1)
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun search(
        q: String,
        category: String?,
        equipment: String?,
        target: String?,
        favoritesOnly: Boolean,
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<ExerciseEntity>>

    @Query("SELECT DISTINCT category FROM exercises ORDER BY category")
    fun categories(): Flow<List<String>>

    @Query("SELECT DISTINCT equipment FROM exercises ORDER BY equipment")
    fun equipmentTypes(): Flow<List<String>>

    @Query("SELECT DISTINCT equipment FROM exercises ORDER BY equipment")
    suspend fun equipmentTypesNow(): List<String>

    @Query("SELECT DISTINCT target FROM exercises ORDER BY target")
    fun targets(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT image, gifUrl FROM exercises")
    suspend fun allMediaPaths(): List<MediaPaths>

    data class MediaPaths(val image: String, val gifUrl: String)

    @Query("SELECT id FROM exercises WHERE isFavorite = 1")
    suspend fun favoriteIds(): List<String>

    @Query("UPDATE exercises SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE exercises SET isFavorite = 1 WHERE id IN (:ids)")
    suspend fun restoreFavorites(ids: List<String>)

    /** All exercises usable with the given equipment (pass noFilter=1 to ignore the filter). */
    @Query("SELECT * FROM exercises WHERE (:noFilter = 1 OR equipment IN (:equipment))")
    suspend fun forEquipment(equipment: List<String>, noFilter: Int): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    suspend fun clear()
}
