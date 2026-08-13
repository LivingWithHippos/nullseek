package com.github.livingwithhippos.nullseek.ui.utils

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.ui.theme.CustomTypography
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.utils.setClipboardText

@Composable
fun CardStat(
    title: String,
    description: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.bodySmall)
        Text(
            text = description,
            style = CustomTypography.cardBoldValue,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
@Preview
fun CardStatPreview() {
    NullSeekTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CardStat(title = "Seeds", description = "123")
        }
    }
}

@Composable
fun CopyButton(
    text: String?,
    description: String,
    @DrawableRes
    customIcon: Int? = null,
    context: Context = LocalContext.current,
) {
    if (text == null) return

    IconButton(onClick = {
        setClipboardText(context, text)
    }) {
        Icon(
            painter = painterResource(customIcon ?: R.drawable.ic_copy),
            contentDescription = description
        )
    }
}