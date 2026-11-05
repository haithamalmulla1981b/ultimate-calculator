package com.hmalmulla.calculator

import android.app.Application
import com.hmalmulla.calculator.data.AppDatabase
import com.hmalmulla.calculator.data.SettingsRepository

/** Simple service locator: one database and one settings store per process. */
class CalculatorApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
