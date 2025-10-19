// MapScreen.kt
package com.cs407.climark.ui.theme.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.climark.ui.theme.viewModels.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState


@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    // permission launcher (unchanged) ...
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.updateLocationPermission(granted)
        if (granted) viewModel.getCurrentLocation()
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            viewModel.updateLocationPermission(true)
            viewModel.getCurrentLocation()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Default camera + animate to current location
    val defaultLocation = LatLng(43.0731, -89.4012)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f)) }
    }

    // When delete mode turns on, announce with Snackbar
    LaunchedEffect(uiState.deleteMode) {
        if (uiState.deleteMode) {
            snackbar.showSnackbar("Delete mode: tap a marker to delete, or tap map to cancel")
        }
    }


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
                uiSettings = MapUiSettings(myLocationButtonEnabled = uiState.locationPermissionGranted),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    when {
                        uiState.addMode   -> viewModel.addMarker(latLng)
                        uiState.deleteMode -> viewModel.cancelDeleteMode()
                        else -> Unit
                    }
                }
            ) {
                // Current location marker — not deletable
                uiState.currentLocation?.let { here ->
                    Marker(
                        state = MarkerState(here),
                        title = "You are here",
                        onClick = { false }  // tapping does nothing
                    )
                }
                // User markers
                uiState.markers.forEach { p ->
                    Marker(
                        state = MarkerState(p),
                        onClick = {
                            if (uiState.deleteMode) {
                                viewModel.removeMarker(p)
                                true
                            } else false
                        }
                    )
                }
            }

            // Two FABs (Add + Delete)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // ADD FAB
                FloatingActionButton(
                    modifier = Modifier.size(60.dp),
                    onClick = { viewModel.toggleAddMode() },
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
                    onClick = { viewModel.toggleDeleteMode() },
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
        }
    }

}
