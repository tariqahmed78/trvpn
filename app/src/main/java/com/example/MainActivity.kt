package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppPreferences
import com.example.data.local.ServerCacheManager
import com.example.data.remote.VpnGateApi
import com.example.data.repository.ServerRepository
import com.example.data.repository.VpnRepository
import com.example.domain.model.AppThemeMode
import com.example.ui.components.glass.GlassBottomBar
import com.example.ui.components.glass.LiquidGlassBackground
import com.example.ui.navigation.BottomNavDestination
import com.example.ui.screens.diagnostics.DiagnosticsDialog
import com.example.ui.screens.disclosure.PrivacyDisclosureDialog
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.locations.LocationsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.TrVpnTheme
import com.example.ui.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: VpnViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = AppPreferences(applicationContext)
        val api = VpnGateApi()
        val cacheManager = ServerCacheManager(applicationContext)
        val serverRepository = ServerRepository(api, cacheManager, preferences)
        val vpnRepository = VpnRepository(preferences, serverRepository)

        viewModel = VpnViewModel(vpnRepository, serverRepository, preferences)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val vpnState by viewModel.vpnState.collectAsState()
            val logs by viewModel.diagnosticsLogs.collectAsState()
            val selectedServer by viewModel.selectedServer.collectAsState()

            var currentDestination by remember { mutableStateOf(BottomNavDestination.HOME) }
            var showPrivacyDisclosure by remember { mutableStateOf(false) }
            var showDiagnosticsDialog by remember { mutableStateOf(false) }

            // Notification permission request for Android 13+ (API 33+)
            val notificationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Permission response handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // VPN Permission launcher for system VPN dialog
            val vpnPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.onConnectToggled(
                        context = this@MainActivity,
                        onRequirePrivacyDisclosure = { showPrivacyDisclosure = true },
                        onRequireVpnPermission = { /* already handled */ }
                    )
                }
            }

            fun requestVpnPermission() {
                val intent = VpnService.prepare(this@MainActivity)
                if (intent != null) {
                    vpnPermissionLauncher.launch(intent)
                } else {
                    viewModel.onConnectToggled(
                        context = this@MainActivity,
                        onRequirePrivacyDisclosure = { showPrivacyDisclosure = true },
                        onRequireVpnPermission = { /* already handled */ }
                    )
                }
            }

            // Predictive back handling: return to Home screen if on secondary tabs
            BackHandler(enabled = currentDestination != BottomNavDestination.HOME) {
                currentDestination = BottomNavDestination.HOME
            }

            TrVpnTheme(themeMode = themeMode) {
                LiquidGlassBackground(vpnState = vpnState) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            GlassBottomBar(
                                currentDestination = currentDestination,
                                onNavigate = { destination ->
                                    currentDestination = destination
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentDestination) {
                                BottomNavDestination.HOME -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateToLocations = {
                                            currentDestination = BottomNavDestination.LOCATIONS
                                        },
                                        onShowDiagnostics = { showDiagnosticsDialog = true },
                                        onRequirePrivacyDisclosure = { showPrivacyDisclosure = true },
                                        onRequireVpnPermission = { requestVpnPermission() }
                                    )
                                }

                                BottomNavDestination.LOCATIONS -> {
                                    LocationsScreen(viewModel = viewModel)
                                }

                                BottomNavDestination.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onShowDiagnostics = { showDiagnosticsDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // Network Privacy Notice Modal
                    if (showPrivacyDisclosure) {
                        PrivacyDisclosureDialog(
                            onAccept = {
                                showPrivacyDisclosure = false
                                viewModel.acceptPrivacyNotice {
                                    requestVpnPermission()
                                }
                            },
                            onDecline = {
                                showPrivacyDisclosure = false
                            }
                        )
                    }

                    // Connection Diagnostics Modal
                    if (showDiagnosticsDialog) {
                        DiagnosticsDialog(
                            logs = logs,
                            vpnState = vpnState,
                            activeServer = selectedServer,
                            onDismiss = { showDiagnosticsDialog = false }
                        )
                    }
                }
            }
        }
    }
}
