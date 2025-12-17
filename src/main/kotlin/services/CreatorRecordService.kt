package services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import services.CreatorConfig.BEARER_TOKEN
import services.CreatorConfig.CHLOE_API
import services.CreatorConfig.FORM
import services.CreatorConfig.HEADER_AUTH_KEY
import services.CreatorConfig.MODEL_REPORT
import services.CreatorConfig.PRIVATELINK
import services.CreatorConfig.REPORT
import services.CreatorConfig.SEND_MODEL_FORM
import services.CreatorConfig.SEND_MODEL_FORM_PRIVATELINK
import services.CreatorConfig.ZOHO_CREATOR_API_ROOT
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


object CreatorConfig {
    const val ZOHO_CREATOR_API_ROOT = "https://www.zohoapis.eu/creator/v2.1/data/"
    const val CHLOE_API = "dfdgroup/chloe-ai/"
    const val FORM = "form/"
    const val REPORT = "report/"
    const val SEND_MODEL_FORM = "new_model_form"
    const val MODEL_REPORT = "models_report"
    const val PRIVATELINK = "privatelink"
    const val SEND_MODEL_FORM_PRIVATELINK = "UyEVByJ4hR7NtYQHVkuH7HB2X3kTO9t4rGwHmbqY79Em18ZQzur6OZBrqrRwyAj4HCQhAfTrDXhm31nwdGbNPtYyRjm5vS9FCmUp"
    const val HEADER_AUTH_KEY = "Authorization"
    const val BEARER_TOKEN = "Zoho-oauthtoken"
}

class CreatorRecordService() {
    private val mapper = ObjectMapper()

