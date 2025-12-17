package services

import authentication.getAccessToken
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import services.ItemServiceConfig.ZOHO_API_ITEMS_ROOT
import services.ItemServiceConfig.ORGANIZATION_ID
import services.ItemServiceConfig.DFD_ORGANIZATION_ID
import services.ItemServiceConfig.BEARER_TOKEN
import services.ItemServiceConfig.HEADER_AUTH_KEY
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ItemServiceConfig {
    const val ZOHO_API_ITEMS_ROOT = "https://www.zohoapis.eu/inventory/v1/items"
    const val ORGANIZATION_ID = "organization_id"
    const val DFD_ORGANIZATION_ID = "20071872645"
    const val BEARER_TOKEN = "Zoho-oauthtoken"
    const val HEADER_AUTH_KEY = "Authorization"
}

fun main() {
    val items = mutableListOf<Item>()
    val fileItems = File("src/main/resources/static/statistics/items.txt").readLines()
    fileItems.forEach { fileItem ->
        val item = fileItem.split(";")
        val itemID = item.get(0)
        val date = item.get(3)
        val quantity = item.get(4)
        items.add(Item(itemID, "N/A", "N/A", date, quantity.toInt()))
    }
    val accessToken = getAccessToken()
    val itemService = ItemService(accessToken)
    items.forEach { item ->
        try {
            val itemPlateBrand = itemService.getItem(item.itemID)
            item.plate = itemService.getPlateName(itemPlateBrand.name)
            item.brand = itemPlateBrand.brand
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    // Write to file
    File("src/main/resources/static/statistics/fullItems.txt").printWriter(Charsets.UTF_8).use { out ->
        out.write(
            items.joinToString("\n") { it.itemID + ";" + it.plate + ";" + it.brand + ";" + it.date + ";" + it.item_quantity } + "\n"
        )
    }
}

class ItemService(private val accessToken: String) {
    val mapper = jacksonObjectMapper()

    fun getPlateName(name: String): String {
        val plateSizes = listOf("245x95", "166x333", "333x166", "300x300", "95x245", "166x300", "333x333")
        val plateSize = plateSizes.find { size -> name.contains(size) }
        if (plateSize != null) {
            val plateStartIndex = name.indexOf(plateSize) + plateSize.length + 1
            if (plateStartIndex < name.length) {
                val plate = name.substring(plateStartIndex).split("|").first().trim()
                if (plate.isNotEmpty()) return plate
            }
        }
        println("Plate size not found for item: $name")
        return "N/A"
    }

    fun getItem(itemID: String): ItemPlateBrand {
        val url = URL("${ZOHO_API_ITEMS_ROOT}/${itemID}?${ORGANIZATION_ID}=${DFD_ORGANIZATION_ID}")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty(HEADER_AUTH_KEY, "$BEARER_TOKEN $accessToken")
        }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        // Parse the Response
        val itemPlateBrand = mapper.readValue(response, ApiItemResponse::class.java)
        return itemPlateBrand.item
    }

    fun searchForItem(plateName: String): ItemDescriptionResponse {
        val url = URL("${ZOHO_API_ITEMS_ROOT}?search_text=${plateName}&${ORGANIZATION_ID}=${DFD_ORGANIZATION_ID}")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty(HEADER_AUTH_KEY, "$BEARER_TOKEN $accessToken")
        }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        // Parse the Response
        val apiResponse = mapper.readValue(response, ApiItemsResponse::class.java)
        return apiResponse.items.firstOrNull()
            ?: throw Exception("Item with plate name $plateName not found.")
    }

    fun getLastInvoiceOfItem(itemID: String): InvoiceDescription {
        val url =
            URL("${ZOHO_API_ITEMS_ROOT}/transactions/invoices?item_id=${itemID}&${ORGANIZATION_ID}=${DFD_ORGANIZATION_ID}")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty(HEADER_AUTH_KEY, "$BEARER_TOKEN $accessToken")
        }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val apiResponse = mapper.readValue(response, ApiInvoicesItemsResponse::class.java)
        return apiResponse.invoices.firstOrNull()
            ?: throw Exception("No invoices found for item ID $itemID.")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ItemDescriptionResponse(
    val item_id: String,
    val name: String,
    val brand: String,
    val description: String,
    val purchase_description: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiItemsResponse(val code: Int, val message: String, val items: List<ItemDescriptionResponse>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ItemPlateBrand(
    val name: String,
    val brand: String
)

data class Item(
    val itemID: String,
    var plate: String,
    var brand: String,
    val date: String,
    val item_quantity: Int
) {
    override fun toString(): String {
        return "Item(itemID='$itemID', plate='$plate', brand='$brand', date='$date', item_quantity=$item_quantity)"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceDescription(
    val date: String,
    val item_quantity: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiInvoicesItemsResponse(val code: Int, val message: String, val invoices: List<InvoiceDescription>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiItemResponse(val code: Int, val message: String, val item: ItemPlateBrand)
