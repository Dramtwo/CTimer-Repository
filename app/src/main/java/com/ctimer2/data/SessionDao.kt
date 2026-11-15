package com.ctimer2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {
    @Query("SELECT * FROM SessionEntity ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAll(): List<SessionEntity>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE SessionEntity SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM SessionEntity WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM SessionEntity")
    suspend fun count(): Int

    @Query("SELECT * FROM SessionEntity WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?
}
