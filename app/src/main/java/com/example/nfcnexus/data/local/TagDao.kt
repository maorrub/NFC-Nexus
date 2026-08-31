package com.example.nfcnexus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM saved_tags ORDER BY timestamp DESC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM saved_tags WHERE category = :category ORDER BY timestamp DESC")
    fun getTagsByCategory(category: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM saved_tags WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM saved_tags WHERE id = :id")
    fun getTagById(id: Long): TagEntity?

    @Query("SELECT * FROM saved_tags WHERE title LIKE '%' || :query || '%' OR tagUid LIKE '%' || :query || '%' OR tagType LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTags(query: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tags: List<TagEntity>): Unit

    @Update
    fun updateTag(tag: TagEntity): Unit

    @Delete
    fun deleteTag(tag: TagEntity): Unit

    @Query("DELETE FROM saved_tags WHERE id = :id")
    fun deleteById(id: Long): Unit

    @Query("DELETE FROM saved_tags WHERE category = 'SCANNED'")
    fun clearScannedHistory(): Unit
}
