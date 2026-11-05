package io.github.agumonshinka.schulte

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Persists widget-facing stats. "Today" is always Asia/Taipei local date.
 */
class StatsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun taipeiToday(): String = LocalDate.now(ZONE).toString()

    fun snapshot(): Snapshot {
        val today = taipeiToday()
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        val streak = prefs.getInt(KEY_STREAK, 0)
        val gamesToday = if (lastDate == today) prefs.getInt(KEY_GAMES_TODAY, 0) else 0
        val bestTodayMs = if (lastDate == today && prefs.contains(KEY_BEST_TODAY_MS)) {
            prefs.getLong(KEY_BEST_TODAY_MS, -1L).takeIf { it >= 0 }
        } else null
        val lastTimeMs = if (lastDate == today && prefs.contains(KEY_LAST_TIME_MS)) {
            prefs.getLong(KEY_LAST_TIME_MS, -1L).takeIf { it >= 0 }
        } else null
        return Snapshot(
            streak = streak,
            today = today,
            lastPlayedDate = lastDate,
            gamesToday = gamesToday,
            bestTodayMs = bestTodayMs,
            lastTimeMs = lastTimeMs
        )
    }

    /** Apply a finished-game payload from the WebView bridge. */
    fun applyResultJson(json: String): Snapshot {
        val obj = JSONObject(json)
        val today = taipeiToday()
        val reportedDate = obj.optString("date", today).ifBlank { today }
        val timeMs = when {
            obj.has("timeMs") -> obj.getLong("timeMs")
            obj.has("time") -> (obj.getDouble("time")).toLong()
            else -> -1L
        }
        val size = obj.optInt("size", 5)
        val hard = obj.optBoolean("hard", false)
        val reverse = obj.optBoolean("reverse", false)
        // Prefer explicit streak from web if present; otherwise maintain locally.
        val webStreak = if (obj.has("streak")) obj.getInt("streak") else null

        val editor = prefs.edit()
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        var streak = prefs.getInt(KEY_STREAK, 0)
        var gamesToday = prefs.getInt(KEY_GAMES_TODAY, 0)
        var bestToday = if (prefs.contains(KEY_BEST_TODAY_MS)) prefs.getLong(KEY_BEST_TODAY_MS, Long.MAX_VALUE) else Long.MAX_VALUE

        if (lastDate != today) {
            gamesToday = 0
            bestToday = Long.MAX_VALUE
            if (webStreak == null) {
                val yesterday = LocalDate.now(ZONE).minusDays(1).toString()
                streak = if (lastDate == yesterday) streak + 1 else 1
            }
        }
        if (webStreak != null) streak = webStreak

        gamesToday += 1
        if (timeMs >= 0 && timeMs < bestToday) bestToday = timeMs

        editor.putString(KEY_LAST_DATE, today)
            .putInt(KEY_STREAK, streak.coerceAtLeast(0))
            .putInt(KEY_GAMES_TODAY, gamesToday)
            .putLong(KEY_LAST_TIME_MS, timeMs.coerceAtLeast(0))
            .putInt(KEY_LAST_SIZE, size)
            .putBoolean(KEY_LAST_HARD, hard)
            .putBoolean(KEY_LAST_REVERSE, reverse)
            .putString(KEY_REPORTED_DATE, reportedDate)
        if (bestToday != Long.MAX_VALUE) {
            editor.putLong(KEY_BEST_TODAY_MS, bestToday)
        }
        editor.apply()
        return snapshot()
    }

    fun applyStatsJson(json: String): Snapshot {
        val obj = JSONObject(json)
        val today = taipeiToday()
        val editor = prefs.edit()
        if (obj.has("streak")) editor.putInt(KEY_STREAK, obj.getInt("streak").coerceAtLeast(0))
        val lastPlay = if (obj.has("lastPlayDate") && !obj.isNull("lastPlayDate")) obj.getString("lastPlayDate") else null
        if (!lastPlay.isNullOrBlank()) editor.putString(KEY_LAST_DATE, lastPlay)
        if (obj.has("gamesToday")) {
            editor.putInt(KEY_GAMES_TODAY, obj.getInt("gamesToday").coerceAtLeast(0))
        }
        if (obj.has("bestTodayMs")) {
            val best = obj.getLong("bestTodayMs")
            if (best >= 0) editor.putLong(KEY_BEST_TODAY_MS, best)
        } else if (obj.has("bestToday")) {
            val best = (obj.getDouble("bestToday")).toLong()
            if (best >= 0) editor.putLong(KEY_BEST_TODAY_MS, best)
        }
        // If last play wasn't today, clear today's counters for widget display consistency.
        val effectiveLast = lastPlay ?: prefs.getString(KEY_LAST_DATE, null)
        if (effectiveLast != null && effectiveLast != today) {
            editor.putInt(KEY_GAMES_TODAY, 0)
            editor.remove(KEY_BEST_TODAY_MS)
            editor.remove(KEY_LAST_TIME_MS)
        }
        editor.apply()
        return snapshot()
    }

    data class Snapshot(
        val streak: Int,
        val today: String,
        val lastPlayedDate: String?,
        val gamesToday: Int,
        val bestTodayMs: Long?,
        val lastTimeMs: Long?
    ) {
        fun bestTodaySecondsLabel(): String? =
            bestTodayMs?.let { String.format(Locale.US, "%.2f", it / 1000.0) }
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Taipei")
        private const val PREFS = "schulte_stats"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_DATE = "last_played_date"
        private const val KEY_GAMES_TODAY = "games_today"
        private const val KEY_BEST_TODAY_MS = "best_today_ms"
        private const val KEY_LAST_TIME_MS = "last_time_ms"
        private const val KEY_LAST_SIZE = "last_size"
        private const val KEY_LAST_HARD = "last_hard"
        private const val KEY_LAST_REVERSE = "last_reverse"
        private const val KEY_REPORTED_DATE = "reported_date"
    }
}
