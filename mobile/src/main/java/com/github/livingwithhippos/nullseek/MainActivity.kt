package com.github.livingwithhippos.nullseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
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

            Box(Modifier.safeDrawingPadding()) {
                NullSeekTheme {
                    NullSeekApp(
                        windowSize = windowSize,
                        rootUiState = uiState,
                        checkValidToken = { provider, token ->
                            viewModel.checkValidToken(provider, token)
                        },
                        loadTorrentDetails = { torrent ->
                            viewModel.loadTorrentDetails(torrent, false)
                        },
                        showSettings = { show ->
                            viewModel.showSettings(show)
                        },
                    )
                }
            }
        }
    }
}
