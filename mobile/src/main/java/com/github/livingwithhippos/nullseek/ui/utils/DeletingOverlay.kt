package com.github.livingwithhippos.nullseek.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Dims the screen and swallows taps while a delete request runs.
 * Scrolling and the back gesture keep working.
 */
@Composable
fun BoxScope.DeletingOverlay(visible: Boolean) {
    if (!visible) return
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
                .clickable(interactionSource = null, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
