package de.wochenplan.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

/** Gleiche Aufgaben, Tages- und Wochen-Schlüssel wie in der Web-App (index.html). */
object Plan {
    const val PREFS = "wochenplan"
    private const val KEY_BIN = "binId"
    private const val KEY_API = "apiKey"
    private const val KEY_CACHE = "cache"

    private val WEEKDAY_TASKS = listOf(
        "Kinder fertig machen", "Emmi Morgens", "Louisa bringen", "Ben bringen", "Emmi Abends",
        "Louisa holen", "Ben holen", "Abendessen machen", "Ben ins Bett", "Louisa ins Bett"
    )
    private val WEEKEND_TASKS = listOf("Ben ins Bett", "Louisa ins Bett")
    private val DAY_KEYS = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    fun isConfigured(ctx: Context) =
        !ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_BIN, "").isNullOrBlank()

    fun saveConfig(ctx: Context, binId: String, apiKey: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BIN, binId.trim())
            .putString(KEY_API, apiKey.trim())
            .remove(KEY_CACHE)
            .apply()
    }

    fun loadConfig(ctx: Context): Pair<String, String> {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (p.getString(KEY_BIN, "") ?: "") to (p.getString(KEY_API, "") ?: "")
    }

    /** Lädt den Stand aus JSONbin; bei Fehler wird der letzte gecachte Stand genutzt (second = true). */
    fun load(ctx: Context): Pair<JSONObject?, Boolean> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val (binId, apiKey) = loadConfig(ctx)
        try {
            val url = URL("https://api.jsonbin.io/v3/b/${java.net.URLEncoder.encode(binId, "UTF-8")}/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (apiKey.isNotBlank()) conn.setRequestProperty("X-Master-Key", apiKey)
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            val record = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                .optJSONObject("record") ?: JSONObject()
            prefs.edit().putString(KEY_CACHE, record.toString()).apply()
            return record to false
        } catch (e: Exception) {
            val cached = prefs.getString(KEY_CACHE, null)
            return (cached?.let { JSONObject(it) }) to true
        }
    }

    fun todayTitle(today: LocalDate = LocalDate.now()): String {
        val name = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMANY)
        return "🏡 %s %02d.%02d.".format(name, today.dayOfMonth, today.monthValue)
    }

    class Today(val marco: List<String>, val julia: List<String>, val open: List<String>)

    fun todayAssignments(record: JSONObject?, today: LocalDate = LocalDate.now()): Today {
        val weekId = "%d-KW%02d".format(
            today.get(IsoFields.WEEK_BASED_YEAR), today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        )
        val dayKey = DAY_KEYS[today.dayOfWeek.value - 1]
        val weekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
        val day = record?.optJSONObject(weekId)?.optJSONObject(dayKey)
        val marco = mutableListOf<String>()
        val julia = mutableListOf<String>()
        val open = mutableListOf<String>()
        for (task in if (weekend) WEEKEND_TASKS else WEEKDAY_TASKS) {
            when (day?.optString(task, "tbd.")) {
                "Marco" -> marco.add(task)
                "Julia" -> julia.add(task)
                else -> open.add(task)
            }
        }
        return Today(marco, julia, open)
    }
}
