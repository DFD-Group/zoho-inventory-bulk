package services

import CompleteSalesOrder
import SalesOrder
import SalesOrderListResponse
import SalesOrderResponse
import authentication.getAccessToken
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import updateSalesOrderTemplate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object Config {
    const val ZOHO_API_ROOT = "https://www.zohoapis.eu/inventory/v1/"
    const val ZOHO_SALES_ORDERS_URL = "${ZOHO_API_ROOT}salesorders"
    const val ORGANIZATION_ID = "organization_id"
    const val DFD_ORGANIZATION_ID = "20071872645"
    const val BEARER_TOKEN = "Zoho-oauthtoken"
    const val HEADER_AUTH_KEY = "Authorization"
}

class SalesOrderService(private val accessToken: String, private val companyOrganizationId: String) {
    private val mapper = jacksonObjectMapper()

    fun main() {
        val accessToken = getAccessToken()
        val salesOrders = mutableListOf<String>()
        var page = 1
        while (page < 8) {
            Thread.sleep(60_000)
            val allSalesOrders = getSalesOrders(accessToken, page, 200)
            val allCompleteSalesOrders = mutableListOf<CompleteSalesOrder>()
            for (salesOrder in allSalesOrders) {
                val completeSalesOrder = getSalesOrder(accessToken, salesOrder.salesorder_id)
                allCompleteSalesOrders.add(completeSalesOrder)
            }
            salesOrders.addAll(filterSalesOrdersByTemplate(allCompleteSalesOrders, "126938000000293264"))
            page++
        }

        for (so in salesOrders) {
            updateSalesOrderTemplate(accessToken,  so.toLong(), "126938000000181100")
        }
    }

    fun filterSalesOrdersByTemplate(
        completeSalesOrders: List<CompleteSalesOrder>,
        templateId: String
    ): List<String> {
        val filteredSalesOrders = completeSalesOrders.filter { it.template_id == templateId }
        val filteredSalesOrdersIDS = mutableListOf<String>()
        for (filteredSalesOrder in filteredSalesOrders) {
            filteredSalesOrdersIDS.add(filteredSalesOrder.salesorder_id)
        }
        return filteredSalesOrdersIDS
    }

    /**
     * Get all sales orders from Zoho Inventory.
     * curl --request GET \
     *  --url 'https://www.zohoapis.com/inventory/v1/salesorders?organization_id=10234695&page=1' \
     */
    fun getSalesOrders(accessToken: String, page: Int, per_page: Int): List<SalesOrder> {
        val salesOrders = mutableListOf<SalesOrder>()

        val url =
            URL("${Config.ZOHO_SALES_ORDERS_URL}?${Config.ORGANIZATION_ID}=${companyOrganizationId}&page=$page&per_page=$per_page")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "w"
            setRequestProperty(Config.HEADER_AUTH_KEY, "${Config.BEARER_TOKEN} $accessToken")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val salesOrderResponse = mapper.readValue(response, SalesOrderListResponse::class.java)
        salesOrders.addAll(salesOrderResponse.salesorders)

        return salesOrders
    }

    /**
     * Get sales order from Zoho Inventory.
     * curl --request GET \
     *  --url 'https://www.zohoapis.com/inventory/v1/salesorder/126938000000049076?organization_id=10234695&page=1' \
     */
    fun getSalesOrder(accessToken: String, salesOrderId: String): CompleteSalesOrder {
        val url =
            URL("${Config.ZOHO_SALES_ORDERS_URL}${"/" + salesOrderId}?${Config.ORGANIZATION_ID}=${companyOrganizationId}")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty(Config.HEADER_AUTH_KEY, "${Config.BEARER_TOKEN} $accessToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
        }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val salesOrderResponse = mapper.readValue(response, SalesOrderResponse::class.java)
        return salesOrderResponse.salesorder
    }

    /**
     * Updates a new Sales Order in Zoho Inventory.
     * curl --request PUT \
     *   --url 'https://www.zohoapis.com/inventory/v1/salesorders/4815000000044895?organization_id=10234695'
     */
    fun updateSalesOrderSalesPerson(salesOrderID: String, newSalesPeronsID: String) {
        val url =
            URL("${Config.ZOHO_SALES_ORDERS_URL}${"/" + salesOrderID}?${Config.ORGANIZATION_ID}=${companyOrganizationId}")
        val connection = url.openConnection() as HttpURLConnection
        val jsonBody = mapper.writeValueAsString(
            mapOf("salesorder" to mapOf("salesperson_id" to newSalesPeronsID))
        )
//        println(jsonBody);
        connection.apply {
            requestMethod = "PUT"
            setRequestProperty(Config.HEADER_AUTH_KEY, "${Config.BEARER_TOKEN} $accessToken")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true;
        }

        OutputStreamWriter(connection.outputStream).use { it.write(jsonBody) }
        val responseCode = connection.responseCode
        println("Response Code: $responseCode")
        val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        if (inputStream != null) {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val response = reader.readText()
                println("Response Body: $response")
            }
        } else {
            println("No response body received from server.")
        }
    }
}