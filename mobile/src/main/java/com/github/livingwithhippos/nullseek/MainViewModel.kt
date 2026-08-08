package com.github.livingwithhippos.nullseek

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    fun saveToken(context: Context, provider: String, token: String) {

        viewModelScope.launch {
            context.dataStore.updateData {
                it.toMutablePreferences().also {preferences ->
                    preferences[stringPreferencesKey(provider)] = token
                }
            }
        }
    }
}