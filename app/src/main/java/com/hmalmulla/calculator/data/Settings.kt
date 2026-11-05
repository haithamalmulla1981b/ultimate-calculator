package com.hmalmulla.calculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hmalmulla.calculator.engine.AngleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Palette(val label: String) { CLASSIC("Classic blue"), NAVY("Navy and blue"), GOLD("Gold accent"), CUSTOM("Custom accent") }
enum class SizeOption(val label: String, val scale: Float) { SMALL("Small", 0.88f), MEDIUM("Medium", 1f), LARGE("Large", 1.14f) }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val palette: Palette = Palette.NAVY,
    val customAccent: Int = 0xFF7C4DFF.toInt(),
    val sound: Boolean = false,
    val haptics: Boolean = true,
    val decimals: Int = 10,
    val grouping: Boolean = true,
    val defaultAngle: AngleMode = AngleMode.DEG,
    val fontSize: SizeOption = SizeOption.MEDIUM,
    val buttonSize: SizeOption = SizeOption.MEDIUM,
    val livePreview: Boolean = true,
    val saveHistory: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val amoled = booleanPreferencesKey("amoled")
        val palette = stringPreferencesKey("palette")
        val accent = intPreferencesKey("accent")
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val decimals = intPreferencesKey("decimals")
        val grouping = booleanPreferencesKey("grouping")
        val angle = stringPreferencesKey("angle")
        val font = stringPreferencesKey("font")
        val button = stringPreferencesKey("button")
        val preview = booleanPreferencesKey("preview")
        val saveHistory = booleanPreferencesKey("save_history")
        val expression = stringPreferencesKey("state_expression")
        val memory = stringPreferencesKey("state_memory")
        val rates = stringPreferencesKey("currency_rates")
        val ratesTime = longPreferencesKey("currency_time")
        val ratesBase = stringPreferencesKey("currency_base")
    }

    private inline fun <reified E : Enum<E>> enumOf(v: String?, d: E): E =
        v?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: d

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            themeMode = enumOf(p[K.theme], d.themeMode),
            amoled = p[K.amoled] ?: d.amoled,
            palette = enumOf(p[K.palette], d.palette),
            customAccent = p[K.accent] ?: d.customAccent,
            sound = p[K.sound] ?: d.sound,
            haptics = p[K.haptics] ?: d.haptics,
            decimals = p[K.decimals] ?: d.decimals,
            grouping = p[K.grouping] ?: d.grouping,
            defaultAngle = enumOf(p[K.angle], d.defaultAngle),
            fontSize = enumOf(p[K.font], d.fontSize),
            buttonSize = enumOf(p[K.button], d.buttonSize),
            livePreview = p[K.preview] ?: d.livePreview,
            saveHistory = p[K.saveHistory] ?: d.saveHistory,
        )
    }

    suspend fun update(transform: (Settings) -> Settings) {
        val s = transform(settings.first())
        context.dataStore.edit { p ->
            p[K.theme] = s.themeMode.name
            p[K.amoled] = s.amoled
            p[K.palette] = s.palette.name
            p[K.accent] = s.customAccent
            p[K.sound] = s.sound
            p[K.haptics] = s.haptics
            p[K.decimals] = s.decimals
            p[K.grouping] = s.grouping
            p[K.angle] = s.defaultAngle.name
            p[K.font] = s.fontSize.name
            p[K.button] = s.buttonSize.name
            p[K.preview] = s.livePreview
            p[K.saveHistory] = s.saveHistory
        }
    }

    suspend fun reset() {
        context.dataStore.edit { p ->
            listOf(K.theme, K.palette, K.angle, K.font, K.button).forEach { p.remove(it) }
            listOf(K.amoled, K.sound, K.haptics, K.grouping, K.preview, K.saveHistory).forEach { p.remove(it) }
            p.remove(K.accent); p.remove(K.decimals)
        }
    }

    // Calculator state survives app restarts.
    suspend fun loadState(): Pair<String, String> {
        val p = context.dataStore.data.first()
        return (p[K.expression] ?: "") to (p[K.memory] ?: "")
    }

    suspend fun saveState(expression: String, memory: String) {
        context.dataStore.edit { it[K.expression] = expression; it[K.memory] = memory }
    }

    // Cached exchange rates (never invented: only what was downloaded).
    data class CachedRates(val json: String, val time: Long, val base: String)

    suspend fun loadRates(): CachedRates? {
        val p = context.dataStore.data.first()
        val j = p[K.rates] ?: return null
        return CachedRates(j, p[K.ratesTime] ?: 0L, p[K.ratesBase] ?: "USD")
    }

    suspend fun saveRates(r: CachedRates) {
        context.dataStore.edit { it[K.rates] = r.json; it[K.ratesTime] = r.time; it[K.ratesBase] = r.base }
    }
}
