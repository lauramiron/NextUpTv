package io.github.lauramiron.nextuptv.data.remote.resume

data class ResumeDto(
    val serviceId: String,           // "netflix" | "prime" | ...
    val serviceItemId: String?,      // e.g. netflix numeric id if you expose it; else null
    val titleText: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val resumeIndex: Int?
//    val progressMinutes: Int?,
//    val progressSeconds: Int?,
//    val lastWatchedAt: Long          // epoch millis (have backend normalize to UTC)
)