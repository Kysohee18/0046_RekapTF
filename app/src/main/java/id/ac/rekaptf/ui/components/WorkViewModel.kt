package id.ac.rekaptf.ui.components

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.rekaptf.data.SessionExpired
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class WorkState(
    val loading: Boolean = false,
    val busy: Boolean = false,
    val value: Any? = null,
    val error: Message? = null,
)

internal sealed interface WorkOutcome {
    data class Draft(val id: String) : WorkOutcome
    data class Confirmed(val id: String) : WorkOutcome
    data object Back : WorkOutcome
}

internal class WorkViewModel(private val onExpired: () -> Unit) : ViewModel() {
    private val mutable = MutableStateFlow(WorkState())
    val state = mutable.asStateFlow()
    private val completed = MutableStateFlow<WorkOutcome?>(null)
    val outcome = completed.asStateFlow()
    private var loading: Job? = null
    private var action: Job? = null

    fun <T : Any> load(@StringRes fallback: Int, task: suspend () -> T) {
        if (mutable.value.busy || completed.value != null) return
        loading?.cancel()
        mutable.value = WorkState(loading = true)
        loading = viewModelScope.launch {
            try {
                mutable.value = WorkState(value = task())
            } catch (error: CancellationException) {
                throw error
            } catch (error: SessionExpired) {
                onExpired()
            } catch (error: Exception) {
                mutable.value = WorkState(error = error.toMessage(fallback))
            }
        }
    }

    fun act(@StringRes fallback: Int, task: suspend () -> WorkOutcome) {
        if (mutable.value.busy || completed.value != null) return
        mutable.value = mutable.value.copy(busy = true, error = null)
        action = viewModelScope.launch {
            try {
                completed.value = task()
                mutable.value = mutable.value.copy(busy = false)
            } catch (error: CancellationException) {
                mutable.value = mutable.value.copy(busy = false)
                throw error
            } catch (error: SessionExpired) {
                mutable.value = mutable.value.copy(busy = false)
                onExpired()
            } catch (error: Exception) {
                mutable.value = mutable.value.copy(busy = false, error = error.toMessage(fallback))
            }
        }
    }

    fun consumeOutcome(expected: WorkOutcome): Boolean = completed.compareAndSet(expected, null)

    fun cancel() {
        action?.cancel()
    }

    fun clearError() {
        mutable.value = mutable.value.copy(error = null)
    }
}
