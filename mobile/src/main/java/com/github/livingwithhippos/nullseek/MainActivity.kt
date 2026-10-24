package com.github.livingwithhippos.nullseek

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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.shouldUseDarkTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class MainActivity : ComponentActivity() {
    private val viewModel: NullRootViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSize = calculateWindowSizeClass(this)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
            val darkTheme = shouldUseDarkTheme(themeMode)

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
                            reloadProviders = { viewModel.loadActiveToken() },
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
}
