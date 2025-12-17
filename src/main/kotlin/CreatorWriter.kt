import authentication.getAccessToken
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import services.CreatorRecordService
import java.io.File
import java.io.OutputStreamWriter

var firstPhase = false
var secondPhase = true

class Plate(
    val plateSign: String,
    val plateSize: String,
    var pdfFile: File?,
    var autocadFile: File?,
    var recordId: String?
)

fun main() {
    val file = File("src/main/resources/static/statistics/items.txt")
    val items = file.readLines().map { it.trim() }
    for (item in items) {
        println("Processing item: $item")
    }

}
fun getIDsAndPlateSizes(modelsJSON: File): List<Pair<String, String>> {
    val mapper = ObjectMapper()
    val result = mutableListOf<Pair<String, String>>()

    val rootNode: JsonNode = mapper.readTree(modelsJSON)
    val dataArray = rootNode.get("data")

    for (item in dataArray) {
        if (!item.get("Dropdown1").asText().isEmpty()){
            continue;
        }
        val id = item.get("ID")?.asText() ?: continue
        val plateSize = item.get("plate_size_field")?.asText() ?: continue
        result.add(Pair(id, plateSize))
    }
    return result
}

private fun uploadPlatesToCreator(
    plates: MutableList<Plate>,
    creatorRecordService: CreatorRecordService,
    accessToken: String,
    delayBetweenCalls: Long
) {
    plates.forEachIndexed { index, plate ->
        val responseMap = creatorRecordService.addModelRecord(accessToken, plate.plateSign, plate.plateSize)
        plate.recordId = responseMap["createdRecordID"] as String?
        creatorRecordService.uploadModelFile(accessToken, "upload_pdf", plate.recordId!!, plate.pdfFile!!)
        creatorRecordService.uploadModelFile(accessToken, "autocad_file", plate.recordId!!, plate.autocadFile!!)

        // Add delay after every API call except the last one
        if (index < plates.size - 1) {
            Thread.sleep(delayBetweenCalls)
        }
    }
}

fun getAllPdfFilesRecursively(directory: File): List<File> {
    return directory.walk().filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }.toList()
}

fun getFilesWithSameNameRecursively(directory: File): MutableList<Plate> {
    val plateSizes = listOf("100", "166", "245", "300", "333", "95", "250", "100x245")
    val plates = mutableListOf<Plate>()

    directory.walk().forEach { file ->
        if (!file.isFile) return@forEach

        if (file.name.contains("graphic", ignoreCase = true) ||
            file.extension.equals("jpg", ignoreCase = true) ||
            file.extension.equals("png", ignoreCase = true)
        ) return@forEach

        val nameWithoutExt = file.nameWithoutExtension
        val lastDashIndex = nameWithoutExt.lastIndexOf("-")
        if (lastDashIndex == -1) return@forEach

        val plateSign = nameWithoutExt.substring(0, lastDashIndex).trim()
        var plateSizeRaw = nameWithoutExt.substring(lastDashIndex + 1).trim()

        val parts = plateSizeRaw.lowercase().split("x")
        if (parts.any { it !in plateSizes }) return@forEach

        val plateSize = when {
            plateSizeRaw.contains("166") -> "166x333"
            plateSizeRaw.contains("245") -> "245x95"
            else -> plateSizeRaw
        }

        val existingPlate = plates.find { it.plateSign == plateSign && it.plateSize == plateSize }

        if (existingPlate != null) {
            if (existingPlate.pdfFile == null && file.extension.equals("pdf", ignoreCase = true)) {
                existingPlate.pdfFile = file
            } else if (existingPlate.autocadFile == null &&
                (file.extension.equals("dwg", ignoreCase = true) || file.extension.equals("dxf", ignoreCase = true))
            ) {
                existingPlate.autocadFile = file
            }
        } else {
            val newPlate = Plate(plateSign, plateSize, null, null, null)
            if (file.extension.equals("pdf", ignoreCase = true)) {
                newPlate.pdfFile = file
            } else if (file.extension.equals("dwg", ignoreCase = true) || file.extension.equals("dxf", ignoreCase = true)) {
                newPlate.autocadFile = file
            }
            plates.add(newPlate)
        }
    }

    // Filter out plates missing either pdf or autocad file
    return plates.filter { it.pdfFile != null && it.autocadFile != null }.toMutableList()
}
