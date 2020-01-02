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
    val gaussianBlurSigma: Double = 3.0,
    // For use with blue cell channel
    val largestAllCellsDiameter: Double? = null
)

private fun renderParamsDialog(paramsDialog: GenericDialog, defaultParams: PreprocessingParameters) {
    paramsDialog.addNumericField("Largest Cell Diameter", defaultParams.largestCellDiameter, 0)
    if (defaultParams.largestAllCellsDiameter != null) {
        paramsDialog.addNumericField(
            "Largest Cell Diameter in Morphology Channel 2",
            defaultParams.largestAllCellsDiameter,
            0
        )
    }
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

private fun getParamsFromDialog(paramsDialog: GenericDialog, isAllCellsEnabled: Boolean = false): PreprocessingParameters {
    val largestCellDiameter = paramsDialog.nextNumber
    val largestAllCellsDiameter = if (isAllCellsEnabled) paramsDialog.nextNumber else null
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
        gaussianBlurSigma,
        largestAllCellsDiameter = largestAllCellsDiameter
    )
}

fun tuneParameters(largestCellDiameter: Double, largestAllCellsDiameter: Double? = null): PreprocessingParameters? {
    val defaultParams = PreprocessingParameters(largestCellDiameter = largestCellDiameter, largestAllCellsDiameter = largestAllCellsDiameter)
    val paramsDialog = GenericDialog("Tune Parameters Manually")
    renderParamsDialog(paramsDialog, defaultParams)
    if (paramsDialog.wasCanceled()) return null
    return getParamsFromDialog(paramsDialog, largestAllCellsDiameter != null)
}
