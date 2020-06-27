package simplergc.commands.batch

import java.util.prefs.Preferences

const val RGCCounterPrefix = "RGCCounterPrefs:"
const val RGCTransductionPrefix = "RGCTransduction:"

fun Preferences.getRGCCounterPref(prefKey: String, def: String): String {
    return this.get(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCCounterPref(prefKey: String, def: Int): Int {
    return this.getInt(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCCounterPref(prefKey: String, def: Double): Double {
    return this.getDouble(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCCounterPref(prefKey: String, def: Boolean): Boolean {
    return this.getBoolean(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefKey: String, def: String) {
    this.put(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefKey: String, def: Int) {
    this.putInt(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefKey: String, def: Double) {
    this.putDouble(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefKey: String, def: Boolean) {
    this.putBoolean(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefKey: String, def: String): String {
    return this.get(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefKey: String, def: Int): Int {
    return this.getInt(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefKey: String, def: Double): Double {
    return this.getDouble(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefKey: String, def: Boolean): Boolean {
    return this.getBoolean(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefKey: String, def: String) {
    this.put(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefKey: String, def: Int) {
    this.putInt(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefKey: String, def: Double) {
    this.putDouble(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefKey: String, def: Boolean) {
    this.putBoolean(RGCTransductionPrefix + prefKey, def)
}
