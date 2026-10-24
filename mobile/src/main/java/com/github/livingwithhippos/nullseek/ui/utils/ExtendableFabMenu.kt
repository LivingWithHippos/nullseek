package com.github.livingwithhippos.nullseek.ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.core.R
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

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showNewDownload) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            expanded = false
                            onNewDownload()
                        },
                        shape = RectangleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
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
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        showSettings()
                    },
                    shape = RectangleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
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
                    modifier = Modifier.widthIn(min = 210.dp),
                )
            }
        }

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
