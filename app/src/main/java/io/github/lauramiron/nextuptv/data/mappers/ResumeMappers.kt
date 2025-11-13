package io.github.lauramiron.nextuptv.data.mappers

import io.github.lauramiron.nextuptv.data.local.entity.ResumeEntryEntity
import io.github.lauramiron.nextuptv.util.StreamingService
import io.github.lauramiron.nextuptv.data.remote.resume.ResumeDataDto

/**
 * Convert ResumeDataDto to a list of ResumeEntryEntity for database storage.
 * Flattens the nested structure: serviceId -> resumeIndex -> item
 *
 * Example input:
 * {
 *   "netflix": {
 *     "0": { "id": "80057281", "title": "Stranger Things" },
 *     "1": { "id": "70142362", "title": "Nurse Jackie" }
 *   }
 * }
 *
 * Converts to 2 ResumeEntryEntity objects.
 */
fun ResumeDataDto.toEntities(): List<ResumeEntryEntity> {
    val entities = mutableListOf<ResumeEntryEntity>()

    this.forEach { (serviceIdStr, serviceData) ->
        // Convert service ID string to enum, skip if unknown
        val service = StreamingService.fromString(serviceIdStr) ?: return@forEach

        serviceData.forEach { (resumeIndexStr, item) ->
            // Convert resume index string to int, skip if invalid
            val resumeIndex = resumeIndexStr.toIntOrNull() ?: return@forEach

            entities += ResumeEntryEntity(
                id = 0,
                serviceId = service,
                serviceItemId = item.id,
                titleText = item.title,
                seasonNumber = null,  // Not provided by current API
                episodeNumber = null, // Not provided by current API
                resumeIndex = resumeIndex,
                resolvedTitleId = null,
                resolvedEpisodeId = null
            )
        }
    }

    return entities
}
