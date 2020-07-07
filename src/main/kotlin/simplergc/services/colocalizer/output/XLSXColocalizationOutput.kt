package simplergc.services.colocalizer.output

import java.io.File
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.SimpleOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class XLSXColocalizationOutput(
    private val result: TransductionResult,
    private val outputFile: File
) : SimpleOutput() {

    override fun output() {

        // Create Excel File.
        val workbook = XSSFWorkbook()

        // Create sheets
        val docSheet = workbook.createSheet("Documentation")
        val docInfo = ArrayList<Array<String>>(
            arrayListOf(
                arrayOf("The article: ", "TODO: Insert citation"),
                arrayOf("", ""),
                arrayOf("Abbreviation", "Description"),
                arrayOf("Summary", "Key measurements per image"),
                arrayOf("Transduced cells analysis", "Per-cell metrics of transduced cells"),
                arrayOf("Parameters", "Parameters used to run the SimpleRGC plugin")
            )
        )
        docInfo.forEachIndexed { rowIdx, rows ->
            run {
                val row = docSheet.createRow(rowIdx)
                rows.forEachIndexed { colIdx, str ->
                    run {
                        val cell = row.createCell(colIdx)
                        cell.setCellValue(str)
                    }
                }
            }
        }

        // TODO: Modify following 2 sheets to work on a per file basis
        val summarySheet = workbook.createSheet("Summary")
        val summaryHeader = arrayOf(
            "File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)",
            "Average Morphology Area (pixel^2)",
            "Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "IntDen",
            "RawIntDen"
        )
        // Create header for summary
        val summaryHeaderRow = summarySheet.createRow(0)
        summaryHeader.forEachIndexed { columnIdx, headerTitle ->
            val cell = summaryHeaderRow.createCell(columnIdx)
            cell.setCellValue(headerTitle)
        }
        // Add summary data
        val summaryData = arrayOf(
            "1",
            result.targetCellCount.toString(),
            result.overlappingTwoChannelCells.size.toString(),
            ((result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100).toString(),
            "TODO: Average Morphology Area",
            (result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size).toString(),
            "TODO: Median",
            "TODO: Min",
            "TODO: Max",
            "TODO: IntDen",
            "TODO: RawIntDen"
        )
        val summaryDataRow = summarySheet.createRow(1)
        summaryData.forEachIndexed { columnIdx, dataEntry ->
            val cell = summaryDataRow.createCell(columnIdx)
            cell.setCellValue(dataEntry)
        }

        val perCellAnalysisSheet = workbook.createSheet("Transduced cells analysis")
        val perCellAnalysisHeader = arrayOf(
            "File Name",
            "Transduced Cell",
            "Morphology Area (pixel^2)",
            "Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "IntDen",
            "RawIntDen"
        )
        // Create header for transduced cell analysis
        val perCellAnalysisHeaderRow = perCellAnalysisSheet.createRow(0)
        perCellAnalysisHeader.forEachIndexed { columnIdx, headerTitle ->
            val cell = perCellAnalysisHeaderRow.createCell(columnIdx)
            cell.setCellValue(headerTitle)
        }
        // Add transduced cell analysis data
        result.overlappingTransducedIntensityAnalysis.forEachIndexed { rowIdx, cellAnalysis ->
            val analysisRow = perCellAnalysisSheet.createRow(rowIdx)
            val analysisData = arrayOf(
                "",
                "TODO:",
                "1",
                cellAnalysis.area.toString(),
                cellAnalysis.mean.toString(),
                cellAnalysis.median.toString(),
                "TODO: Min",
                "TODO: Max",
                "TODO: IntDen",
                "TODO: RawIntDen"
            )
            analysisData.forEachIndexed { colIdx, dataEntry ->
                val cell = analysisRow.createCell(colIdx)
                cell.setCellValue(dataEntry)
            }
        }

        val paramsSheet = workbook.createSheet("Parameters")
        val paramsHeader = arrayOf(
            "File name",
            "Simple RGC plugin",
            "Version",
            "Pixel size (micrometers)",
            "Morphology channel",
            "Transduction channel",
            "Cell diameter (px)",
            "Local threshold radius",
            "Gaussian blur sigma",
            "Exclude axons"
        )
        // TODO: insert params here

        // Write file and close streams
        val outputXlsxFile =
            if (outputFile.extension == "xlsx") outputFile else File("${outputFile.parent}${File.separator}${outputFile.nameWithoutExtension}.xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }
}
