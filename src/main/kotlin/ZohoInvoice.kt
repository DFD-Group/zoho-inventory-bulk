import authentication.getAccessToken
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import services.InvoiceService
import java.io.File
import java.io.Serializable

object Config {
    const val ZOHO_API_ROOT = "https://www.zohoapis.eu/inventory/v1/";
    const val ZOHO_INVOICES_URL = ZOHO_API_ROOT + "invoices"
    const val ZOHO_SALES_ORDERS_URL = ZOHO_API_ROOT + "salesorders"
    const val ORGANIZATION_ID = "organization_id"
    const val DFD_ORGANIZATION_ID = "20071872645"
    const val PRODUCT_INFO_FILE = "src/main/resources/static/product_info"
    const val INVOICE_FILE = "src/main/resources/static/invoices_with_old_template.md"
    const val BEARER_TOKEN = "Zoho-oauthtoken"
    const val HEADER_AUTH_KEY = "Authorization"
}

fun main() {
    val accessToken = getAccessToken()
    val invoiceService = InvoiceService(accessToken, Config.DFD_ORGANIZATION_ID)
    val templateId = "126938000000359139"
    val invoices = invoiceService.findInvoicesUsingTemplate(accessToken, templateId);
    println(invoices);

//    val invoices = File(Config.INVOICE_FILE).readLines()
//    invoices.forEach { invoiceId ->
//        println("Updating invoice ${invoiceId} with template $templateId")
//        invoiceService.updateInvoiceTemplate(accessToken, invoiceId, templateId)
//    }
}

object FileUtil {

    fun readProductInfo(filePath: String): List<ProductInfo> {
        return File(filePath).readLines().map {
            val parts = it.split(" ")
            ProductInfo(parts[0], parts[1], parts[2])
        }
    }

    fun writeInvoiceReport(filePath: String, invoiceData: Collection<ProductInvoiceData>) {
        val file = File(filePath)
        file.printWriter().use { out ->
            val headers = listOf("sku", "quantity", "invoice number", "invoice date", "item price", "item quantity")
            out.println(headers.joinToString(" | "))
            val separator = headers.joinToString(" | ") { "-".repeat(it.length) }
            out.println(separator)
            invoiceData.forEach { item ->
                out.println("${item.sku} | ${item.quantity} | ${item.invoiceNumber} | ${item.date} | ${item.itemPrice} | ${item.itemQuantity}")
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Invoice(val invoice_id: String, val template_id: String) // Simplified model

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceListResponse(val invoices: List<Invoice>, val page_context: PageContext)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PageContext(val has_more_page: Boolean, val page: Int)

data class ProductInfo(val sku: String, val quantity: String, val itemId: String)

data class ProductInvoiceData(
    val sku: String,
    val quantity: String,
    val invoiceNumber: String,
    val date: String,
    val itemPrice: Double,
    val itemQuantity: Int
) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceResponse(
    val invoice_number: String,
    val date: String,
    val item_price: Double,
    val item_quantity: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceApiResponse(val invoices: List<InvoiceResponse>)
