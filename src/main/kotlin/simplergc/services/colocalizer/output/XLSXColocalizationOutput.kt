package simplergc.services.colocalizer.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.commands.RGCTransduction.TransductionParameters

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class XLSXColocalizationOutput(
    private val transductionParameters: TransductionParameters
) : ColocalizationOutput() {

    companion object {
        const val HEADER_OFFSET = 1
    }

    override fun output() {

        val workbook = XSSFWorkbook()

        writeDocSheet(workbook)

        val headerCellStyle = createHeaderCellStyle(workbook)

        writeSummarySheet(workbook, headerCellStyle)

        writeTransductionAnalysisSheet(workbook, headerCellStyle)

        writeParamsSheet(workbook, headerCellStyle)

        autoSizeColumns(workbook)

        // Write file and close streams
        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    private fun writeDocSheet(workbook: XSSFWorkbook) {
        val docSheet = workbook.createSheet("Documentation")
        val docInfo = arrayListOf(
            listOf("The article: ", "TODO: Insert citation"),
            listOf("", ""),
            listOf("Abbreviation", "Description"),
            listOf("Summary", "Key measurements per image"),
            listOf("Transduced cells analysis", "Per-cell metrics of transduced cells"),
            listOf("Parameters", "Parameters used to run the SimpleRGC plugin")
        )
        docInfo.forEachIndexed { rowIdx, rows ->
            val row = docSheet.createRow(rowIdx)
            rows.forEachIndexed { colIdx, str ->
                val cell = row.createCell(colIdx)
                cell.setCellValue(str)
            }
        }
    }

    private fun writeSummarySheet(
        workbook: XSSFWorkbook,
        headerCellStyle: XSSFCellStyle?
    ) {
        val summarySheet = workbook.createSheet("Summary")
        val summaryHeader = listOf(
            "File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)",
            "Average Morphology Area (pixel^2)",
            "Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen"
        )
        // Create header for summary
        val summaryHeaderRow = summarySheet.createRow(0)
        summaryHeader.forEachIndexed { columnIdx, headerTitle ->
            val cell = summaryHeaderRow.createCell(columnIdx)
            cell.setCellValue(headerTitle)
            cell.cellStyle = headerCellStyle
        }
        // Add summary data
        fileNameAndResultsList.forEachIndexed { rowIdx, fileNameAndResult ->
            val summaryData = listOf(
                fileNameAndResult.first,
                fileNameAndResult.second.targetCellCount.toString(),
                fileNameAndResult.second.overlappingTwoChannelCells.size.toString(),
                ((fileNameAndResult.second.overlappingTwoChannelCells.size / fileNameAndResult.second.targetCellCount.toDouble()) * 100).toString(),
                (fileNameAndResult.second.overlappingTransducedIntensityAnalysis.sumBy { it.area } / fileNameAndResult.second.overlappingTransducedIntensityAnalysis.size).toString(),
                (fileNameAndResult.second.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / fileNameAndResult.second.overlappingTransducedIntensityAnalysis.size).toString(),
                (fileNameAndResult.second.overlappingTransducedIntensityAnalysis.sumBy { it.median } / fileNameAndResult.second.overlappingTransducedIntensityAnalysis.size).toString(),
                (fileNameAndResult.second.overlappingTransducedIntensityAnalysis.sumBy { it.min } / fileNameAndResult.second.overlappingTransducedIntensityAnalysis.size).toString(),
                (fileNameAndResult.second.overlappingTransducedIntensityAnalysis.sumBy { it.max } / fileNameAndResult.second.overlappingTransducedIntensityAnalysis.size).toString(),
                (fileNameAndResult.second.overlappingTransducedIntensityAnalysis.sumBy { it.rawIntDen } / fileNameAndResult.second.overlappingTransducedIntensityAnalysis.size).toString()
            )
            val summaryDataRow = summarySheet.createRow(rowIdx + HEADER_OFFSET)
            summaryData.forEachIndexed { columnIdx, dataEntry ->
                val cell = summaryDataRow.createCell(columnIdx)
                cell.setCellValue(dataEntry)
            }
        }
    }

    private fun writeTransductionAnalysisSheet(
        workbook: XSSFWorkbook,
        headerCellStyle: XSSFCellStyle?
    ) {
        val perCellAnalysisSheet = workbook.createSheet("Transduced cells analysis")
        val perCellAnalysisHeader = listOf(
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
            cell.cellStyle = headerCellStyle
        }
        // Add transduced cell analysis data
        fileNameAndResultsList.forEachIndexed { baseRowIdx, fileNameAndResult ->
            fileNameAndResult.second.overlappingTransducedIntensityAnalysis.forEachIndexed { addedRowIdx, cellAnalysis ->
                val analysisRow = perCellAnalysisSheet.createRow(baseRowIdx + addedRowIdx + HEADER_OFFSET)
                val analysisData = listOf(
                    fileNameAndResult.first,
                    addedRowIdx.toString(),
                    cellAnalysis.area.toString(),
                    cellAnalysis.mean.toString(),
                    cellAnalysis.median.toString(),
                    cellAnalysis.min.toString(),
                    cellAnalysis.max.toString(),
                    cellAnalysis.rawIntDen.toString()
                )
                analysisData.forEachIndexed { colIdx, dataEntry ->
                    val cell = analysisRow.createCell(colIdx)
                    cell.setCellValue(dataEntry)
                }
            }
        }
    }

    private fun writeParamsSheet(
        workbook: XSSFWorkbook,
        headerCellStyle: XSSFCellStyle?
    ) {
        val paramsSheet = workbook.createSheet("Parameters")
        val paramsHeader = listOf(
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
        // Create header for parameters
        val paramsHeaderRow = paramsSheet.createRow(0)
        paramsHeader.forEachIndexed { columnIdx, headerTitle ->
            val cell = paramsHeaderRow.createCell(columnIdx)
            cell.setCellValue(headerTitle)
            cell.cellStyle = headerCellStyle
        }
        // Add parameters data
        fileNameAndResultsList.forEachIndexed { rowIdx, fileNameAndResult ->
            val paramsRow = paramsSheet.createRow(rowIdx + HEADER_OFFSET)
            val paramsData = listOf(
                fileNameAndResult.first,
                PLUGIN_NAME,
                PLUGIN_VERSION,
                transductionParameters.morphologyChannel,
                transductionParameters.excludeAxonsFromMorphologyChannel,
                transductionParameters.transductionChannel,
                transductionParameters.excludeAxonsFromTransductionChannel,
                transductionParameters.cellDiameterRange,
                transductionParameters.localThresholdRadius,
                transductionParameters.gaussianBlurSigma
            )
            paramsData.forEachIndexed { colIdx, dataEntry ->
                val cell = paramsRow.createCell(colIdx)
                cell.setCellValue(dataEntry)
            }
        }
    }

    private fun createHeaderCellStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.color = IndexedColors.BLUE.getIndex()

        val headerCellStyle = workbook.createCellStyle()
        headerCellStyle.setFont(headerFont)
        return headerCellStyle
    }

    private fun autoSizeColumns(workbook: XSSFWorkbook) {
        for (sheet in workbook.sheetIterator()) {
            if (sheet.physicalNumberOfRows > 0) {
                val row = sheet.getRow(sheet.firstRowNum)
                for (cell in row.cellIterator()) {
                    val columnIndex = cell.columnIndex
                    sheet.autoSizeColumn(columnIndex)
                }
            }
        }
    }
}
