package io.github.lauramiron.nextuptv.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.lauramiron.nextuptv.data.local.entity.ExternalIdEntity
import io.github.lauramiron.nextuptv.data.local.entity.GenreEntity
import io.github.lauramiron.nextuptv.data.local.entity.PersonEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleGenreCrossRef
import io.github.lauramiron.nextuptv.data.local.entity.TitlePersonCrossRef

@Dao
interface TitleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: TitleEntity): Long

    @Update
    suspend fun update(entity: TitleEntity)

    @Query("SELECT * FROM titles WHERE id = :id")
    fun getTitle(id: Long): TitleEntity?

    @Query("SELECT id FROM titles WHERE monId = :monId LIMIT 1")
    suspend fun findIdByMonId(monId: String): Long?

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun countAll(): Int

    @Transaction
    suspend fun upsert(entity: TitleEntity): Long {
        val insertId = insertIgnore(entity)
        if (insertId != -1L) return insertId
        val existingId = findIdByMonId(entity.monId) ?: error("Title missing after IGNORE")
        //update(entity.copy(id = existingId))
        return existingId
    }

//    @Transaction
//    @Query("""
//        SELECT t.* FROM titles t
//        JOIN external_ids x ON x.entityType='title' AND x.entityId=t.id
//        WHERE x.provider=:provider AND x.providerId=:providerId
//    """)
//    fun getTitleByExternal(provider: String, providerId: String): TitleEntity?
}

//@Dao
//interface EpisodeDao {
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun insertIgnoreAll(items: List<EpisodeEntity>): List<Long>
//
//    @Update
//    suspend fun updateAll(items: List<EpisodeEntity>)
//
//    @Query("""
//        SELECT id FROM episodes
//        WHERE titleId = :titleId AND
//              ((:season IS NULL AND seasonNumber IS NULL) OR seasonNumber = :season) AND
//              ((:ep IS NULL AND episodeNumber IS NULL) OR episodeNumber = :ep)
//        LIMIT 1
//    """)
//    suspend fun findIdByNaturalKey(
//        titleId: Long,
//        season: Int?,
//        ep: Int?
//    ): Long?
//
//    @Transaction
//    suspend fun upsertAll(items: List<EpisodeEntity>): Int {
//        if (items.isEmpty()) return 0
//        val results = insertIgnoreAll(items)
//        val toUpdate = mutableListOf<EpisodeEntity>()
//        results.forEachIndexed { i, rowId ->
//            if (rowId == -1L) {
//                val e = items[i]
//                val id = findIdByNaturalKey(e.titleId, e.seasonNumber, e.episodeNumber)
//                if (id != null) toUpdate += e.copy(id = id)
//            }
//        }
//        if (toUpdate.isNotEmpty()) updateAll(toUpdate)
//        return items.size
//    }
//}

@Dao
interface ExternalIdDao {
    // 1) Fast-path insert that won’t replace existing rows
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(items: List<ExternalIdEntity>): List<Long>

    // 2) Batch update by primary key
    @Update
    suspend fun updateAll(items: List<ExternalIdEntity>)

    // 3) Find existing row id by natural key (unique index recommended)
    @Query("""
        SELECT id FROM external_ids
        WHERE entityId = :entityId AND provider = :provider
        LIMIT 1
    """)
    suspend fun findId(entityId: Long, provider: String): Long?

    /**
     * Upsert all:
     * - INSERT IGNORE first
     * - For conflicts, look up existing ids and UPDATE with latest providerId/available/price
     * Returns the number of rows processed.
     */
    @Transaction
    suspend fun upsertAll(items: List<ExternalIdEntity>): Int {
        if (items.isEmpty()) return 0

        val results = insertIgnoreAll(items)
        val toUpdate = ArrayList<ExternalIdEntity>()

        results.forEachIndexed { i, rowId ->
            if (rowId == -1L) {
                val e = items[i]
                val id = findId(e.entityId, e.provider)
                if (id != null) {
                    // carry over the PK and update the mutable columns
                    toUpdate += e.copy(id = id)
                }
            }
        }

        if (toUpdate.isNotEmpty()) {
            updateAll(toUpdate)
        }

        return items.size
    }
}

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(items: List<GenreEntity>): List<Long>

    @Query("SELECT id FROM genres WHERE name = :name LIMIT 1")
    suspend fun findIdByName(name: String): Long?

    @Update
    suspend fun updateAll(items: List<GenreEntity>)

    /**
     * Upserts by name and returns IDs in the same order as `names`.
     */
    @Transaction
    suspend fun upsertAllByName(names: List<String>): List<Long> {
        if (names.isEmpty()) return emptyList()
        // Build entities from names
        val entities = names.map { GenreEntity(name = it) }
        val ids = MutableList(entities.size) { -1L }
        val results = insertIgnoreAll(entities)

        results.forEachIndexed { i, rowId ->
            if (rowId != -1L) {
                ids[i] = rowId
            } else {
                val existingId = findIdByName(entities[i].name)
                if (existingId != null) {
                    ids[i] = existingId
                }
            }
        }
        return ids
    }
}

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(items: List<PersonEntity>): List<Long>

    @Update
    suspend fun updateAll(items: List<PersonEntity>)

    @Query("SELECT id FROM people WHERE name = :name LIMIT 1")
    suspend fun findIdByName(name: String): Long?

    @Transaction
    suspend fun upsertAll(items: List<PersonEntity>): List<Long> {
        if (items.isEmpty()) return emptyList()
        val ids = MutableList(items.size) { -1L }
        val results = insertIgnoreAll(items)
//        val toUpdate = mutableListOf<PersonEntity>()
        results.forEachIndexed { i, rowId ->
            if (rowId != -1L) {
                ids[i] = rowId
            } else {
                val existing = findIdByName(items[i].name) ?: return@forEachIndexed
                ids[i] = existing
                // If you track other mutable columns on PersonEntity, update them here.
//                toUpdate += items[i].copy(id = existing)
            }
        }
//        if (toUpdate.isNotEmpty()) updateAll(toUpdate)
        return ids
    }
}

@Dao
interface TitleGenreCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertAll(items: List<TitleGenreCrossRef>): List<Long>
}

@Dao
interface TitlePersonCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertAll(items: List<TitlePersonCrossRef>): List<Long>
}
