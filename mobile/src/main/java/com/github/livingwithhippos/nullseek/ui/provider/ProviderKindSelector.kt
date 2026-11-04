package com.github.livingwithhippos.nullseek.ui.provider

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind

/** Picks the debrid service of a new account. */
@Composable
fun ProviderKindSelector(
    selected: ProviderKind,
    onSelect: (ProviderKind) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val kinds = ProviderKind.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        kinds.forEachIndexed { index, kind ->
            SegmentedButton(
                selected = kind == selected,
                onClick = { onSelect(kind) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = kinds.size),
            ) {
                Text(kind.displayName)
            }
        }
    }
}
