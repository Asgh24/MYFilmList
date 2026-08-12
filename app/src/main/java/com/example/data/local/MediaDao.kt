package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY lastModified DESC")
    fun getAllMediaItems(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE detectedType = :type ORDER BY cleanTitle ASC")
    fun getMediaByType(type: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: String): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE media_items SET cleanTitle = :newTitle, detectedType = :newType, titleEnglish = :titleEng, titleRomaji = :titleRom, synopsis = :synopsis, posterUrl = :poster, rating = :rating, genresJson = :genres, needsReview = 0, candidatesJson = NULL WHERE id IN (:ids)")
    suspend fun updateCollectionMetadata(
        ids: List<String>,
        newTitle: String,
        newType: String,
        titleEng: String?,
        titleRom: String?,
        synopsis: String?,
        poster: String?,
        rating: Double?,
        genres: String?
    )

    @Query("UPDATE media_items SET needsReview = :needsReview, candidatesJson = :candidatesJson WHERE id IN (:ids)")
    suspend fun updateCollectionReviewStatus(
        ids: List<String>,
        needsReview: Boolean,
        candidatesJson: String?
    )

    @Query("UPDATE media_items SET isWatched = :isWatched, playbackPositionMs = :position WHERE id = :id")
    suspend fun updateWatchStatus(id: String, isWatched: Boolean, position: Long)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
