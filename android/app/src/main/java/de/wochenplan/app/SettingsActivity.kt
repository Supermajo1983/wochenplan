package de.wochenplan.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import kotlin.concurrent.thread

/** Eingabe der JSONbin-Daten; dient auch als Konfigurations-Screen beim Platzieren des Widgets. */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Bei Abbruch darf das Widget nicht platziert werden.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        setContentView(R.layout.activity_settings)
        val binInput = findViewById<EditText>(R.id.bin_id)
        val keyInput = findViewById<EditText>(R.id.api_key)
        val (bin, key) = Plan.loadConfig(this)
        binInput.setText(bin)
        keyInput.setText(key)

        findViewById<Button>(R.id.save).setOnClickListener {
            Plan.saveConfig(this, binInput.text.toString(), keyInput.text.toString())
            val appContext = applicationContext
            thread { TodayWidgetProvider.update(appContext) }
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
