package com.application.myapplication

import com.mechanizo.android.customer.data.model.response.directions.RouteDirections
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val ORS_API_KEY = "5b3ce3597851110001cf6248e715069851984c41a8fca1c08f0fbb2c"
const val ORS_BASE_URL = "https://api.openrouteservice.org/v2/directions/"

interface DirectionsApiService {

    @GET("driving-car")
    suspend fun getRouteDirections(
        @Query("api_key") apiKey: String = ORS_API_KEY,
        @Query("start") start: String,
        @Query("end") end: String,
    ): Response<RouteDirections>

    companion object {
        operator fun invoke(): DirectionsApiService {


            val httpLoggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger.DEFAULT)

            val requestInterceptor = Interceptor { chain ->

                val url = chain.request()
                    .url
                    .newBuilder()
                    .build()

                val request = chain.request()
                    .newBuilder()
                    .url(url)
                    .build()

                return@Interceptor chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .addInterceptor(httpLoggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(ORS_BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DirectionsApiService::class.java)
        }
    }
}