// MapViewModel.kt
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
import kotlinx.coroutines.tasks.await

data class MapState(
    val markers: List<LatLng> = emptyList(),
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleteMode: Boolean = false,
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


}
