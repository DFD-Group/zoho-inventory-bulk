import authentication.getAccessToken
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// https://www.zohoapis.eu/inventory/v1/items?organization_id={{DFD Organization ID}}&search_text=119.033060

private const val zoho_items_url = "https://www.zohoapis.eu/inventory/v1/items"
private const val organization_id = "organization_id"
private const val search_text = "search_text"
private val dfd_organization_id = "20071872645"
private const val product_info_file = "src/main/resources/static/packaging"
private val accessToken = getAccessToken()


fun main() {
    val file = File("src/main/resources/static/packaging");
    val skus = file.readLines();
    val itemIDS = ArrayList<String>();
    skus.forEach {
        println(getItemId(it));
    }
}

fun updateItemPrice(itemId: String, price: String) {
    // Prepare the Request
    val accessToken = getAccessToken()
    val fullUrl = URL(zoho_items_url + "/" + itemId + "?" + organization_id + "=" + dfd_organization_id)
    // Send the Request
    val connection = fullUrl.openConnection() as HttpURLConnection
    connection.requestMethod = "PUT"
    connection.setRequestProperty("Authorization", "Zoho-oauthtoken $accessToken")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.doOutput = true
    val lev_price = price.toDouble() * 1.95583
    val body = "{\"rate\":$price, \"purchase_rate\":\"$lev_price\"}"
    connection.outputStream.bufferedWriter().use { it.write(body) }
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    println(response)
}

fun writeProductDetailsToFile() {
    val file = File(product_info_file)
    file.printWriter().use { out ->
        getSkuAndQuantityFromFile(product_info_file, 0, 1)
            .associateWith { skuQuantity -> getItemId(skuQuantity.first) }
            .forEach { sku, itemId ->
                out.println("${sku.first} ${sku.second} $itemId")
            }
    }
}

fun getItemIdAndPriceFromFile(filePath: String, start: Int, end: Int): List<Pair<String, String>> {
    return getDetailsFromFile(filePath, start, end)
}

fun getSkuAndQuantityFromFile(filePath: String, start: Int, end: Int): List<Pair<String, String>> {
    return getDetailsFromFile(filePath, start, end)
}

private fun getDetailsFromFile(filePath: String, start: Int, end: Int): List<Pair<String, String>> {
    val file = File(filePath)
    return file.readLines().map {
        val parts = it.split(" ")
        parts[start] to parts[end]
    }
}

fun getItemId(sku: String): String {
    // Prepare the Request
    val params = organization_id + "=" + dfd_organization_id + "&" + search_text + "=" + sku
    val fullUrl = URL(zoho_items_url + "?" + params)
    // Send the Request
    val connection = fullUrl.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("Authorization", "Zoho-oauthtoken $accessToken")
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    // Parse the Response
    val mapper = jacksonObjectMapper()
    val apiResponse = mapper.readValue(response, ApiResponse::class.java)
    return apiResponse.items.firstOrNull()?.item_id ?: throw IllegalStateException("Item ID not found")
}

fun getItemDescription(itemId: String): ItemDescriptionResponse {
    // Prepare the Request
    val fullUrl = URL(zoho_items_url + "/" + itemId + "?" + organization_id + "=" + dfd_organization_id)
    // Send the Request
    val connection = fullUrl.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("Authorization", "Zoho-oauthtoken $accessToken")
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    // Parse the Response
    val mapper = jacksonObjectMapper()
    val apiResponse = mapper.readValue(response, ApiItemResponse::class.java)
    return apiResponse.item
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ItemIdResponse(val item_id: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiResponse(val code: Int, val message: String, val items: List<ItemIdResponse>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ItemDescriptionResponse(val item_id: String, val name: String, val brand: String, val description: String, val purchase_description: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiItemResponse(val code: Int, val message: String, val item: ItemDescriptionResponse)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiItemsResponse(val code: Int, val message: String, val items: List<ItemDescriptionResponse>)



