package uz.ishvaqtim.app.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supabase bazasi bilan oddiy aloqa.
 * Supabase "PostgREST" degan tayyor server orqali ishlaydi: biz jadval nomini
 * manzilga qo'shib, oddiy HTTP so'rov yuboramiz (maxsus kutubxona shart emas).
 *
 * ANON_KEY - bu "ochiq" kalit, faqat ruxsat etilgan amallarni bajaradi
 * (hozircha jadval qoidalari "hammaga ruxsat" qilib qo'yilgan, keyinroq qattiqlashtiramiz).
 */
object SupabaseClient {

    private const val PROJECT_URL = "https://hkyyrsgnoonsihbwydrs.supabase.co"
    private const val ANON_KEY = "sb_publishable_kdlfcT6hiuEXhZCwWPApeg__SFyVNsX"

    private const val TAG = "SupabaseClient"

    /**
     * Karta skanini "nfc_scans" jadvaliga yozadi.
     * Natija: true - muvaffaqiyatli yuborildi, false - internet yoki server xatosi.
     */
    suspend fun insertNfcScan(cardHash: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("card_hash", cardHash)
        }
        postJson("/rest/v1/nfc_scans", body)
    }

    private fun postJson(path: String, body: JSONObject): Boolean {
        return try {
            val url = URL(PROJECT_URL + path)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("apikey", ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer $ANON_KEY")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Prefer", "return=minimal")

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }

            val code = connection.responseCode
            connection.disconnect()

            // Supabase muvaffaqiyatli yozuvda 201 (Created) qaytaradi
            val success = code in 200..299
            if (!success) {
                Log.w(TAG, "Supabase javobi: $code")
            }
            success
        } catch (e: Exception) {
            Log.w(TAG, "Supabase xatosi: ${e.message}")
            false
        }
    }
}
