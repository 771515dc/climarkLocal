// MapViewModel.kt
package com.cs407.climark.ui.theme.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.climark.data.weather.WeatherApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


data class DailyWeather(
    val dateIso: String,     // "2025-10-19"
    val dayLabel: String,    // "SUN"
    val tMin: Int,           // rounded
    val tMax: Int,           // rounded
    val precipPct: Int,      // 0..100
    val weatherCode: Int
)

data class MapState(
    val markers: List<LatLng> = emptyList(),
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleteMode: Boolean = false,
    val weather: List<DailyWeather> = emptyList(),
    val weatherError: String? = null,
    val weatherLoading: Boolean = false,
    val weatherLocationLabel: String? = null,
    val showWeatherCard: Boolean = false,
    val selectedMarker: LatLng? = null,
    val addMode: Boolean = false
)



class MapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MapState())
    val uiState = _uiState.asStateFlow()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun initializeLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }
    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val loc = fusedLocationClient.lastLocation.await()
                if (loc == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Location unavailable.")
                    return@launch
                }
                val here = LatLng(loc.latitude, loc.longitude)
                _uiState.value = _uiState.value.copy(
                    currentLocation = here,
                    // NOTE: do NOT add 'here' to markers; keep user markers separate.
                    isLoading = false
                )
            } catch (se: SecurityException) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Location permission required.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Location error.")
            }
        }
    }

    // --- Marker helpers (NEW) ---
    fun addMarker(p: LatLng) {
        _uiState.value = _uiState.value.copy(
            markers = _uiState.value.markers + p,
            addMode = false                     // exit add mode after 1 add
        )
    }

    fun removeMarker(p: LatLng) {
        _uiState.value = _uiState.value.copy(
            markers = _uiState.value.markers.filterNot { it == p },
            deleteMode = false                  // exit delete mode after 1 delete
        )
    }

    fun toggleAddMode() {
        _uiState.value = _uiState.value.copy(
            addMode = !_uiState.value.addMode,
            deleteMode = false                  // don’t allow both at once
        )
    }

    fun cancelAddMode() {
        if (_uiState.value.addMode) {
            _uiState.value = _uiState.value.copy(addMode = false)
        }
    }

    fun toggleDeleteMode() {
        _uiState.value = _uiState.value.copy(
            deleteMode = !_uiState.value.deleteMode,
            addMode = false
        )
    }

    fun cancelDeleteMode() {
        if (_uiState.value.deleteMode) {
            _uiState.value = _uiState.value.copy(deleteMode = false)
        }
    }


    fun fetchWeatherFor(latLng: LatLng) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(weatherLoading = true, weatherError = null)
            try {
                val resp = withContext(Dispatchers.IO) {
                    WeatherApi.service.getWeatherData(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude
                    )
                }

                val daily = resp.daily
                val list = if (daily != null) {
                    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                    daily.time.indices.map { i ->
                        val iso = daily.time[i]
                        val date = LocalDate.parse(iso, isoFormatter)
                        DailyWeather(
                            dateIso = iso,
                            dayLabel = date.dayOfWeek.name.take(3), // MON/TUE/...
                            tMin = (daily.tMin.getOrNull(i) ?: 0.0).toInt(),
                            tMax = (daily.tMax.getOrNull(i) ?: 0.0).toInt(),
                            precipPct = (daily.precipMax.getOrNull(i) ?: 0),
                            weatherCode = (daily.weatherCode.getOrNull(i) ?: 0)
                        )
                    }
                } else emptyList()

                val lat = latLng.latitude
                val lon = latLng.longitude
                val label = buildString {
                    append(String.format(Locale.US, "%.2f°%s", kotlin.math.abs(lat), if (lat >= 0) "N" else "S"))
                    append(" • ")
                    append(String.format(Locale.US, "%.2f°%s", kotlin.math.abs(lon), if (lon >= 0) "E" else "W"))
                }

                _uiState.value = _uiState.value.copy(
                    weather = list,
                    weatherLoading = false,
                    weatherLocationLabel = label
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    weatherLoading = false,
                    weatherError = e.message ?: "Weather fetch failed"
                )
            }
        }
    }

    fun onMarkerTapped(p: LatLng) {
        // show the card immediately and start loading weather
        _uiState.value = _uiState.value.copy(
            selectedMarker = p,
            showWeatherCard = true,
            weatherLoading = true,
            weatherError = null
        )
        fetchWeatherFor(p)  // your existing network call
    }

    fun hideWeatherCard() {
        if (_uiState.value.showWeatherCard) {
            _uiState.value = _uiState.value.copy(showWeatherCard = false)
        }
    }



}
