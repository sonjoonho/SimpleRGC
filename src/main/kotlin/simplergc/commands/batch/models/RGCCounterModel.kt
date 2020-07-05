package simplergc.commands.batch.models

import java.util.prefs.Preferences
import org.scijava.Context
import simplergc.commands.batch.RGCBatch.OutputFormat

/** RGCCounterModel is used to load initial values, and handles the updating and saving of parameters. **/
class RGCCounterModel(val context: Context, private val prefs: Preferences) {
    var inputDirectory: String
        get() {
            return prefs.getRGCCounterPref(Param.inputDirectory, "")
        }
        set(value) {
            prefs.getRGCCounterPref(Param.inputDirectory, value)
        }

    var shouldProcessFilesInNestedFolders: Boolean
        get() {
            return prefs.getRGCCounterPref(Param.shouldProcessFilesInNestedFolders, false)
        }
        set(value) {
            prefs.putRGCCounterPref(Param.shouldProcessFilesInNestedFolders, value)
        }

    var channelToUse: Int
        get() {
            return prefs.getRGCCounterPref(Param.channelToUse, 1)
        }
        set(value) {
            prefs.putRGCCounterPref(Param.channelToUse, value)
        }

    var cellDiameter: String
        get() {
            return prefs.getRGCCounterPref(Param.cellDiameter, "0.00-30.0")
        }
        set(value) {
            prefs.putRGCCounterPref(Param.cellDiameter, value)
        }

    var thresholdRadius: Int
        get() {
            return prefs.getRGCCounterPref(Param.thresholdRadius, 20)
        }
        set(value) {
            prefs.putRGCCounterPref(Param.thresholdRadius, value)
        }

    var gaussianBlur: Double
        get() {
            return prefs.getRGCCounterPref(Param.gaussianBlur, 3.0)
        }
        set(value) {
            prefs.putRGCCounterPref(Param.gaussianBlur, value)
        }

    var outputFormat: String
        get() {
            return prefs.getRGCCounterPref(Param.outputFormat, OutputFormat.CSV)
        }
        set(value) {
            prefs.putRGCCounterPref(Param.outputFormat, value)
        }

    var shouldRemoveAxons: Boolean
        get() {
            return prefs.getRGCCounterPref(Param.shouldRemoveAxons, false)
        }
        set(value) {
            prefs.putRGCCounterPref(Param.shouldRemoveAxons, value)
        }

    var outputFile: String
        get() {
            return prefs.getRGCCounterPref(Param.outputFile, "")
        }
        set(value) {
            prefs.putRGCCounterPref(Param.outputFile, value)
        }
}
