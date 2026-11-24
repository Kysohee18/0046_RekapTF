package id.ac.rekaptf.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.rekaptf.R
import id.ac.rekaptf.data.RekapRepository
import id.ac.rekaptf.ui.components.Message
import id.ac.rekaptf.ui.components.toMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AccountState(
    val businessName: String? = null,
    val busy: Boolean = false,
    val error: Message? = null,
)

internal class AccountViewModel : ViewModel() {
    internal val repository = RekapRepository()
    private val _state = MutableStateFlow(AccountState())
    val state = _state.asStateFlow()

    fun signIn(email: String, password: String, businessName: String?, confirmation: String?) {
        val current = _state.value
        if (current.busy || current.businessName != null) return
        val error = when {
            businessName != null && businessName.isBlank() -> R.string.error_business_name
            email.isBlank() -> R.string.error_email_required
            password.isEmpty() -> R.string.error_password_required
            businessName != null && password.codePointCount(0, password.length) < 8 -> R.string.error_password_length
            businessName != null && password != confirmation -> R.string.error_password_confirmation
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = Message(error)) }
            return
        }
        _state.value = AccountState(busy = true)
        viewModelScope.launch {
            _state.value = try {
                AccountState(businessName = repository.signIn(email, password, businessName))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AccountState(error = error.toMessage(R.string.error_sign_in))
            }
        }
    }

    fun signOut() {
        val current = _state.value
        if (current.busy || current.businessName == null) return
        _state.value = AccountState(busy = true)
        viewModelScope.launch {
            val warning = try {
                repository.signOut()
                null
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Message(R.string.logout_revoke_unverified)
            }
            _state.value = AccountState(error = warning)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun sessionExpired() {
        if (_state.value.businessName != null) {
            _state.value = AccountState(error = Message(R.string.error_session_expired))
        }
    }
}
