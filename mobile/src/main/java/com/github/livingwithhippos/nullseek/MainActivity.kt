package com.github.livingwithhippos.nullseek

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.shouldUseDarkTheme
import com.github.livingwithhippos.nullseek.widget.EXTRA_WIDGET_DESTINATION
import com.github.livingwithhippos.nullseek.widget.TorboxWidgetUpdateWorker
import org.koin.androidx.viewmodel.ext.android.viewModel

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class MainActivity : ComponentActivity() {
    private val viewModel: NullRootViewModel by viewModel()
    private var pendingWidgetDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingWidgetDestination = intent?.getStringExtra(EXTRA_WIDGET_DESTINATION)

        setContent {
            val windowSize = calculateWindowSizeClass(this)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
            val darkTheme = shouldUseDarkTheme(themeMode)

            LaunchedEffect(uiState.user) {
                if (uiState.user != null) {
                    TorboxWidgetUpdateWorker.schedulePeriodic(applicationContext)
                    TorboxWidgetUpdateWorker.enqueueImmediate(applicationContext)
                }
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle =
                        if (darkTheme) {
                            SystemBarStyle.dark(Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        },
                    navigationBarStyle =
                        if (darkTheme) {
                            SystemBarStyle.dark(Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        },
                )
            }

            NullSeekTheme(
                darkTheme = darkTheme,
                themeColor = themeColor,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.safeDrawingPadding()) {
                        NullSeekApp(
                            windowSize = windowSize,
                            rootUiState = uiState,
                            pendingWidgetDestination = pendingWidgetDestination,
                            onWidgetDestinationConsumed = {
                                pendingWidgetDestination = null
                            },
                            reloadProviders = {
                                viewModel.loadActiveToken()
                                TorboxWidgetUpdateWorker.enqueueImmediate(applicationContext)
                            },
                            checkValidToken = { provider, token ->
                                viewModel.checkValidToken(provider, token)
                            },
                            loadTorrentDetails = { torrent ->
                                viewModel.loadTorrentDetails(torrent, false)
                            },
                            loadWebDetails = { webDownload ->
                                viewModel.loadWebDetails(webDownload, false)
                            },
                            loadUsenetDetails = { usenetDownload ->
                                viewModel.loadUsenetDetails(usenetDownload, false)
                            },
                            closeDetails = {
                                viewModel.closeDetails()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_WIDGET_DESTINATION)?.let { destination ->
            pendingWidgetDestination = destination
        }
    }
}
