package simplergc.commands.batch.models

import java.util.prefs.Preferences
import org.scijava.Context
import simplergc.commands.batch.RGCBatch.OutputFormat

/** RGCCounterModel is used to load initial values, and handles the updating and saving of parameters. **/
class RGCCounterModel(val context: Context, private val prefs: Preferences) {
    var inputDirectory: String
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.inputDirectory, "")
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.inputDirectory, value)
        }

    var shouldProcessFilesInNestedFolders: Boolean
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.shouldProcessFilesInNestedFolders, false)
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.shouldProcessFilesInNestedFolders, value)
        }

    var channelToUse: Int
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.channelToUse, 1)
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.channelToUse, value)
        }

    var cellDiameter: String
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.cellDiameter, "0.00-30.0")
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.cellDiameter, value)
        }

    var thresholdRadius: Int
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.thresholdRadius, 20)
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.thresholdRadius, value)
        }

    var gaussianBlur: Double
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.gaussianBlur, 3.0)
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.gaussianBlur, value)
        }

    var outputFormat: String
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.outputFormat, OutputFormat.CSV)
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.outputFormat, value)
        }

    var shouldRemoveAxons: Boolean
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.shouldRemoveAxons, false)
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.shouldRemoveAxons, value)
        }

    var outputFile: String
        get() {
            return prefs.getRGCCounterPref(PreferenceKeys.outputFile, "")
        }
        set(value) {
            prefs.putRGCCounterPref(PreferenceKeys.outputFile, value)
        }
}
