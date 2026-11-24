package id.ac.rekaptf.data

import id.ac.rekaptf.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

internal class ApiError(val status: Int, val detail: String?) : IOException()
internal class InvalidEndpoint : IOException()
internal data class EvidenceUpload(val bytes: ByteArray, val mimeType: String)

internal class RekapApi {
    suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        body: JSONObject? = null,
        upload: EvidenceUpload? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = (endpoint(path).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = if (path.startsWith("/v1/drafts/extract")) 65_000 else 20_000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        try {
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(bytes) }
            } else if (upload != null) {
                connection.sendEvidence(upload)
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                val message = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    val text = CharArray(4096)
                    val count = reader.read(text, 0, text.size)
                    if (count < 0) null else runCatching {
                        JSONObject(String(text, 0, count)).optString("message").trim().take(400)
                    }.getOrNull()?.takeIf(String::isNotEmpty)
                }
                throw ApiError(status, message)
            }
            if (status == HttpURLConnection.HTTP_NO_CONTENT) {
                JSONObject()
            } else {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun HttpURLConnection.sendEvidence(upload: EvidenceUpload) {
        val boundary = UUID.randomUUID().toString()
        val ext = if (upload.mimeType == "image/png") "png" else "jpg"
        val header = ("--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"image\"; filename=\"bukti.$ext\"\r\n" +
            "Content-Type: ${upload.mimeType}\r\n\r\n").toByteArray()
        val footer = "\r\n--$boundary--\r\n".toByteArray()
        doOutput = true
        setFixedLengthStreamingMode(header.size + upload.bytes.size + footer.size)
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        outputStream.use {
            it.write(header)
            it.write(upload.bytes)
            it.write(footer)
        }
    }

    private fun endpoint(path: String): URI {
        val base = runCatching { URI(BuildConfig.API_BASE_URL) }.getOrNull() ?: throw InvalidEndpoint()
        val localDebug = BuildConfig.DEBUG && base.scheme == "http" && base.host in LocalHosts
        if ((!localDebug && base.scheme != "https") || base.host == null ||
            base.userInfo != null || !base.path.isNullOrEmpty() || base.query != null || base.fragment != null
        ) {
            throw InvalidEndpoint()
        }
        return base.resolve(path)
    }

    private companion object {
        val LocalHosts = setOf("10.0.2.2", "127.0.0.1")
    }
}
