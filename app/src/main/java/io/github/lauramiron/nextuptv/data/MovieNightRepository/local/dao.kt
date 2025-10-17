package io.github.lauramiron.nextuptv.data.MovieNightRepository.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TitleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTitles(titles: List<TitleEntity>): List<Long>

    @Query("SELECT * FROM titles WHERE id = :id")
    fun getTitle(id: Long): TitleEntity?

    @Transaction
    @Query("""
        SELECT t.* FROM titles t
        JOIN external_ids x ON x.entityType='title' AND x.entityId=t.id
        WHERE x.provider=:provider AND x.providerId=:providerId
    """)
    fun getTitleByExternal(provider: String, providerId: String): TitleEntity?
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertEpisodes(episodes: List<EpisodeEntity>): List<Long>

    @Transaction
    @Query("""
        SELECT e.* FROM episodes e
        JOIN external_ids x ON x.entityType='episode' AND x.entityId=e.id
        WHERE x.provider=:provider AND x.providerId=:providerId
    """)
    fun getEpisodeByExternal(provider: String, providerId: String): EpisodeEntity?
}

@Dao
interface ExternalIdDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(ids: List<ExternalIdEntity>)
}
