package com.moztro.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.moztro.app.ui.HomeScreen
import com.moztro.app.ui.theme.MoztroTheme
import com.moztro.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Launcher for WRITE_EXTERNAL_STORAGE on Android ≤ 9
    private val writeStorageLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            viewModel.ensureDirectoryStructureNow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request storage permissions so Moztro folder can be created
        requestStoragePermissions()

        setContent {
            val isFullscreen by viewModel.isFullscreen.collectAsState()

            LaunchedEffect(isFullscreen) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (isFullscreen) {
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            MoztroTheme {
                HomeScreen(viewModel = viewModel)
            }
        }

        // Handle initial share intent if app was launched via Share Sheet
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_SEND -> {
                // Check if extra stream (single file / image / doc / video / etc.)
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }

                if (streamUri != null) {
                    viewModel.handleSharedFiles(listOf(streamUri), applicationContext)
                } else {
                    // Check if plain text / link
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.handleSharedText(sharedText, applicationContext)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val streamUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }

                if (!streamUris.isNullOrEmpty()) {
                    val validUris = streamUris.filterNotNull()
                    if (validUris.isNotEmpty()) {
                        viewModel.handleSharedFiles(validUris, applicationContext)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // When returning from MANAGE_APP_ALL_FILES_ACCESS_PERMISSION settings screen,
        // check if permission was granted and create directories
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                viewModel.ensureDirectoryStructureNow()
            }
        }
    }

    private fun requestStoragePermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ — must use MANAGE_EXTERNAL_STORAGE via Settings
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to general manage all files page
                        try {
                            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        } catch (_: Exception) {}
                    }
                }
                // ensureDirectoryStructure will run in onResume once permission is granted
            }
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
                // Android 9 and below — runtime WRITE_EXTERNAL_STORAGE
                val writeGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (!writeGranted) {
                    writeStorageLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
                // If already granted, ViewModel init will handle it
            }
            // Android 10 — no MANAGE_EXTERNAL_STORAGE; uses requestLegacyExternalStorage in manifest
            else -> { /* ensureDirectoryStructure handled by ViewModel init */ }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            viewModel.onAppExit()
        }
    }
}
