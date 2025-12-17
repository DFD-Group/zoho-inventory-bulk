package authentication

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.Instant.now
import java.util.*

private const val zoho_oauth_url = "https://accounts.zoho.eu/oauth/v2/token"
private val url = URL(zoho_oauth_url)
private const val authorization_properties_path = "src/main/resources/static/authorization.properties"
private const val access_token_expiration_key = "access_token_expiration"
private const val access_token_minimum_valid_minutes = 2

private const val refresh_token_key = "refresh_token"
private const val client_id_key = "client_id"
private const val client_secret_key = "client_secret"
private const val redirect_uri_key = "redirect_uri"
private const val grant_type_key = "grant_type"

val properties = Properties()

fun getAccessToken(): String {
    properties.load(File(authorization_properties_path).inputStream())
    if (accessTokenIsValid()) {
        println("Access token is still valid. Continuing...")
        return properties.getProperty("access_token")
    }
    refreshAccessToken()
    properties.load(File(authorization_properties_path).inputStream())
    return properties.getProperty("access_token")
}

fun refreshAccessToken() {
    properties.load(File(authorization_properties_path).inputStream())
    val params = getParamsFromProperties()
    if (accessTokenIsValid()) {
        return
    }

    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.outputStream.write(params.toByteArray())

    val response = connection.inputStream.bufferedReader().use { it.readText() }
    val accessToken = parseAccessToken(response)

    val properties = Properties()
    properties.load(File(authorization_properties_path).inputStream())
    properties.setProperty("access_token", accessToken)
    val nowPlusOneHour = now().plus(Duration.ofHours(1)).toString()
    properties.setProperty(access_token_expiration_key, nowPlusOneHour)
    properties.store(File(authorization_properties_path).outputStream(), null)
}


fun accessTokenIsValid(): Boolean {
    val accessTokenExpiration = properties.getProperty(access_token_expiration_key)
    if (accessTokenExpiration != null) {
        val expirationTime = Instant.parse(accessTokenExpiration)
        val validUntil = expirationTime.minus(Duration.ofMinutes(access_token_minimum_valid_minutes.toLong()))
        if (now().isBefore(validUntil)) {
            return true
        }
    }
    return false
}

fun getParamsFromProperties(): String {
    val refreshToken = properties.getProperty(refresh_token_key)
    val clientId = properties.getProperty(client_id_key)
    val clientSecret = properties.getProperty(client_secret_key)
    val redirectUri = properties.getProperty(redirect_uri_key)
    val grantType = properties.getProperty(grant_type_key)

    return refresh_token_key + "=$refreshToken" +
            "&" + client_id_key + "=$clientId" +
            "&" + client_secret_key + "=$clientSecret" +
            "&" + redirect_uri_key + "=$redirectUri" +
            "&" + grant_type_key + "=$grantType"
}

fun parseAccessToken(response: String): String {
    // Assuming the response is in JSON format and contains an "access_token" field
    val regex = """"access_token"\s*:\s*"([^"]+)"""".toRegex()
    return regex.find(response)?.groups?.get(1)?.value ?: throw IllegalStateException("Access token not found")
}