    /**
     * https://creatorapp.zoho.eu/dfdgroup/chloe-ai/#Report:models_report
     * curl "https://www.zohoapis.com/creator/v2.1/data/dfdgroup/chloe-ai/report/models_report?max_records=1000"
     * -H "Authorization: Zoho-oauthtoken 1000.XXXXXXXXXXXXXXXXXXXXXX.XXXXXXXXXXXXXXXXXXXXXXXX"
     */
    fun getModelRecordIDs(accessToken: String): Map<String, List<String>> {
        val baseUrl = "${ZOHO_CREATOR_API_ROOT}${CHLOE_API}${REPORT}${MODEL_REPORT}"
        val urlWithParams = "$baseUrl?max_records=1000"

        val connection = URL(urlWithParams).openConnection() as HttpURLConnection
        var rawResponse = "No response yet"
        val records = mutableMapOf<String, List<String>>()
        val recordIDs = mutableListOf<String>()
        records.put("IDs", recordIDs)
        val plateSizes = mutableListOf<String>()
        records.put("Sizes", plateSizes)

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty(HEADER_AUTH_KEY, "$BEARER_TOKEN $accessToken")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val mapper = ObjectMapper()
                val jsonResponse = mapper.readTree(response)
                rawResponse = "Response: ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse)}"
                val dataArray = jsonResponse.get("data")
                for (record in dataArray) {
                    recordIDs.add(record.get("ID").asText())
                    plateSizes.add(record.get("plate_size_field").asText())
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw RuntimeException("HTTP error code: $responseCode, Error: $errorResponse")
            }
        } catch (e: Exception) {
            println("Exception while making the API request: ${e.message}")
        } finally {
            connection.disconnect()
        }
        return records
    }

    fun updateRecord(accessToken: String, recordID: String, plateSize: String?) {
        //<base_url>/creator/v2.1/data/<account_owner_name>/<app_link_name>/report/<report_link_name>/<record_ID>
        val url = URL("${ZOHO_CREATOR_API_ROOT}${CHLOE_API}${REPORT}${MODEL_REPORT}/$recordID")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(HEADER_AUTH_KEY, "$BEARER_TOKEN $accessToken")
            }
            val payload: ObjectNode = mapper.createObjectNode()

            val dataArr: ArrayNode = mapper.createArrayNode()

            val modelObject: ObjectNode = mapper.createObjectNode()
            modelObject.put("plate_size", plateSize)
            dataArr.add(modelObject)

            payload.set<ArrayNode>("data", dataArr)

            val skipWorkflowArr: ArrayNode = mapper.createArrayNode()
            skipWorkflowArr.add("all")

            payload.set<ArrayNode>("skip_workflow", skipWorkflowArr)


            connection.outputStream.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(payload.toString())
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val mapper = ObjectMapper()
                val jsonResponse = mapper.readTree(response)
                println("Response: ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse)}")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw RuntimeException("HTTP error code: $responseCode, Error: $errorResponse")
            }
        } catch (e: Exception) {
            println("Exception while making the API request: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * https://creatorapp.zoho.eu/dfdgroup/chloe-ai/#Report:orders
     * https://creatorapp.zohopublic.eu/dfdgroup/chloe-ai/form-perma/send_model_form/CDENsdJTZ9fyV57ZNufd9SUSBd1s7bjWmCR2zUEay4sa88W4HkgAOnwQQmFC2bvUdjZY5vzGXnCmZ44n5OvMutqaRNRgKBzmz3WR
     * curl "https://www.zohoapis.com/creator/v2.1/publish/dfdgroup/chloe-ai/form/send_model_form?privatelink=CDENsdJTZ9fyV57ZNufd9SUSBd1s7bjWmCR2zUEay4sa88W4HkgAOnwQQmFC2bvUdjZY5vzGXnCmZ44n5OvMutqaRNRgKBzmz3WR"
     * -X POST
     * -d "@newrecords.json"
     */
    fun addModelRecord(accessToken: String, plateSign: String, plateSize: String): Map<String, Any> {
        val mapper = ObjectMapper()

        val payload: ObjectNode = mapper.createObjectNode()

        val dataArr: ArrayNode = mapper.createArrayNode()

        val modelObject: ObjectNode = mapper.createObjectNode()
        modelObject.put("plate_sign", plateSign)
        modelObject.put("plate_size", plateSize)
        dataArr.add(modelObject)

        payload.set<ArrayNode>("data", dataArr)

        val jsonString = payload.toString()

        val url = URL("${ZOHO_CREATOR_API_ROOT}${CHLOE_API}${FORM}${SEND_MODEL_FORM}?${PRIVATELINK}=${SEND_MODEL_FORM_PRIVATELINK}")
        val connection = url.openConnection() as HttpURLConnection
        var createdRecordID = "0"
        var rawResponse = "No response yet"
        val okRequestResponseMap = mutableMapOf<String, String>()
        val errorResponseMap = mutableMapOf<String, String>()

        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(HEADER_AUTH_KEY, "$BEARER_TOKEN $accessToken")
            }

            connection.outputStream.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = mapper.readTree(response)
                rawResponse = "Response: ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse)}"

                if (jsonResponse.has("result")) {
                    val resultArray = jsonResponse.get("result")
                    if (resultArray.isArray) {
                        for (result in resultArray) {
                            okRequestResponseMap.put("code", result.get("code").asText())
                            okRequestResponseMap.put("message", result.get("message").asText())

                            if (result.has("data")) {
                                val data = result.get("data")
                                createdRecordID = data.get("ID").asText()
                                okRequestResponseMap.put("recordId", createdRecordID)
                            } else {
                                errorResponseMap.put("error", "No data found in the result.")
                            }
                        }
                    }
                } else {
                    errorResponseMap.put("error", "No result found in the response.")
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw RuntimeException("HTTP error code: $responseCode, Error: $errorResponse")
            }
        } catch (e: Exception) {
            errorResponseMap.put("error", "Exception occurred: ${e.message}")
        } finally {
            connection.disconnect()
        }
        val responseMap = mutableMapOf<String, Any>()
        responseMap.put("rawResponse", rawResponse)
        responseMap.put("okResponse", okRequestResponseMap)
        responseMap.put("errorResponse", errorResponseMap)
        responseMap.put("createdRecordID", createdRecordID)
        return responseMap
    }

    fun uploadModelFile(accessToken: String, fieldLink: String,  recordId: String, file: File){
        val boundary = "Boundary-${System.currentTimeMillis()}"
        //https://www.zohoapis.eu/creator/v2.1/data/dfdgroup/chloe-ai/report/models/210172000000109003/autocad_file/upload?skip_workflow=["all"]
        val url = URL("https://www.zohoapis.eu/creator/v2.1/data/dfdgroup/chloe-ai/report/models/" + recordId + "/" + fieldLink + "/upload")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                connectTimeout = 30000
                readTimeout = 30000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Authorization", "Zoho-oauthtoken $accessToken")
            }

            connection.outputStream.use { outputStream ->
                writeMultipartData(outputStream, file, boundary)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val mapper = ObjectMapper()
                val jsonResponse = mapper.readTree(response)
                println("Response: ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse)}")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw RuntimeException("HTTP error code: $responseCode, Error: $errorResponse")
            }
        } catch (e: Exception) {
            println("Exception while making the API request: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    private fun writeMultipartData(outputStream: OutputStream, file: File, boundary: String) {
        val lineSeparator = "\r\n"
        val fileField = "file"
        val fileMimeType = "application/octet-stream"

        outputStream.write("--$boundary$lineSeparator".toByteArray())
        outputStream.write("Content-Disposition: form-data; name=\"$fileField\"; filename=\"${file.name}\"$lineSeparator".toByteArray())
        outputStream.write("Content-Type: $fileMimeType$lineSeparator$lineSeparator".toByteArray())
        file.inputStream().use { it.copyTo(outputStream) }
        outputStream.write(lineSeparator.toByteArray())
        outputStream.write("--$boundary--$lineSeparator".toByteArray())
    }
}