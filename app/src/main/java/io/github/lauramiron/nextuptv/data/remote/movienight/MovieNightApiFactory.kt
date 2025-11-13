package io.github.lauramiron.nextuptv.data.remote.movienight

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object MovieNightApiFactory {
    fun create(
        apiBaseUrl: String = "https://streaming-availability.p.rapidapi.com/",
        apiKey: String,
        debugLogs: Boolean = false
    ): MovieNightApi {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // Increased for large responses (HBO, etc.)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (debugLogs) {
            // Add custom interceptor to log response size
            clientBuilder.addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val responseBody = response.body
                val bodyString = responseBody?.string() ?: ""

                println("Response size: ${bodyString.length} bytes")
                println("Response preview (first 500 chars): ${bodyString.take(500)}")
                println("Response end (last 200 chars): ${bodyString.takeLast(200)}")

                // Recreate response with the body we just read
                response.newBuilder()
                    .body(okhttp3.ResponseBody.create(responseBody?.contentType(), bodyString))
                    .build()
            }

            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(clientBuilder.build())
            .build()

        val service = retrofit.create(MovieNightApiService::class.java)
        return MovieNightApi(service, apiKey)
    }
}
