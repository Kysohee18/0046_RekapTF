package id.ac.rekaptf.ui.components

import androidx.annotation.StringRes
import id.ac.rekaptf.R
import id.ac.rekaptf.data.ApiError
import id.ac.rekaptf.data.EvidenceError
import id.ac.rekaptf.data.InvalidEndpoint
import id.ac.rekaptf.data.SnapshotError
import java.io.IOException
import org.json.JSONException

internal data class Message(@StringRes val text: Int, val detail: String? = null)

internal fun Exception.toMessage(@StringRes fallback: Int): Message = when (this) {
    is ApiError -> Message(fallback, detail)
    is InvalidEndpoint -> Message(R.string.error_api_config)
    is EvidenceError -> Message(text)
    is SnapshotError -> Message(text)
    is IOException -> Message(R.string.error_network)
    is JSONException -> Message(R.string.error_server_response)
    else -> Message(fallback)
}
