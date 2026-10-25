package com.github.livingwithhippos.nullseek.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import com.github.livingwithhippos.nullseek.billing.EntitlementCache
import com.github.livingwithhippos.nullseek.core.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TorboxQuickActionsWidget :
    GlanceAppWidget(),
    KoinComponent {
    private val entitlementCache: EntitlementCache by inject()

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val isUnlocked = entitlementCache.isUnlocked()

        provideContent {
            GlanceTheme {
                QuickActionsWidgetContent(
                    context = context,
                    isUnlocked = isUnlocked,
                )
            }
        }
    }
}

class TorboxQuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TorboxQuickActionsWidget()
}

@Composable
private fun QuickActionsWidgetContent(
    context: Context,
    isUnlocked: Boolean,
) {
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isUnlocked) {
            Row(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .clickable(
                            actionStartActivity(
                                createWidgetLaunchIntent(context, WIDGET_DEST_SETTINGS),
                            ),
                        ).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = context.getString(R.string.widget_locked_title),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        maxLines = 1,
                    )
                    Text(
                        text = context.getString(R.string.widget_locked_message),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                            ),
                        maxLines = 1,
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Box(
                    modifier =
                        GlanceModifier
                            .cornerRadius(16.dp)
                            .background(GlanceTheme.colors.primary)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = context.getString(R.string.widgets_subscribe_button),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        maxLines = 1,
                    )
                }
            }
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickActionPill(
                    context = context,
                    iconRes = R.drawable.ic_add,
                    label = context.getString(R.string.widget_action_add),
                    destination = WIDGET_DEST_NEW_DOWNLOAD,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                QuickActionPill(
                    context = context,
                    iconRes = R.drawable.ic_search,
                    label = context.getString(R.string.widget_action_search),
                    destination = WIDGET_DEST_SEARCH,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                QuickActionPill(
                    context = context,
                    iconRes = R.drawable.ic_list_bullet,
                    label = context.getString(R.string.widget_action_lists),
                    destination = WIDGET_DEST_LISTS,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    context: Context,
    iconRes: Int,
    label: String,
    destination: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier =
            modifier
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .clickable(
                    actionStartActivity(
                        createWidgetLaunchIntent(context, destination),
                    ),
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = label,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = label,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            maxLines = 1,
        )
    }
}
