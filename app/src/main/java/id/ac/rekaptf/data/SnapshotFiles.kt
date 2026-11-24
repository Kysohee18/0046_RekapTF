package id.ac.rekaptf.data

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import id.ac.rekaptf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

internal class SnapshotError(@StringRes val text: Int) : IOException()
internal class FileLimitExceeded : IOException()

internal class SnapshotFiles(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun save(uri: Uri, recap: Recap): Unit = withContext(Dispatchers.IO) {
        val bytes = recap.toDocument().toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > SnapshotByteLimit) throw SnapshotError(R.string.snapshot_size_error)
        try {
            val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException()
            stream.use { it.write(bytes) }
        } catch (_: IOException) {
            throw SnapshotError(R.string.snapshot_write_error)
        } catch (_: SecurityException) {
            throw SnapshotError(R.string.snapshot_write_error)
        }
    }

    suspend fun open(uri: Uri): Recap = withContext(Dispatchers.IO) {
        val bytes = try {
            val stream = resolver.openInputStream(uri) ?: throw IOException()
            stream.use { it.readAtMost(SnapshotByteLimit) }
        } catch (_: FileLimitExceeded) {
            throw SnapshotError(R.string.snapshot_size_error)
        } catch (_: IOException) {
            throw SnapshotError(R.string.snapshot_read_error)
        } catch (_: SecurityException) {
            throw SnapshotError(R.string.snapshot_read_error)
        }
        try {
            val parser = JSONTokener(bytes.toString(Charsets.UTF_8))
            val content = parser.nextValue() as? JSONObject ?: throw JSONException("Expected object")
            require(parser.nextClean() == '\u0000')
            Recap.fromDocument(content)
        } catch (_: JSONException) {
            throw SnapshotError(R.string.snapshot_invalid_error)
        } catch (_: IllegalArgumentException) {
            throw SnapshotError(R.string.snapshot_invalid_error)
        }
    }

    private companion object {
        const val SnapshotByteLimit = 4 * 1024 * 1024
    }
}

internal fun InputStream.readAtMost(limit: Int): ByteArray {
    val buffer = ByteArray(8192)
    val output = ByteArrayOutputStream()
    while (true) {
        val size = read(buffer)
        if (size < 0) break
        if (size > limit - output.size()) throw FileLimitExceeded()
        output.write(buffer, 0, size)
    }
    return output.toByteArray()
}
