package com.github.livingwithhippos.nullseek.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType

@Composable
fun SettingsScreen(
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Settings Screen - Navigation Type: $navigationType")
    }
}