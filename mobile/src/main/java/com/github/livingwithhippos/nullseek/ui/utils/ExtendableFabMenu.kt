package com.github.livingwithhippos.nullseek.ui.utils

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R

@Composable
fun ExtendableFabMenu(
    onNewDownload: () -> Unit,
    showSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(id = R.string.expand_menu),
                )
            }
        },
        modifier = modifier.padding(16.dp),
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onNewDownload()
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(id = R.string.new_download),
                )
            },
            text = { Text(text = stringResource(id = R.string.new_download)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                showSettings()
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = stringResource(id = R.string.settings),
                )
            },
            text = { Text(text = stringResource(id = R.string.settings)) },
        )
    }
}
