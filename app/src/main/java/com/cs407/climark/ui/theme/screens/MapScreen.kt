package com.cs407.climark.ui.theme.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon

import androidx.compose.ui.unit.dp



@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launcher to request location permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.updateLocationPermission(granted)
        if (granted) viewModel.getCurrentLocation()
    }

    // One-time check + request
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val granted = fineGranted || coarseGranted
        viewModel.updateLocationPermission(granted)
        if (granted) {
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

    // Default (fallback) location — Madison, WI
    val defaultLocation = LatLng(43.0731, -89.4012)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    // Move camera when current location arrives
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { here ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(here, 15f))
        }
    }

    // Map


    Box(Modifier.fillMaxSize()) {
        // your GoogleMap(...)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = MapProperties(
                isMyLocationEnabled = uiState.locationPermissionGranted
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = uiState.locationPermissionGranted
            ),
            cameraPositionState = cameraPositionState
        ) {
            // Show marker when we know the user's location
            uiState.currentLocation?.let { here ->
                Marker(
                    state = MarkerState(position = here),
                    title = "You are here"
                )
            }

            // (Optional) also show any markers stored in state
            uiState.markers.forEach { p ->
                Marker(state = MarkerState(position = p))
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(all = 10.dp)
                .size(60.dp),
            onClick = { /* TODO: implement re-center after currentLocation is in ViewModel */ },
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Your Location",
                modifier = Modifier.size(50.dp)
            )
        }
    }

}
