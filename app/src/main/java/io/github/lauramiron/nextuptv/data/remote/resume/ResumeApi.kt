package io.github.lauramiron.nextuptv.data.remote.resume

import retrofit2.http.GET
import retrofit2.http.Query

interface ResumeApi {
    @GET("resume")
    suspend fun getResume(@Query("limit") limit: Int = 50): ResumeDataDto
}