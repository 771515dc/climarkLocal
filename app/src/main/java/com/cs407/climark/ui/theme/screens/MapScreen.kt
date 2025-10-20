// MapScreen.kt
package com.cs407.climark.ui.theme.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.climark.R
import com.cs407.climark.ui.theme.viewModels.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import com.google.maps.android.compose.MapEffect


private fun Double.format2() = String.format(java.util.Locale.US, "%.2f", this)
private fun latLngLabel(latLng: LatLng?): String =
    latLng?.let {
        val ns = if (it.latitude >= 0) "N" else "S"
        val ew = if (it.longitude >= 0) "E" else "W"
        "${kotlin.math.abs(it.latitude).format2()}°$ns • ${kotlin.math.abs(it.longitude).format2()}°$ew"
    } ?: "—"

// Use your PNG drawables
private fun weatherIconResFor(code: Int): Int = when (code) {
    0 -> R.drawable.sunny
    1, 2, 3 -> R.drawable.cloudy
    45, 48 -> R.drawable.foggy
    51, 53, 55, 56, 57 -> R.drawable.hail
    61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.rain
    71, 73, 75, 77, 85, 86 -> R.drawable.snowy
    95, 96, 99 -> R.drawable.thunder
    else -> R.drawable.cloudy
}

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.updateLocationPermission(granted)
        if (granted) viewModel.getCurrentLocation()
    }

    LaunchedEffect(Unit) {
        viewModel.initializeLocationClient(context)
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            viewModel.updateLocationPermission(true)
            viewModel.getCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Camera
    val defaultLocation = LatLng(43.0731, -89.4012)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
        }
    }

    // Snackbars
    LaunchedEffect(uiState.addMode) {
        if (uiState.addMode) snackbar.showSnackbar("Add mode: tap the map to add one marker")
    }
    LaunchedEffect(uiState.deleteMode) {
        if (uiState.deleteMode) snackbar.showSnackbar("Delete mode: tap a marker to delete, or tap map to cancel")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { paddingVals ->
        Box(Modifier.fillMaxSize().padding(paddingVals)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                properties = MapProperties(isMyLocationEnabled = uiState.locationPermissionGranted),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    when {
                        uiState.addMode -> {
                            viewModel.addMarker(latLng)    // add ONE marker, exits add mode
                            viewModel.hideWeatherCard()
                        }
                        uiState.deleteMode -> viewModel.cancelDeleteMode()
                        else -> viewModel.hideWeatherCard()
                    }
                }
            ) {
                // Current location marker (not deletable)


                MapEffect(uiState.locationPermissionGranted) { map ->
                    // When the user taps the BLUE my-location dot:
                    map.setOnMyLocationClickListener { loc ->
                        val p = LatLng(loc.latitude, loc.longitude)
                        viewModel.onMarkerTapped(p)      // show card immediately + start weather fetch
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(p, 15f))
                        }
                    }
                }

                // User markers
                uiState.markers.forEach { p ->
                    Marker(
                        state = MarkerState(p),
                        title = "Marker at ${"%.5f".format(p.latitude)}, ${"%.5f".format(p.longitude)}",
                        onClick = {
                            if (uiState.deleteMode) {
                                viewModel.removeMarker(p)
                                true
                            } else {
                                viewModel.onMarkerTapped(p)
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(p, 15f))
                                }
                                false // let default InfoWindow show
                            }
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // LOCATE FAB (on top)
                FloatingActionButton(
                    modifier = Modifier.size(60.dp),
                    onClick = {
                        viewModel.hideWeatherCard()  // match behavior: FABs dismiss the card
                        if (uiState.locationPermissionGranted) {
                            val target = uiState.currentLocation
                            if (target != null) {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(target, 15f)
                                    )
                                }
                                // Optional: also show weather for current location
                                viewModel.onMarkerTapped(target)
                            } else {
                                // Permission granted but no fix yet—try to fetch one
                                viewModel.getCurrentLocation()
                            }
                        } else {
                            // Ask for permission if not granted
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Center on my location",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // ADD FAB
                FloatingActionButton(
                    modifier = Modifier.size(60.dp),
                    onClick = {
                        viewModel.hideWeatherCard()
                        viewModel.toggleAddMode()
                    },
                    containerColor = if (uiState.addMode)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.background,
                    contentColor = if (uiState.addMode)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onBackground
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add marker", modifier = Modifier.size(32.dp))
                }

                // DELETE FAB
                FloatingActionButton(
                    modifier = Modifier.size(60.dp),
                    onClick = {
                        viewModel.hideWeatherCard()
                        viewModel.toggleDeleteMode()
                    },
                    containerColor = if (uiState.deleteMode)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.background,
                    contentColor = if (uiState.deleteMode)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onBackground
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete marker", modifier = Modifier.size(32.dp))
                }
            }


            // SINGLE Weather Info Card — appears only when showWeatherCard = true
            if (uiState.showWeatherCard) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .sizeIn(minHeight = 84.dp)
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "Location: ${uiState.weatherLocationLabel ?: latLngLabel(uiState.selectedMarker)}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))

                        if (uiState.weatherLoading) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading weather…")
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(uiState.weather) { d ->
                                    val isToday = d.dateIso == java.time.LocalDate.now().toString()
                                    val weight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(d.dayLabel, fontWeight = weight)
                                        Image(
                                            painter = painterResource(weatherIconResFor(d.weatherCode)),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text("${d.precipPct}%")
                                        Text("${d.tMax}°", fontWeight = weight)
                                        Text("${d.tMin}°")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
