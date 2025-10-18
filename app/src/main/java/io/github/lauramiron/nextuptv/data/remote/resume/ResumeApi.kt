package io.github.lauramiron.nextuptv.data.remote.resume

import retrofit2.http.GET
import retrofit2.http.Query

interface ResumeApi {
    @GET("v1/resume")
    suspend fun getResume(@Query("limit") limit: Int = 50): List<ResumeDto>
}