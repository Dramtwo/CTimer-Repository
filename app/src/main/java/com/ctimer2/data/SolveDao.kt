package com.ctimer2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SolveDao {
    @Insert
    suspend fun insert(record: SolveRecord): Long

    @Query("SELECT * FROM SolveRecord WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getBySession(sessionId: Long): List<SolveRecord>

    @Query("SELECT * FROM SolveRecord ORDER BY timestamp ASC")
    suspend fun getAll(): List<SolveRecord>

    @Query("SELECT COUNT(*) FROM SolveRecord WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Query("DELETE FROM SolveRecord WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("DELETE FROM SolveRecord WHERE id = :id")
    suspend fun deleteById(id: Long)
}
