package io.github.lauramiron.nextuptv.data

import androidx.room.withTransaction
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.dao.ResumeWithTitleRow
import io.github.lauramiron.nextuptv.data.local.entity.ResumeEntryEntity
import io.github.lauramiron.nextuptv.data.remote.resume.ResumeApi
import kotlinx.coroutines.flow.Flow

class ResumeRepository(
    private val api: ResumeApi,
    private val db: AppDb
) {
//    private val resumeDao = db.resumeDao()
    private val extDao = db.externalIdDao()
    private val titleDao = db.titleDao()
//    private val episodeDao = db.episodeDao()

//    suspend fun syncResume(limit: Int = 50) = db.withTransaction {
//        val dtos = api.getResume(limit)
//        val entities = dtos.map { it.toEntity() }
//        resumeDao.upsertAll(entities)
//
//        // Best-effort resolution for a small batch of unresolved
//        resolveUnresolved(max = 100)
//    }
//
//    private fun resolveUnresolved(max: Int) {
//        val unresolved = resumeDao.findUnresolved(limit = max)
//
//        unresolved.forEach { re ->
//            // 1) Provider id match, if present
//            val byProvider = re.serviceItemId?.let { pid ->
//                when (re.serviceId) {
//                    "netflix" -> extDao.findTitleByExternal("netflix", pid)
//                    "prime"   -> extDao.findTitleByExternal("prime", pid)
//                    "disney"  -> extDao.findTitleByExternal("disney", pid)
//                    else      -> null
//                }
//            }
//
//            if (byProvider != null) {
//                resumeDao.markResolved(
//                    resumeId = re.id,
//                    titleId = byProvider.id,
//                    episodeId = null,                 // You can extend for episode-level later
//                )
//                return@forEach
//            }
//
//            // 2) Title-based heuristic
//            val titleMatch = titleDao.findByNormalizedName(normalize(re.titleText))
//            if (titleMatch != null) {
//                // Optional: episode matching (by S/E or episodeText fuzzy)
//                val episodeId = resolveEpisode(titleMatch.id, re)
//                resumeDao.markResolved(
//                    resumeId = re.id,
//                    titleId = titleMatch.id,
//                    episodeId = episodeId,
//                )
//            }
//        }
//    }
//
//    private fun resolveEpisode(titleId: Long, re: ResumeEntryEntity): Long? {
//        // Try S/E first
//        val s = re.seasonNumber
//        val e = re.episodeNumber
//        if (s != null && e != null) {
//            return episodeDao.findByTitleSeasonEpisode(titleId, s, e)?.id
//        }
//        return null
//    }
//
//    fun resumeFeed(limit: Int = 30): Flow<List<ResumeWithTitleRow>> =
//        resumeDao.resumeFeed(limit)
}