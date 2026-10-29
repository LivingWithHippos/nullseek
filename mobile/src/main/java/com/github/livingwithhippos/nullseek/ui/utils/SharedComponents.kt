package com.github.livingwithhippos.nullseek.ui.utils

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.ui.theme.CustomTypography
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedIconButton
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
fun CachedTag(modifier: Modifier = Modifier) {

    Surface(
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(id = R.string.cached),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
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
        shape = RoundedCornerShape(0.dp),
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
fun SharedComponentsPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CardStat(titles = listOf("Seeds", "Total"), description = "123")
                CopyButton(text = "Sample Text", description = "Copy Sample Text")
                IconText(text = "Sample Text", icon = R.drawable.ic_magnet)
                CachedTag()
                FileTypeBadge(category = FileTypeCategory.VIDEO)
            }
        }
    }
}