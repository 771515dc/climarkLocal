package com.cs407.climark.data.weather

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- models ---
data class WeatherApiResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val daily: DailyData?,
    @SerializedName("daily_units") val dailyUnits: DailyUnits?
)

data class DailyUnits(
    val time: String?,
    @SerializedName("temperature_2m_max") val tMaxUnit: String?,
    @SerializedName("temperature_2m_min") val tMinUnit: String?,
    @SerializedName("precipitation_probability_max") val precipUnit: String?
)

data class DailyData(
    val time: List<String>,
    @SerializedName("temperature_2m_max") val tMax: List<Double>,
    @SerializedName("temperature_2m_min") val tMin: List<Double>,
    @SerializedName("precipitation_probability_max") val precipMax: List<Int>,
    @SerializedName("weathercode") val weatherCode: List<Int>
)

// --- Retrofit service ---
interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode",
        @Query("timezone") timezone: String = "auto",
        @Query("past_days") pastDays: Int = 2,   // >= 2 days history
        @Query("forecast_days") forecastDays: Int = 4 // >= 2 days future
    ): WeatherApiResponse
}


object WeatherApi {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val client by lazy {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder().addInterceptor(log).build()
    }

    val service: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}
