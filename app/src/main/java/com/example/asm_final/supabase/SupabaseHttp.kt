package com.example.asm_final.supabase

import android.content.Context
import android.os.Looper
import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.BackendClient
import org.json.JSONObject
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL

/** Deep link used by Supabase confirmation emails to return to the Android app. */
object SupabaseAuthRedirect {
    const val CALLBACK_URL = "aimentor://auth/callback"

    @JvmStatic fun withCallback(path: String): String =
        "$path?redirect_to=${Uri.encode(CALLBACK_URL)}"
}

/** Bounded HTTPS transport. Never logs credentials or silently retries token mutations. */
class SupabaseHttp : SupabaseTransport {
    companion object {
        @JvmStatic fun enabled(context: Context): Boolean = BuildConfig.SUPABASE_ENABLED &&
            !context.getSharedPreferences("backend_test", 0).contains("endpoint")
    }
    override fun request(method: String, path: String, body: JSONObject?, token: String?): String {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Network on main thread" }
        require(path.startsWith("/") && !path.startsWith("//") && !path.contains(".."))
        val connection = URL(BuildConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 12000; connection.readTimeout = if(path.startsWith("/functions/")) 60000 else 20000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val input = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = input?.use { stream ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(4096)
                while (true) { val n=stream.read(chunk); if(n<0) break; require(buffer.size()+n<=1024*1024); buffer.write(chunk,0,n) }
                buffer.toString("UTF-8")
            }.orEmpty()
            if (status !in 200..299) {
                val failure = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                val code = failure.optString("error_code", failure.optString("code", ""))
                val mapped = when {
                    code in listOf("NOT_CONFIGURED","RATE_LIMITED","INVALID_REQUEST","EMPTY_RESPONSE","PROVIDER","UNAUTHORIZED") -> code
                    code.matches(Regex("PT[0-9]{3}")) && failure.optString("message").matches(Regex("[A-Z_]{1,80}")) -> failure.getString("message")
                    code == "email_address_not_authorized" -> "EMAIL_SENDING_NOT_CONFIGURED"
                    code == "email_not_confirmed" -> "EMAIL_CONFIRMATION_REQUIRED"
                    code == "invalid_credentials" -> "INVALID_CREDENTIALS"
                    code == "user_already_exists" -> "ACCOUNT_ALREADY_EXISTS"
                    status == 429 -> "RATE_LIMITED"
                    else -> "SUPABASE_REQUEST_FAILED"
                }
                throw BackendClient.Failure(mapped, status)
            }
            return text
        } finally { connection.disconnect() }
    }
}
