package io.github.lauramiron.nextuptv.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.lauramiron.nextuptv.data.local.entity.ResumeEntryEntity
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<ResumeEntryEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM resume_entries")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM resume_entries WHERE serviceId = :service")
    suspend fun countByService(service: StreamingService): Int

    @Query("SELECT COUNT(*) FROM resume_entries WHERE resolvedTitleId IS NULL")
    suspend fun countUnresolved(): Int

    @Query("""
        SELECT * FROM resume_entries
        WHERE resolvedTitleId IS NULL
        ORDER BY resumeIndex
        LIMIT :limit
    """)
    suspend fun findUnresolved(limit: Int): List<ResumeEntryEntity>

    @Query("""
        UPDATE resume_entries
        SET resolvedTitleId = :titleId,
            resolvedEpisodeId = :episodeId
        WHERE id = :resumeId
    """)
    suspend fun markResolved(
        resumeId: Long,
        titleId: Long?,
        episodeId: Long?,
    )

    // Feed for UI: give recent, resolved-first, but include unresolved fallbacks
    @Query("""
        SELECT re.*, t.name AS resolvedTitleName, t.imageSetJson AS resolvedTitleImage
        FROM resume_entries re
        LEFT JOIN titles t ON t.id = re.resolvedTitleId
        ORDER BY resumeIndex
        LIMIT :limit
    """)
    fun resumeFeed(limit: Int): Flow<List<ResumeWithTitleRow>>
}

// Lightweight projection for the row (avoid loading the world)
data class ResumeWithTitleRow(
    @Embedded val entry: ResumeEntryEntity,
    val resolvedTitleName: String?,
    val resolvedTitleImage: String?
)
