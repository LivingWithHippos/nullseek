package com.github.livingwithhippos.nullseek.widget

import androidx.glance.LocalContext
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.github.livingwithhippos.nullseek.MainActivity
import com.github.livingwithhippos.nullseek.billing.EntitlementCache
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TorboxDownloadsWidget :
    GlanceAppWidget(),
    KoinComponent {
    private val entitlementCache: EntitlementCache by inject()
    private val widgetRepository: TorboxWidgetRepository by inject()

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val isUnlocked = entitlementCache.isUnlocked()
        val snapshot =
            if (isUnlocked) {
                val cached = widgetRepository.getSnapshot()
                if (cached.lastUpdatedEpochMillis == 0L) {
                    TorboxWidgetUpdateWorker.enqueueImmediate(context)
                }
                cached
            } else {
                DownloadsWidgetSnapshot()
            }

        provideContent {
            GlanceTheme {
                DownloadsWidgetContent(
                    context = context,
                    isUnlocked = isUnlocked,
                    snapshot = snapshot,
                )
            }
        }
    }
}

class TorboxDownloadsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TorboxDownloadsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        TorboxWidgetUpdateWorker.schedulePeriodic(context)
        TorboxWidgetUpdateWorker.enqueueImmediate(context)
    }
}

class RefreshTorboxWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        TorboxWidgetUpdateWorker.enqueueImmediate(context)
    }
}

internal fun createWidgetLaunchIntent(
    context: Context,
    destination: String,
): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_WIDGET_DESTINATION, destination)
    }

@Composable
private fun DownloadsWidgetContent(
    context: Context,
    isUnlocked: Boolean,
    snapshot: DownloadsWidgetSnapshot,
) {
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp),
    ) {
        if (!isUnlocked) {
            LockedWidgetView(
                title = context.getString(R.string.widget_locked_title),
                message = context.getString(R.string.widget_locked_message),
                buttonLabel = context.getString(R.string.widgets_subscribe_button),
                unlockIntent = createWidgetLaunchIntent(context, WIDGET_DEST_SETTINGS),
            )
        } else if (!snapshot.hasProvider) {
            LockedWidgetView(
                title = context.getString(R.string.widget_setup_needed_title),
                message = context.getString(R.string.widget_setup_needed_message),
                buttonLabel = context.getString(R.string.app_name),
                unlockIntent = createWidgetLaunchIntent(context, WIDGET_DEST_SETTINGS),
            )
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                DownloadsWidgetHeader(
                    context = context,
                    activeCount = snapshot.activeCount,
                )
                Spacer(modifier = GlanceModifier.height(8.dp))

                if (snapshot.items.isEmpty()) {
                    Box(
                        modifier =
                            GlanceModifier
                                .fillMaxSize()
                                .clickable(
                                    actionStartActivity(
                                        createWidgetLaunchIntent(context, WIDGET_DEST_LISTS),
                                    ),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = snapshot.error ?: context.getString(R.string.widget_no_downloads),
                            style =
                                TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(snapshot.items, itemId = { it.id.toLong() }) { item ->
                            Column(
                                modifier =
                                    GlanceModifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                            ) {
                                DownloadWidgetRow(
                                    context = context,
                                    item = item,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsWidgetHeader(
    context: Context,
    activeCount: Int,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity(
                            createWidgetLaunchIntent(context, WIDGET_DEST_LISTS),
                        ),
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_downloads_label),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
            )
            if (activeCount > 0) {
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = LocalContext.current.getString(R.string.widget_active_count, activeCount),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier =
                GlanceModifier
                    .size(32.dp)
                    .cornerRadius(8.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
                    .clickable(
                        actionStartActivity(
                            createWidgetLaunchIntent(context, WIDGET_DEST_NEW_DOWNLOAD),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_add),
                contentDescription = context.getString(R.string.widget_action_add),
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            )
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        Box(
            modifier =
                GlanceModifier
                    .size(32.dp)
                    .cornerRadius(8.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .clickable(actionRunCallback<RefreshTorboxWidgetAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.widget_action_refresh),
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun DownloadWidgetRow(
    context: Context,
    item: WidgetDownloadItem,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .cornerRadius(10.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(
                    actionStartActivity(
                        createWidgetLaunchIntent(context, WIDGET_DEST_LISTS),
                    ),
                ),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.name,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = item.statusLabel,
                style =
                    TextStyle(
                        color =
                            if (item.isFinished) {
                                GlanceTheme.colors.primary
                            } else {
                                GlanceTheme.colors.onSurfaceVariant
                            },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
            )
        }

        if (!item.isFinished && item.progress > 0.0) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = item.progress.toFloat().coerceIn(0f, 1f),
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .height(4.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.surface,
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val typeTag =
                when (item.type) {
                    WidgetDownloadType.TORRENT -> LocalContext.current.getString(R.string.type_torrent)
                    WidgetDownloadType.WEB -> LocalContext.current.getString(R.string.type_web)
                    WidgetDownloadType.USENET -> LocalContext.current.getString(R.string.type_usenet)
                }
            Text(
                text = "$typeTag • ${convertToSize(item.sizeBytes)}",
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )

            if (!item.isFinished && item.downloadSpeedBytes > 0) {
                Text(
                    text = "${convertToSize(item.downloadSpeedBytes)}/s",
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun LockedWidgetView(
    title: String,
    message: String,
    buttonLabel: String,
    unlockIntent: Intent,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(unlockIntent))
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = message,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            maxLines = 2,
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Box(
            modifier =
                GlanceModifier
                    .cornerRadius(20.dp)
                    .background(GlanceTheme.colors.primary)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clickable(actionStartActivity(unlockIntent)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = buttonLabel,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
            )
        }
    }
}
