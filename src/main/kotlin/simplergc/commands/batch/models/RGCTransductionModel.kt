package simplergc.commands.batch.models

import java.util.prefs.Preferences
import org.scijava.Context
import simplergc.commands.batch.RGCBatch.OutputFormat

/** RGCTransductionModel is used to load initial values, and handles the updating and saving of parameters. **/
class RGCTransductionModel(val context: Context, private val prefs: Preferences) {
    var inputDirectory: String
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.inputDirectory, "")
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.inputDirectory, value)
        }

    var shouldProcessFilesInNestedFolders: Boolean
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.shouldProcessFilesInNestedFolders, false)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.shouldProcessFilesInNestedFolders, value)
        }

    var targetChannel: Int
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.targetChannel, 1)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.targetChannel, value)
        }

    var transductionChannel: Int
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.transductionChannel, 2)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.transductionChannel, value)
        }

    var cellDiameter: String
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.cellDiameter, "0.00-30.0")
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.cellDiameter, value)
        }

    var thresholdRadius: Int
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.thresholdRadius, 20)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.thresholdRadius, value)
        }

    var gaussianBlur: Double
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.gaussianBlur, 3.0)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.gaussianBlur, value)
        }

    var shouldRemoveAxonsFromTargetChannel: Boolean
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.shouldRemoveAxonsFromTargetChannel, false)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.shouldRemoveAxonsFromTargetChannel, value)
        }

    var shouldRemoveAxonsFromTransductionChannel: Boolean
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.shouldRemoveAxonsFromTransductionChannel, false)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.shouldRemoveAxonsFromTransductionChannel, value)
        }

    var outputFormat: String
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.outputFormat, OutputFormat.XLSX)
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.outputFormat, value)
        }

    var outputFile: String
        get() {
            return prefs.getRGCTransductionPref(PreferenceKeys.outputFile, "")
        }
        set(value) {
            prefs.putRGCTransductionPref(PreferenceKeys.outputFile, value)
        }
}
