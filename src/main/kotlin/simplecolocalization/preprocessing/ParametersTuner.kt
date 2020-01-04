package simplecolocalization.preprocessing

import ij.gui.GenericDialog

/** This module contains the functions and data structures used for tuning the parameters when performing the preprocessing **/

object ThresholdTypes {
    const val GLOBAL = "Global"
    const val LOCAL = "Local"
}

object LocalThresholdAlgos {
    const val OTSU = "Otsu"
    const val BERNSEN = "Bernsen"
    const val NIBLACK = "Niblack"
}

// We suspect this value may depend on the image, we will have to find a way to
data class PreprocessingParameters(
    var largestCellDiameter: Double,
    var shouldSubtractBackground: Boolean = false,
    var thresholdLocality: String = ThresholdTypes.LOCAL,
    var localThresholdAlgo: String = LocalThresholdAlgos.NIBLACK,
    var shouldDespeckle: Boolean = true,
    var despeckleRadius: Double = 2.0,
    var shouldGaussianBlur: Boolean = true,
    var gaussianBlurSigma: Double = 3.0,
    // For use with blue cell channel
    var largestAllCellsDiameter: Double? = null
)

private fun renderParamsDialog(paramsDialog: GenericDialog, defaultParams: PreprocessingParameters) {
    paramsDialog.addCheckbox("Subtract Background?", defaultParams.shouldSubtractBackground)
    paramsDialog.addChoice("Threshold Locality", arrayOf(
        ThresholdTypes.GLOBAL,
        ThresholdTypes.LOCAL
    ), defaultParams.thresholdLocality)
    paramsDialog.addChoice("Local Thresholding Algorithm", arrayOf(
        LocalThresholdAlgos.OTSU,
        LocalThresholdAlgos.BERNSEN,
        LocalThresholdAlgos.NIBLACK
    ), defaultParams.localThresholdAlgo)
    paramsDialog.addCheckbox("Despeckle?", defaultParams.shouldDespeckle)
    paramsDialog.addNumericField("Despeckle Radius (px)", defaultParams.despeckleRadius, 0)
    paramsDialog.addCheckbox("Gaussian Blur?", defaultParams.shouldGaussianBlur)
    paramsDialog.addNumericField("Gaussian Blur Sigma (px)", defaultParams.gaussianBlurSigma, 0)
    paramsDialog.showDialog()
}

private fun updateParamsFromDialog(params: PreprocessingParameters, paramsDialog: GenericDialog) {
    params.shouldSubtractBackground = paramsDialog.nextBoolean
    params.thresholdLocality = paramsDialog.nextChoice
    params.localThresholdAlgo = paramsDialog.nextChoice
    params.shouldDespeckle = paramsDialog.nextBoolean
    params.despeckleRadius = paramsDialog.nextNumber
    params.shouldGaussianBlur = paramsDialog.nextBoolean
    params.gaussianBlurSigma = paramsDialog.nextNumber
}

fun tuneParameters(largestCellDiameter: Double, largestAllCellsDiameter: Double? = null): PreprocessingParameters? {
    val params = PreprocessingParameters(largestCellDiameter = largestCellDiameter, largestAllCellsDiameter = largestAllCellsDiameter)
    val paramsDialog = GenericDialog("Tune Parameters Manually")
    renderParamsDialog(paramsDialog, params)
    if (paramsDialog.wasCanceled()) return null
    updateParamsFromDialog(params, paramsDialog)
    return params
}
