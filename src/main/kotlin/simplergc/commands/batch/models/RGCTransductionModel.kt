package simplergc.commands.batch.models

import java.util.prefs.Preferences
import org.scijava.Context

/** RGCTransductionModel is used to load initial values, and handles the updating and saving of parameters. **/
class RGCTransductionModel(val context: Context, private val prefs: Preferences) {
    var inputDirectory: String
        get() {
            return prefs.getRGCTransductionPref(Param.inputDirectory, "")
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.inputDirectory, value)
        }

    var shouldProcessFilesInNestedFolders: Boolean
        get() {
            return prefs.getRGCTransductionPref(Param.shouldProcessFilesInNestedFolders, false)
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.shouldProcessFilesInNestedFolders, value)
        }

    var targetChannel: Int
        get() {
            return prefs.getRGCTransductionPref(Param.targetChannel, 1)
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.targetChannel, value)
        }

    var transductionChannel: Int
        get() {
            return prefs.getRGCTransductionPref(Param.transductionChannel, 2)
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.transductionChannel, value)
        }

    var cellDiameter: String
        get() {
            return prefs.getRGCTransductionPref(Param.cellDiameter, "0.00-30.0")
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.cellDiameter, value)
        }

    var thresholdRadius: Int
        get() {
            return prefs.getRGCTransductionPref(Param.thresholdRadius, 20)
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.thresholdRadius, value)
        }

    var gaussianBlur: Double
        get() {
            return prefs.getRGCTransductionPref(Param.gaussianBlur, 3.0)
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.gaussianBlur, value)
        }

    var shouldRemoveAxonsFromTargetChannel: Boolean
        get() {
            return prefs.getRGCTransductionPref(Param.shouldRemoveAxonsFromTargetChannel, false)
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.shouldRemoveAxonsFromTargetChannel, value)
        }

    var shouldRemoveAxonsFromTransductionChannel: Boolean
        get() {
            return prefs.getRGCTransductionPref(Param.shouldRemoveAxonsFromTransductionChannel, false)
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.shouldRemoveAxonsFromTransductionChannel, value)
        }

    var outputFile: String
        get() {
            return prefs.getRGCTransductionPref(Param.outputFile, "")
        }
        set(value) {
            prefs.getRGCTransductionPref(Param.outputFile, value)
        }
}
