package com.github.livingwithhippos.nullseek.ui.utils

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExtendableFabMenu(
    onNewDownload: () -> Unit,
    showSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showNewDownload: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
                containerCornerRadius = ToggleFloatingActionButtonDefaults.containerCornerRadius(0.dp, 0.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_action),
                    contentDescription = stringResource(id = R.string.expand_menu),
                )
            }
        },
        modifier = modifier.padding(16.dp),
    ) {
        if (showNewDownload) {
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
                text = {
                    Text(
                        text = stringResource(id = R.string.new_download),
                    )
                },
                modifier = Modifier.widthIn(min = 210.dp),
            )
        }
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
            text = {
                Text(
                    text = stringResource(id = R.string.settings),
                )
            },
            modifier = Modifier.widthIn(min = 210.dp)
        )
    }
}

@Preview
@Composable
fun ExtendableFabMenuPreview() {

    NullSeekTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ExtendableFabMenu(
                onNewDownload = {},
                showSettings = {},
            )
        }
    }

}