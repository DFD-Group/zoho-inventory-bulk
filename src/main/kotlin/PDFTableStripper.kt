import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripperByArea
import java.awt.Rectangle
import java.awt.geom.Rectangle2D
import java.io.File
import java.io.IOException

class PDFTableStripper : PDFTextStripperByArea() {

    val res = 72.0 // PDF units are at 72 DPI
    val xCoef = 0.4
    val yCoef = 1
    val widthCoef = 7.5
    val heightCoef = 9.5

    /**
     * Sets the region dynamically to extract tables. Adjust based on the specific document.
     */
    private fun setRegion(rect: Rectangle2D) {
        addRegion("tableRegion", rect)
    }

    /**
     * Process a page and extract tables using the defined region.
     */
    private fun extractTable(page: PDPage) {
        // Process text in the defined region
        extractRegions(page)
        val extractedText = getTextForRegion("tableRegion")// here is the problem
        postProcessText(extractedText)
    }

    /**
     * Clean up and format the extracted text into rows and columns.
     */
    private fun postProcessText(text: String) {
        val rows = text.lines()
        val cleanedRows = mergeBrokenLines(rows)
        cleanedRows.forEachIndexed { rowIndex, row ->
            println("Row $rowIndex: $row")
        }
    }

    /**
     * Merge fragmented lines into complete rows based on punctuation or structure.
     */
    private fun mergeBrokenLines(rows: List<String>): List<String> {
        val mergedRows = mutableListOf<String>()
        var tempRow = ""

        for (row in rows) {
            if (row.endsWith(",") || row.endsWith("-")) {
                // Temporarily hold fragmented text
                tempRow += " ${row.trim()}"
            } else {
                // Finalize the row
                tempRow += " ${row.trim()}"
                mergedRows.add(tempRow.trim())
                tempRow = ""
            }
        }

        if (tempRow.isNotEmpty()) {
            mergedRows.add(tempRow.trim()) // Add any leftover row
        }

        return mergedRows
    }

    /**
     * Main function to process the document.
     */
    @Throws(IOException::class)
    fun processDocument(filePath: String) {
        PDDocument.load(File(filePath)).use { document ->
            val res = 72.0 // PDF units at 72 DPI
            val x = Math.round(xCoef * res).toInt()
            val y = Math.round(yCoef * res).toInt()
            val width = Math.round(widthCoef * res).toInt()
            val height = Math.round(heightCoef * res).toInt()
            val Rectangle = Rectangle(x, y, width, height)
            val stripper = PDFTableStripper()
            stripper.sortByPosition = true

            // Define a region dynamically (adjust coordinates as needed)
            stripper.setRegion(Rectangle)

            // Process each page
            for (pageIndex in 0 until document.numberOfPages) {
                println("Processing Page $pageIndex")
                val page = document.getPage(pageIndex)
                stripper.extractTable(page)
            }
        }
    }
}

fun main(args: Array<String>) {
    try {
        val filePath = args.getOrNull(0)
            ?: "/Users/Chavdar/Library/CloudStorage/Dropbox/1.DFD Group/Поръчки/0946 Kromm exp-20112024/Kromm поръчки/Price Confirmed Bon de commande - P41220.pdf" // Replace with the actual file path
        PDFTableStripper().processDocument(filePath)
    } catch (e: IOException) {
        println("Error processing PDF: ${e.message}")
    }
}