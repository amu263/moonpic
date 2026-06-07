package dev.moonpic.core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LutDao {
    @Query("SELECT * FROM luts ORDER BY created_at DESC")
    fun observeAll(): Flow<List<LutEntity>>

    @Query("SELECT * FROM luts WHERE id = :id")
    suspend fun findById(id: Long): LutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lut: LutEntity): Long

    @Delete
    suspend fun delete(lut: LutEntity)

    @Query("DELETE FROM luts")
    suspend fun clear()
}
