package simplecolocalization.commands.batch

import java.util.prefs.Preferences

const val RGCCounterPrefix = "RGCCounterPrefs:"
const val RGCTransductionPrefix = "RGCTransduction:"

fun Preferences.getRGCCounterPref(prefs: Preferences, prefKey: String, def: String): String {
    return prefs.get(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCCounterPref(prefs: Preferences, prefKey: String, def: Int): Int {
    return prefs.getInt(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCCounterPref(prefs: Preferences, prefKey: String, def: Double): Double {
    return prefs.getDouble(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCCounterPref(prefs: Preferences, prefKey: String, def: Boolean): Boolean {
    return prefs.getBoolean(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefs: Preferences, prefKey: String, def: String) {
    prefs.put(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefs: Preferences, prefKey: String, def: Int) {
    prefs.putInt(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefs: Preferences, prefKey: String, def: Double) {
    prefs.putDouble(RGCCounterPrefix + prefKey, def)
}

fun Preferences.putRGCCounterPref(prefs: Preferences, prefKey: String, def: Boolean) {
    prefs.putBoolean(RGCCounterPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefs: Preferences, prefKey: String, def: String): String {
    return prefs.get(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefs: Preferences, prefKey: String, def: Int): Int {
    return prefs.getInt(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefs: Preferences, prefKey: String, def: Double): Double {
    return prefs.getDouble(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.getRGCTransductionPref(prefs: Preferences, prefKey: String, def: Boolean): Boolean {
    return prefs.getBoolean(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefs: Preferences, prefKey: String, def: String) {
    prefs.put(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefs: Preferences, prefKey: String, def: Int) {
    prefs.putInt(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefs: Preferences, prefKey: String, def: Double) {
    prefs.putDouble(RGCTransductionPrefix + prefKey, def)
}

fun Preferences.putRGCTransductionPref(prefs: Preferences, prefKey: String, def: Boolean) {
    prefs.putBoolean(RGCTransductionPrefix + prefKey, def)
}
