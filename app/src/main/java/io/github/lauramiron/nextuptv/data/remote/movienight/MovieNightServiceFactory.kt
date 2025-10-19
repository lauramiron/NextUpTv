package io.github.lauramiron.nextuptv.data.remote.movienight

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object MovieNightServiceFactory {
    fun create(
        apiBaseUrl: String = "https://streaming-availability.p.rapidapi.com/",
        apiKey: String,
        debugLogs: Boolean = false
    ): MovieNightApi {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (debugLogs) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(clientBuilder.build())
            .build()

        val service = retrofit.create(MovieNightService::class.java)
        return MovieNightApi(service, apiKey)
    }
}
