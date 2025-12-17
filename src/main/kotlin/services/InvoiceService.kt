package services

import Config
import Invoice
import InvoiceApiResponse
import InvoiceListResponse
import InvoiceResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.HttpURLConnection
import java.net.URL

public class InvoiceService(val accessToken: String, val companyOrganizationId: String) {
    private val mapper = jacksonObjectMapper()

    fun findInvoicesUsingTemplate(accessToken: String, targetTemplateId: String): List<Invoice> {
        val allInvoices = getAllInvoices(accessToken)
        return filterInvoicesByTemplate(allInvoices, targetTemplateId)
    }

    fun filterInvoicesByTemplate(invoices: List<Invoice>, templateId: String): List<Invoice> {
        return invoices.filter { it.template_id == templateId }
    }

    /**
     * Get all invoices from Zoho Inventory.
     * curl --request GET \
     *  --url 'https://www.zohoapis.com/inventory/v1/invoices?organization_id=10234695&page=1' \
     */
    fun getAllInvoices(accessToken: String): List<Invoice> {
        val invoices = mutableListOf<Invoice>()
        val mapper = jacksonObjectMapper()
        var page = 1
        var hasMorePages: Boolean

        do {
            val url = URL("${Config.ZOHO_INVOICES_URL}?${Config.ORGANIZATION_ID}=${companyOrganizationId}&page=$page")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty(Config.HEADER_AUTH_KEY, "${Config.BEARER_TOKEN} $accessToken")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val invoiceResponse = mapper.readValue(response, InvoiceListResponse::class.java)
            invoices.addAll(invoiceResponse.invoices)
            hasMorePages = invoiceResponse.page_context.has_more_page
            page++
        } while (hasMorePages)

        return invoices
    }

    /**
     * Get the latest invoice for a given item.
     * curl --request GET \
     *  --url 'https://www.zohoapis.com/inventory/v1/invoices?organization_id=10234695&item_id=126938000000218631' \
     */
    fun getLatestInvoice(itemId: String): InvoiceResponse {
        val url =
                URL("${Config.ZOHO_INVOICES_URL}?${Config.ORGANIZATION_ID}=${companyOrganizationId}&item_id=$itemId")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "GET"
            setRequestProperty(Config.HEADER_AUTH_KEY, "${Config.BEARER_TOKEN} $accessToken")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        return mapper.readValue(response, InvoiceApiResponse::class.java).invoices.first()
    }

    /**
     * curl --request PUT \
     *   --url 'https://www.zohoapis.com/inventory/v1/invoices/982000000567114/templates/982000000000143?organization_id=10234695' \
     *   --header 'Authorization: Zoho-oauthtoken 1000.41d9xxxxxxxxxxxxxxxxxxxxxxxxc2d1.8fccxxxxxxxxxxxxxxxxxxxxxxxx125f'
     */
    fun updateInvoiceTemplate(accessToken: String, invoiceId: String, newTemplateId: String): String {
        val url = URL("${Config.ZOHO_INVOICES_URL}/$invoiceId/templates/${newTemplateId}?${Config.ORGANIZATION_ID}=${companyOrganizationId}")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "PUT"
            setRequestProperty(Config.HEADER_AUTH_KEY, "${Config.BEARER_TOKEN} $accessToken")
            doOutput = true
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}