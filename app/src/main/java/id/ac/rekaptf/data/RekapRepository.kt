package id.ac.rekaptf.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder

internal enum class Directory(val path: String) {
    Categories("categories"),
    Payers("payers"),
}

internal data class DirectoryEntry(val id: String, val name: String, val note: String) {
    constructor(json: JSONObject) : this(json.getString("id"), json.getString("name"), json.optString("note"))
}

internal class SessionExpired : Exception()

internal class RekapRepository {
    private val api = RekapApi()
    private val lock = Mutex()
    private var session: Session? = null

    suspend fun signIn(email: String, password: String, businessName: String?): String = lock.withLock {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val action = if (businessName == null) "login" else "register"
        businessName?.let { body.put("businessName", it.trim()) }
        val signedIn = Session(api.request("POST", "/v1/auth/$action", body = body))
        session = signedIn
        signedIn.businessName
    }

    suspend fun signOut() {
        val current = lock.withLock {
            val active = session
            session = null
            active
        } ?: return
        api.request("POST", "/v1/auth/logout", body = current.refreshBody())
    }

    suspend fun list(directory: Directory, query: String = ""): List<DirectoryEntry> {
        val encodedQuery = encode(query.trim())
        return authenticated("GET", "/v1/${directory.path}?q=$encodedQuery").decodeItems(::DirectoryEntry)
    }

    suspend fun entry(directory: Directory, id: String): DirectoryEntry =
        DirectoryEntry(authenticated("GET", itemPath(directory, id)))

    suspend fun save(directory: Directory, id: String?, name: String, note: String) {
        val body = JSONObject().put("name", name.trim()).put("note", note.trim())
        if (id == null) {
            authenticated("POST", "/v1/${directory.path}", body)
        } else {
            authenticated("PUT", itemPath(directory, id), body)
        }
    }

    suspend fun delete(directory: Directory, id: String) {
        authenticated("DELETE", itemPath(directory, id))
    }

    suspend fun isDemo(): Boolean = api.request("GET", "/health").getString("mode") == "demo"

    suspend fun extractEvidence(rawText: String): Income = Income(
        authenticated("POST", "/v1/drafts/extract-text", JSONObject().put("text", rawText)),
    )

    suspend fun extractEvidence(image: ByteArray, mimeType: String): Income = Income(
        authenticated("POST", "/v1/drafts/extract", upload = EvidenceUpload(image, mimeType)),
    )

    suspend fun listIncome(drafts: Boolean, query: String = "", range: DateRange? = null): List<Income> {
        val dates = range?.let { "&from=${encode(it.from)}&to=${encode(it.to)}" }.orEmpty()
        val path = "/v1/${incomePath(drafts)}?q=${encode(query.trim())}$dates"
        return authenticated("GET", path).decodeItems(::Income)
    }

    suspend fun getIncome(drafts: Boolean, id: String): Income =
        Income(authenticated("GET", itemPath(incomePath(drafts), id)))

    suspend fun saveIncome(drafts: Boolean, income: Income): Income =
        Income(authenticated("PUT", itemPath(incomePath(drafts), income.id), income.fields()))

    suspend fun deleteIncome(drafts: Boolean, id: String) {
        authenticated("DELETE", itemPath(incomePath(drafts), id))
    }

    suspend fun confirmDraft(income: Income): Income =
        Income(authenticated("POST", itemPath("drafts", income.id) + "/confirm", income.fields()))

    suspend fun recap(range: DateRange): Recap {
        val response = authenticated("GET", "/v1/recap?from=${encode(range.from)}&to=${encode(range.to)}")
        return withContext(Dispatchers.Default) { Recap(response) }
    }

    suspend fun savedRecaps(query: String = ""): List<SavedRecap> =
        authenticated("GET", "/v1/saved-recaps?q=${encode(query.trim())}").decodeItems(::SavedRecap)

    suspend fun savedRecap(id: String): SavedRecap = SavedRecap(authenticated("GET", itemPath("saved-recaps", id)))

    suspend fun saveRecap(recap: SavedRecap): SavedRecap = if (recap.id.isBlank()) {
        SavedRecap(authenticated("POST", "/v1/saved-recaps", recap.fields()))
    } else {
        SavedRecap(authenticated("PUT", itemPath("saved-recaps", recap.id), recap.fields()))
    }

    suspend fun deleteRecap(id: String) {
        authenticated("DELETE", itemPath("saved-recaps", id))
    }

    private suspend fun <T> JSONObject.decodeItems(transform: (JSONObject) -> T): List<T> =
        withContext(Dispatchers.Default) { mapObjects("items", transform) }

    private fun incomePath(drafts: Boolean): String = if (drafts) "drafts" else "transactions"

    private fun itemPath(directory: Directory, id: String): String =
        itemPath(directory.path, id)

    private fun itemPath(collection: String, id: String): String = "/v1/$collection/${encode(id)}"

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private suspend fun authenticated(
        method: String,
        path: String,
        body: JSONObject? = null,
        upload: EvidenceUpload? = null,
    ): JSONObject =
        lock.withLock {
            val current = session ?: throw SessionExpired()
            try {
                return@withLock api.request(method, path, current.accessToken, body, upload)
            } catch (error: ApiError) {
                if (error.status != HttpURLConnection.HTTP_UNAUTHORIZED) throw error
            }
            val updated = withContext(NonCancellable) {
                // A successful rotation must be kept even if its screen is left during the request.
                rejectExpiredSession {
                    Session(api.request("POST", "/v1/auth/refresh", body = current.refreshBody()))
                        .also { session = it }
                }
            }
            rejectExpiredSession { api.request(method, path, updated.accessToken, body, upload) }
        }

    private suspend fun <T> rejectExpiredSession(action: suspend () -> T): T = try {
        action()
    } catch (error: ApiError) {
        if (error.status != HttpURLConnection.HTTP_UNAUTHORIZED) throw error
        session = null
        throw SessionExpired()
    }

    private data class Session(val accessToken: String, val refreshToken: String, val businessName: String) {
        constructor(json: JSONObject) : this(
            json.getString("accessToken"),
            json.getString("refreshToken"),
            json.getString("businessName"),
        )

        fun refreshBody(): JSONObject = JSONObject().put("refreshToken", refreshToken)
    }
}
