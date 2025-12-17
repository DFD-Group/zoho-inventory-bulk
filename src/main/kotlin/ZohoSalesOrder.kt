import authentication.getAccessToken
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import services.SalesOrderService
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

//https://www.zohoapis.eu/inventory/v1/salesorders/126938000002628759?organization_id={{DFD Organization ID}}

private const val zoho_sales_orders_url = "https://www.zohoapis.eu/inventory/v1/salesorders"
private const val zoho_sales_order_id = "126938000002628759"
private const val organization_id = "organization_id"
private const val dfd_organization_id = "20071872645"
private const val product_info_file = "src/main/resources/static/product_info"
private const val sales_order_file = "src/main/resources/static/sales_order_update.json"
private const val bearer = "Zoho-oauthtoken"
private const val headerAuthKey = "Authorization"
private const val chavdarSalesPersonID = "126938000002470050"

fun main() {
    val accessToken = getAccessToken()
    val salesOrders = mutableListOf<String>()
    var page = 1
    while (page < 8) {
        Thread.sleep(60_000)
        val salesOrderService = SalesOrderService(accessToken, Config.DFD_ORGANIZATION_ID)
        val allSalesOrders = salesOrderService.getSalesOrders(accessToken, page, 200)
        val allCompleteSalesOrders = mutableListOf<CompleteSalesOrder>()
        for (salesOrder in allSalesOrders) {
            val completeSalesOrder = salesOrderService.getSalesOrder(accessToken, salesOrder.salesorder_id)
            allCompleteSalesOrders.add(completeSalesOrder)
        }
        salesOrders.addAll(salesOrderService.filterSalesOrdersByTemplate(allCompleteSalesOrders, "126938000000293264"))
        page++
    }

    for (so in salesOrders) {
        updateSalesOrderTemplate(accessToken,  so.toLong(), "126938000000181100")
    }
}

fun updateSalesOrderItems() {
    val itemIdAndQuantity = getItemIdAndQuantityFromFile(product_info_file)
    val jsonBody = getJsonBody(itemIdAndQuantity)
    writeJsonBodyToFile(sales_order_file, jsonBody)
    val accessToken = getAccessToken()
    val fullUrl = URL("$zoho_sales_orders_url/$zoho_sales_order_id?$organization_id=$dfd_organization_id")
    val connection = fullUrl.openConnection() as HttpURLConnection
    connection.requestMethod = "PUT"
    connection.setRequestProperty(headerAuthKey, "$bearer $accessToken")
    connection.doOutput = true
    connection.outputStream.write(jsonBody.toByteArray())
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    println(response)
}


fun updateSalesOrderTemplate(accessToken: String, salesOrderID: Long, templateID: String) {
    val fullUrl =
        URL("$zoho_sales_orders_url/$salesOrderID/templates/$templateID?$organization_id=$dfd_organization_id")
    print(fullUrl)
    val connection = fullUrl.openConnection() as HttpURLConnection
    connection.requestMethod = "PUT"
    connection.setRequestProperty(headerAuthKey, "$bearer $accessToken")
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    println(response)
}


fun writeJsonBodyToFile(salesOrderFile: String, jsonBody: String) {
    val file = File(salesOrderFile)
    file.printWriter().use { out ->
        out.println(jsonBody)
    }
}

private fun getJsonBody(skusAndQuantity: List<Pair<String, String>>): String {
    val lineItems = skusAndQuantity.map { (itemId, quantity) -> LineItem(itemId, quantity) }
    val mapper = jacksonObjectMapper()
    return mapper.writeValueAsString(mapOf("line_items" to lineItems))
}

fun getItemIdAndQuantityFromFile(filePath: String): List<Pair<String, String>> {
    val file = File(filePath)
    return file.readLines().map {
        val parts = it.split(" ")
        parts[2] to parts[1]
    }
}

data class LineItem(val item_id: String, val quantity: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SalesOrder(val salesorder_id: String) // Simplified model

@JsonIgnoreProperties(ignoreUnknown = true)
data class SalesOrderListResponse(val salesorders: List<SalesOrder>, val page_context: PageContext)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SalesOrderResponse(val salesorder: CompleteSalesOrder)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CompleteSalesOrder(
    val salesorder_id: String,
    val salesorder_number: String,
    val customer_id: String,
    val template_id: String,
    var salesperson_id: String,
    val line_items: List<CompleteLineItem>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CompleteLineItem(
    val line_item_id: Long,
    val item_id: Long,
//    val name: String,
//    val description: String,
    val rate: Double,
    val quantity: Int,
    val unit: String,
    val tax_id: Long,
    val tax_name: String,
    val tax_type: String,
    val tax_percentage: Double,
    val item_total: Double,
    val warehouse_id: Long,
)

