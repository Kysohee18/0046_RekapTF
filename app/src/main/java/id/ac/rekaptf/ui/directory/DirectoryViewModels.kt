package id.ac.rekaptf.ui.directory

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.rekaptf.R
import id.ac.rekaptf.data.Directory
import id.ac.rekaptf.data.DirectoryEntry
import id.ac.rekaptf.data.RekapRepository
import id.ac.rekaptf.data.SessionExpired
import id.ac.rekaptf.ui.components.Message
import id.ac.rekaptf.ui.components.toMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DirectoryListState(
    val items: List<DirectoryEntry>? = null,
    val query: String = "",
    val loading: Boolean = true,
    val error: Message? = null,
)

internal class DirectoryListViewModel(
    private val repository: RekapRepository,
    private val directory: Directory,
    private val onExpired: () -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(DirectoryListState())
    val state = _state.asStateFlow()
    private var request: Job? = null

    fun load(query: String = _state.value.query) {
        val cleanQuery = query.trim()
        val previous = _state.value
        request?.cancel()
        _state.value = DirectoryListState(
            items = previous.items.takeIf { cleanQuery == previous.query },
            query = cleanQuery,
        )
        request = viewModelScope.launch {
            try {
                _state.value = DirectoryListState(
                    items = repository.list(directory, cleanQuery),
                    query = cleanQuery,
                    loading = false,
                )
            } catch (error: SessionExpired) {
                onExpired()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.toMessage(R.string.error_directory_load)) }
            }
        }
    }
}

internal data class DirectoryEditorState(
    val entry: DirectoryEntry? = null,
    val busy: Boolean = false,
    val error: Message? = null,
    @StringRes val completed: Int? = null,
)

internal class DirectoryEditorViewModel(
    private val repository: RekapRepository,
    private val directory: Directory,
    private val id: String?,
    private val onExpired: () -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(DirectoryEditorState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val entryId = id ?: return
        perform(R.string.error_entry_load) {
            DirectoryEditorState(entry = repository.entry(directory, entryId))
        }
    }

    fun save(name: String, note: String) {
        if (_state.value.busy || _state.value.completed != null) return
        if (name.isBlank()) {
            _state.update { it.copy(error = Message(R.string.error_entry_name)) }
            return
        }
        perform(R.string.error_entry_save) {
            repository.save(directory, id, name, note)
            _state.value.copy(completed = R.string.entry_saved)
        }
    }

    fun delete() {
        val entryId = id ?: return
        perform(R.string.error_entry_delete) {
            repository.delete(directory, entryId)
            _state.value.copy(completed = R.string.entry_deleted)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun perform(@StringRes fallback: Int, action: suspend () -> DirectoryEditorState) {
        val current = _state.value
        if (current.busy || current.completed != null) return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                _state.value = action().copy(busy = false)
            } catch (error: SessionExpired) {
                onExpired()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = current.copy(error = error.toMessage(fallback))
            }
        }
    }
}
