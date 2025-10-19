package com.cs407.climark.ui.theme.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // <- needed for await()

data class MapState(
    val markers: List<LatLng> = emptyList(),
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapState())
    val uiState = _uiState.asStateFlow()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun initializeLocationClient(context: Context) {
        // Use applicationContext to avoid leaking an Activity
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            // 1) start loading, clear previous error
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 2) request last known location (permission must already be granted)
                val loc = fusedLocationClient.lastLocation.await()

                // 3) handle null location
                if (loc == null) {
                    _uiState.value =
                        _uiState.value.copy(isLoading = false, error = "Location unavailable.")
                    return@launch
                }

                // 4) update state with new LatLng and also add a marker there (optional)
                val here = LatLng(loc.latitude, loc.longitude)
                _uiState.value = _uiState.value.copy(
                    currentLocation = here,
                    markers = _uiState.value.markers + here,
                    isLoading = false
                )
            } catch (se: SecurityException) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Location permission required.")
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = e.message ?: "Location error.")
            }
        }
    }

    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }
}
