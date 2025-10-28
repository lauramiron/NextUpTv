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
import io.github.lauramiron.nextuptv.data.local.entity.PopularityEntity
import io.github.lauramiron.nextuptv.data.local.entity.PopularityType
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleGenreCrossRef
import io.github.lauramiron.nextuptv.data.local.entity.TitlePersonCrossRef
import java.util.Date

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
    suspend fun findId(entityId: Long, provider: StreamingService): Long?

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

@Dao
interface PopularityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(items: List<PopularityEntity>): List<Long>

    @Update
    suspend fun updateAll(items: List<PopularityEntity>)

    @Query("""
        DELETE FROM title_popularities
        WHERE service = :service
        AND popularityType = :popularityType
        AND titleId NOT IN (:titleIds)
    """)
    suspend fun deleteStaleEntries(
        service: StreamingService,
        popularityType: PopularityType,
        titleIds: List<Long>
    )

    @Query("""
        DELETE FROM title_popularities
        WHERE service = :service
        AND popularityType = :popularityType
    """)
    suspend fun deleteAllForServiceAndType(
        service: StreamingService,
        popularityType: PopularityType
    )

    @Query("""
        SELECT * FROM title_popularities
        WHERE service = :service
        AND popularityType = :popularityType
        AND titleId = :titleId
        LIMIT 1
    """)
    suspend fun findExisting(
        service: StreamingService,
        popularityType: PopularityType,
        titleId: Long
    ): PopularityEntity?

    /**
     * Updates the top shows list for a specific streaming service.
     *
     * This function:
     * - Deletes rows where popularityType=TOP_SHOWS and service matches, but titleId is not in the provided list
     * - Inserts new entries for titleIds that don't exist
     * - Updates the updatedAt timestamp for entries that already exist
     *
     * @param service The streaming service to update
     * @param titleIds List of title IDs that should be in the top shows
     */
    @Transaction
    suspend fun updateTopShows(service: StreamingService, titleIds: List<Long>) {
        val popularityType = PopularityType.TOP_SHOWS
        val currentTime = Date()

        // Delete entries that are no longer in the top shows
        if (titleIds.isNotEmpty()) {
            deleteStaleEntries(service, popularityType, titleIds)
        } else {
            // If empty list, delete all for this service and type
            deleteAllForServiceAndType(service, popularityType)
            return // Nothing to insert
        }

        // Create entities for all titleIds
        val entities = titleIds.map { titleId ->
            PopularityEntity(
                id = 0,
                service = service,
                popularityType = popularityType,
                titleId = titleId,
                updatedAt = currentTime
            )
        }

        // Try to insert all
        val results = insertIgnoreAll(entities)

        // For rows that already existed (insert returned -1), update them
        val toUpdate = mutableListOf<PopularityEntity>()
        results.forEachIndexed { i, rowId ->
            if (rowId == -1L) {
                // Row already exists, need to update it
                val existing = findExisting(service, popularityType, titleIds[i])
                if (existing != null) {
                    toUpdate += existing.copy(updatedAt = currentTime)
                }
            }
        }

        if (toUpdate.isNotEmpty()) {
            updateAll(toUpdate)
        }
    }
}
