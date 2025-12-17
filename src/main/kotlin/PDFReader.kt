import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.text.replace

fun main() {
//    val filePath = getFilePath()
//    filePath?.let {
//        val sections = readPdf(it)
//    val products =
//        readPdf("/Users/chavdar/DFD Group Dropbox/DFD Group Team Folder/Поръчки/1120 GF exp-12122025/GF_Bestellung_53119.pdf")
//        readPdf("/Users/Chavdar/Library/CloudStorage/Dropbox/1.DFD Group/Поръчки/0946 Kromm exp-20112024/Kromm поръчки/Price Confirmed Bon de commande - P41220.pdf")

//    products.forEach { (articleNumber, quantity) ->
//        println("$articleNumber: ${quantity}")
//        println("********************************************************")
//    }
//    } ?: println("No file selected")
}

fun readPdf(filePath: String): List<Pair<String, String>> {
    val products = mutableListOf<Pair<String, String>>()

    // Split text into lines and search for relevant lines
    PDDocument.load(File(filePath)).use { document ->
        val pdfTextStripper = PDFTextStripper()
        val text = pdfTextStripper.getText(document)
        var currentArticleNumber = ""
        var currentQuantity = ""
        // Split text into lines and search for relevant lines
        text.lines().forEach { line ->
            if (currentArticleNumber.isNotEmpty() && currentQuantity.isNotEmpty()) {
                products.add(currentArticleNumber to currentQuantity)
                currentArticleNumber = ""
                currentQuantity = ""
            }
            if ((line.length == 11 || line.length == 12) && line.contains(".")) {
                currentArticleNumber += line
            }

            if (line.contains("St.")){
                currentQuantity += line.replace(" St.", "")
            }
        }
    }
    return products
}

private fun getFilePath(): String? {
    val projectRoot = File(System.getProperty("user.dir")).parent

    val fileChooser = JFileChooser(projectRoot)
    fileChooser.dialogTitle = "Select a PDF file"
    fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")

    val result = fileChooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFile.absolutePath
    } else {
        null
    }
}