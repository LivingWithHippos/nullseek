package com.github.livingwithhippos.nullseek.ui.utils

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.ui.theme.CustomTypography
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory
import com.github.livingwithhippos.nullseek.utils.setClipboardText

@Composable
fun CardStat(
    titles: List<String>,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        titles.forEach { title ->
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
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
            CardStat(titles = listOf("Seeds", "Total"), description = "123")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CopyButton(
    text: String?,
    description: String,
    modifier: Modifier = Modifier,
    @DrawableRes
    customIcon: Int? = null,
    context: Context = LocalContext.current,
) {
    if (text == null) return

    OutlinedIconButton(
        onClick = {
            setClipboardText(context, text)
        },
        modifier = modifier,
        border = ButtonDefaults.outlinedButtonBorder(),
        colors =
            IconButtonDefaults.outlinedIconButtonVibrantColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
    ) {
        Icon(
            painter = painterResource(customIcon ?: R.drawable.ic_copy),
            contentDescription = description,
        )
    }
}

@Composable
@Preview
fun CopyButtonPreview() {
    NullSeekTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CopyButton(text = "Sample Text", description = "Copy Sample Text")
        }
    }
}

@Composable
fun IconText(
    text: String,
    @DrawableRes
    icon: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
@Preview
fun IconTextPreview() {
    NullSeekTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            IconText(text = "Sample Text", icon = R.drawable.ic_magnet)
        }
    }
}

@Composable
fun FileTypeBadge(
    category: FileTypeCategory,
    modifier: Modifier = Modifier,
) {
    val labelRes =
        when (category) {
            FileTypeCategory.VIDEO -> R.string.file_type_video
            FileTypeCategory.AUDIO -> R.string.file_type_audio
            FileTypeCategory.DOCUMENT -> R.string.file_type_document
            FileTypeCategory.BOOK -> R.string.file_type_book
            FileTypeCategory.ARCHIVE -> R.string.file_type_archive
            FileTypeCategory.SUBTITLE -> R.string.file_type_subtitle
            FileTypeCategory.EXECUTABLE -> R.string.file_type_executable
        }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
@Preview
fun FileTypeBadgePreview() {
    NullSeekTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            FileTypeBadge(category = FileTypeCategory.VIDEO)
        }
    }
}