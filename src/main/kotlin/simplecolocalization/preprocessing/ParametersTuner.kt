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
    val largestCellDiameter: Double,
    val shouldSubtractBackground: Boolean = false,
    val thresholdLocality: String = ThresholdTypes.LOCAL,
    val localThresholdAlgo: String = LocalThresholdAlgos.NIBLACK,
    val shouldDespeckle: Boolean = true,
    val despeckleRadius: Double = 2.0,
    val shouldGaussianBlur: Boolean = true,
    val gaussianBlurSigma: Double = 3.0
)

private fun renderParamsDialog(paramsDialog: GenericDialog, defaultParams: PreprocessingParameters) {
    paramsDialog.addNumericField("Largest Cell Diameter", defaultParams.largestCellDiameter, 0)
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
    paramsDialog.addNumericField("Despeckle Radius", defaultParams.despeckleRadius, 0)
    paramsDialog.addCheckbox("Gaussian Blur?", defaultParams.shouldGaussianBlur)
    paramsDialog.addNumericField("Gaussian Blur Sigma", defaultParams.gaussianBlurSigma, 0)
    paramsDialog.showDialog()
}

private fun getParamsFromDialog(paramsDialog: GenericDialog): PreprocessingParameters {
    val largestCellDiameter = paramsDialog.nextNumber
    val shouldSubtractBackground = paramsDialog.nextBoolean
    val thresholdLocality = paramsDialog.nextChoice
    val localThresholdAlgo = paramsDialog.nextChoice
    val shouldDespeckle = paramsDialog.nextBoolean
    val despeckleRadius = paramsDialog.nextNumber
    val shouldGaussianBlur = paramsDialog.nextBoolean
    val gaussianBlurSigma = paramsDialog.nextNumber
    return PreprocessingParameters(
        largestCellDiameter,
        shouldSubtractBackground,
        thresholdLocality,
        localThresholdAlgo,
        shouldDespeckle,
        despeckleRadius,
        shouldGaussianBlur,
        gaussianBlurSigma
    )
}

fun tuneParameters(largestCellDiameter: Double): PreprocessingParameters {
    val defaultParams = PreprocessingParameters(largestCellDiameter = largestCellDiameter)
    val paramsDialog = GenericDialog("Tune Parameters Manually")
    renderParamsDialog(paramsDialog, defaultParams)
    if (paramsDialog.wasCanceled()) throw RuntimeException()
    return getParamsFromDialog(paramsDialog)
}
