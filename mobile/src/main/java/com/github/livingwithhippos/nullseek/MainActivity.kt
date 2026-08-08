package com.github.livingwithhippos.nullseek

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.livingwithhippos.nullseek.ui.provider.ProviderEditor
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NullSeekTheme {
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }) { innerPadding ->
                    ProviderEditor(
                        modifier = Modifier.padding(innerPadding),
                        onSave = { provider, token ->
                            viewModel.saveToken(baseContext, provider, token)
                        },
                        onSnack = { text, duration, label ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = text,
                                    duration = duration,
                                    actionLabel = label ?: "Dismiss"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}