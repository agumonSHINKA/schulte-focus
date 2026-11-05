package io.github.agumonshinka.schulte

import android.webkit.JavascriptInterface
import io.github.agumonshinka.schulte.widget.SchulteWidgetProvider

class AndroidBridge(
    private val activity: MainActivity,
    private val store: StatsStore
) {
    @JavascriptInterface
    fun reportResult(json: String) {
        val snap = store.applyResultJson(json)
        activity.runOnUiThread {
            SchulteWidgetProvider.refreshAll(activity.applicationContext)
            activity.onStatsUpdated(snap)
        }
    }

    @JavascriptInterface
    fun reportStats(json: String) {
        val snap = store.applyStatsJson(json)
        activity.runOnUiThread {
            SchulteWidgetProvider.refreshAll(activity.applicationContext)
            activity.onStatsUpdated(snap)
        }
    }
}
