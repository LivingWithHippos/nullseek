package com.github.livingwithhippos.nullseek.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.NullRootViewModel
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import com.github.livingwithhippos.nullseek.tv.ui.navigation.TvNullSeekApp
import com.github.livingwithhippos.nullseek.tv.ui.theme.TvNullSeekTheme
import com.github.livingwithhippos.nullseek.tv.ui.theme.shouldUseDarkTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NullRootViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()

            TvNullSeekTheme(
                darkTheme = shouldUseDarkTheme(themeMode),
                themeColor = themeColor,
                pureBlack = themeMode == ThemeMode.AMOLED,
            ) {
                TvNullSeekApp(
                    rootUiState = uiState,
                    reloadProviders = { viewModel.loadActiveToken() },
                    checkValidToken = { provider, token -> viewModel.checkValidToken(provider, token) },
                    loadTorrentDetails = { viewModel.loadTorrentDetails(it, false) },
                    loadWebDetails = { viewModel.loadWebDetails(it, false) },
                    loadUsenetDetails = { viewModel.loadUsenetDetails(it, false) },
                    closeDetails = { viewModel.closeDetails() },
                )
            }
        }
    }
}
