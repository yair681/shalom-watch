package com.tefillin.shalom

import androidx.wear.tiles.*
import androidx.wear.tiles.material.*
import androidx.wear.tiles.material.layouts.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

class AlertTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return Futures.immediateFuture(createTile(requestParams))
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build()
        )
    }

    private fun createTile(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val alerts = fetchAlerts()
        
        val text = if (alerts.isEmpty()) {
            "✅ אין אזעקות"
        } else {
            "🚨 ${alerts.first()}"
        }

        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .addContent(
                        LayoutElementBuilders.Column.Builder()
                            .addContent(
                                LayoutElementBuilders.Text.Builder()
                                    .setText(
                                        TypeBuilders.StringProp.Builder("פיקוד העורף").build()
                                    )
                                    .setFontStyle(
                                        LayoutElementBuilders.FontStyle.Builder()
                                            .setSize(TypeBuilders.FloatProp.Builder(14f).build())
                                            .setColor(
                                                ColorBuilders.argb(0xFFFFD700.toInt())
                                            )
                                            .setBold(TypeBuilders.BoolProp.Builder(true).build())
                                            .build()
                                    )
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Text.Builder()
                                    .setText(
                                        TypeBuilders.StringProp.Builder(text).build()
                                    )
                                    .setFontStyle(
                                        LayoutElementBuilders.FontStyle.Builder()
                                            .setSize(TypeBuilders.FloatProp.Builder(12f).build())
                                            .setColor(
                                                ColorBuilders.argb(0xFFFFFFFF.toInt())
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(60 * 1000L) // refresh every minute
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(layout)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun fetchAlerts(): List<String> {
        return try {
            val json = URL("https://www.oref.org.il/WarningMessages/alert/alerts.json")
                .readText(Charsets.UTF_8)
            if (json.isBlank() || json == "null") return emptyList()
            val obj = JSONObject(json)
            val data = obj.optJSONArray("data") ?: return emptyList()
            (0 until data.length()).map { data.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
