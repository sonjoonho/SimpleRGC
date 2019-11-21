package simplecolocalization.preprocessing

import ij.gui.GenericDialog
import kotlin.math.roundToInt

/** This module contains the functions and data structures used for tuning the parameters when performing the preprocessing **/

object ThresholdTypes {
    const val GLOBAL = "Global"
    const val LOCAL = "Local"
}

object GlobalThresholdAlgos {
    const val OTSU = "Otsu"
    const val MOMENTS = "Moments"
    const val SHANBHAG = "Shanbhag"
}

object LocalThresholdAlgos {
    const val OTSU = "Otsu"
    const val BERNSEN = "Bernsen"
    const val NIBLACK = "Niblack"
}

data class PreprocessingParameters(
    val shouldSubtractBackground: Boolean = true,
    val largestCellDiameter: Double = 30.0,
    val thresholdLocality: String = ThresholdTypes.GLOBAL,
    val globalThresholdAlgo: String = GlobalThresholdAlgos.OTSU,
    val localThresholdAlgo: String = LocalThresholdAlgos.OTSU,
    val localThresholdRadius: Int = 15,
    val shouldDespeckle: Boolean = true,
    val despeckleRadius: Double = 1.0,
    val shouldGaussianBlur: Boolean = true,
    val gaussianBlurSigma: Double = 3.0
)

private fun renderParamsDialog(paramsDialog: GenericDialog, defaultParams: PreprocessingParameters) {
    paramsDialog.addCheckbox("Subtract Background?", defaultParams.shouldSubtractBackground)
    paramsDialog.addNumericField("Largest Cell Diameter", defaultParams.largestCellDiameter, 0)
    paramsDialog.addChoice("Threshold Locality", arrayOf(
        ThresholdTypes.GLOBAL,
        ThresholdTypes.LOCAL
    ), defaultParams.thresholdLocality)
    paramsDialog.addChoice("Global Thresholding Algorithm", arrayOf(
        GlobalThresholdAlgos.OTSU,
        GlobalThresholdAlgos.MOMENTS,
        GlobalThresholdAlgos.SHANBHAG
    ), defaultParams.globalThresholdAlgo)
    paramsDialog.addChoice("Local Thresholding Algorithm", arrayOf(
        LocalThresholdAlgos.OTSU,
        LocalThresholdAlgos.BERNSEN,
        LocalThresholdAlgos.NIBLACK
    ), defaultParams.localThresholdAlgo)
    paramsDialog.addNumericField("Local Threshold radius", defaultParams.localThresholdRadius.toDouble(), 0)
    paramsDialog.addCheckbox("Despeckle?", defaultParams.shouldDespeckle)
    paramsDialog.addNumericField("Despeckle Radius", defaultParams.despeckleRadius, 0)
    paramsDialog.addCheckbox("Gaussian Blur?", defaultParams.shouldGaussianBlur)
    paramsDialog.addNumericField("Gaussian Blur Sigma", defaultParams.gaussianBlurSigma, 0)
    paramsDialog.showDialog()
}

private fun getParamsFromDialog(paramsDialog: GenericDialog): PreprocessingParameters {
    val shouldSubtractBackground = paramsDialog.nextBoolean
    val largestCellDiameter = paramsDialog.nextNumber
    val thresholdLocality = paramsDialog.nextChoice
    val globalThresholdAlgo = paramsDialog.nextChoice
    val localThresholdAlgo = paramsDialog.nextChoice
    val localThresholdRadius = paramsDialog.nextNumber.roundToInt()
    val shouldDespeckle = paramsDialog.nextBoolean
    val despeckleRadius = paramsDialog.nextNumber
    val shouldGaussianBlur = paramsDialog.nextBoolean
    val gaussianBlurSigma = paramsDialog.nextNumber
    return PreprocessingParameters(
        shouldSubtractBackground,
        largestCellDiameter,
        thresholdLocality,
        globalThresholdAlgo,
        localThresholdAlgo,
        localThresholdRadius,
        shouldDespeckle,
        despeckleRadius,
        shouldGaussianBlur,
        gaussianBlurSigma
    )
}

fun tuneParameters(): PreprocessingParameters {
    val defaultParams = PreprocessingParameters()
    val paramsDialog = GenericDialog("Tune Parameters Manually")
    renderParamsDialog(paramsDialog, defaultParams)
    if (paramsDialog.wasCanceled()) return defaultParams // TODO(tiger-cross): Cancel plugin!
    return getParamsFromDialog(paramsDialog)
}
