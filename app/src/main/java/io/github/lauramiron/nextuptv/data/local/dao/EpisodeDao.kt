package io.github.lauramiron.nextuptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.lauramiron.nextuptv.data.local.entity.EpisodeEntity

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(items: List<EpisodeEntity>): List<Long>

    @Update
    suspend fun updateAll(items: List<EpisodeEntity>)

    @Query("""
        SELECT id FROM episodes
        WHERE titleId = :titleId AND
              ((:season IS NULL AND seasonNumber IS NULL) OR seasonNumber = :season) AND
              ((:ep IS NULL AND episodeNumber IS NULL) OR episodeNumber = :ep)
        LIMIT 1
    """)
    suspend fun findIdByNaturalKey(
        titleId: Long,
        season: Int?,
        ep: Int?
    ): Long?

    @Transaction
    suspend fun upsertAll(items: List<EpisodeEntity>): Int {
        if (items.isEmpty()) return 0
        val results = insertIgnoreAll(items)
        val toUpdate = mutableListOf<EpisodeEntity>()
        results.forEachIndexed { i, rowId ->
            if (rowId == -1L) {
                val e = items[i]
                val id = findIdByNaturalKey(e.titleId, e.seasonNumber, e.episodeNumber)
                if (id != null) toUpdate += e.copy(id = id)
            }
        }
        if (toUpdate.isNotEmpty()) updateAll(toUpdate)
        return items.size
    }
}