package io.github.lauramiron.nextuptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.lauramiron.nextuptv.data.local.entity.LibrarySyncMetadataEntity
import io.github.lauramiron.nextuptv.data.local.entity.SyncType

@Dao
interface LibrarySyncMetadataDao {

    @Insert
    suspend fun insert(metadata: LibrarySyncMetadataEntity): Long

    @Query("SELECT * FROM library_sync_metadata ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSyncs(limit: Int = 10): List<LibrarySyncMetadataEntity>

    @Query("SELECT * FROM library_sync_metadata WHERE success = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSuccessfulSync(): LibrarySyncMetadataEntity?

    @Query("SELECT * FROM library_sync_metadata WHERE syncType = :syncType ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSyncOfType(syncType: SyncType): LibrarySyncMetadataEntity?

    @Query("SELECT * FROM library_sync_metadata WHERE syncType = 'FULL' AND success = 0 AND nextCursor IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastInterruptedFullSync(): LibrarySyncMetadataEntity?

    @Query("SELECT COUNT(*) FROM library_sync_metadata WHERE success = 1")
    suspend fun countSuccessfulSyncs(): Int

    @Query("SELECT COUNT(*) FROM library_sync_metadata WHERE success = 0")
    suspend fun countFailedSyncs(): Int

    @Query("DELETE FROM library_sync_metadata WHERE id NOT IN (SELECT id FROM library_sync_metadata ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldSyncs(keepCount: Int = 100)
}
