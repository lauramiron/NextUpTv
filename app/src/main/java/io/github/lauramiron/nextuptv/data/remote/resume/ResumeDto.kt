package io.github.lauramiron.nextuptv.data.remote.resume

/**
 * Individual resume item containing title and service-specific ID
 */
data class ResumeItemDto(
    val id: String,      // Service-specific ID (e.g., Netflix title ID)
    val title: String    // Human-readable title
)

/**
 * Resume data for a single streaming service.
 * Maps resume index (as string) to resume item details.
 * Example: {"0": {id: "80057281", title: "Stranger Things"}, "1": {...}}
 */
typealias ServiceResumeDataDto = Map<String, ResumeItemDto>

/**
 * Complete resume data response from the API.
 * Maps service ID to that service's resume data.
 * Example: {"netflix": {...}, "prime": {...}}
 */
typealias ResumeDataDto = Map<String, ServiceResumeDataDto>