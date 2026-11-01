package com.openwrtmanager.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwrtmanager.app.data.local.RouterProfileStore
import com.openwrtmanager.app.data.local.SecureCredentialStore
import com.openwrtmanager.app.data.model.RouterProfile
import com.openwrtmanager.app.data.repository.LoginOutcome
import com.openwrtmanager.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LoginUiState(
    val name: String = "",
    val baseUrl: String = "http://192.168.1.1",
    val username: String = "root",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSucceeded: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val profileStore: RouterProfileStore,
    private val credentialStore: SecureCredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val savedProfiles: StateFlow<List<RouterProfile>> =
        profileStore.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onNameChange(v: String) = update { it.copy(name = v) }
    fun onBaseUrlChange(v: String) = update { it.copy(baseUrl = v) }
    fun onUsernameChange(v: String) = update { it.copy(username = v) }
    fun onPasswordChange(v: String) = update { it.copy(password = v) }

    fun fillFromProfile(profile: RouterProfile) {
        val savedPassword = credentialStore.getPassword(profile.id) ?: ""
        update {
            it.copy(
                name = profile.name,
                baseUrl = profile.baseUrl,
                username = profile.username,
                password = savedPassword
            )
        }
    }

    fun deleteProfile(profile: RouterProfile) {
        viewModelScope.launch {
            profileStore.delete(profile.id)
            credentialStore.deletePassword(profile.id)
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.baseUrl.isBlank() || state.username.isBlank()) {
            update { it.copy(errorMessage = "Enter the router address and username") }
            return
        }
        update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val normalizedUrl = normalizeBaseUrl(state.baseUrl)
            // 复用同名+同地址的已有 profile id，否则新建
            val existing = profileStore.getAll().firstOrNull {
                it.baseUrl == normalizedUrl && it.username == state.username
            }
            val profileId = existing?.id ?: UUID.randomUUID().toString()

            when (val outcome = sessionRepository.login(profileId, normalizedUrl, state.username, state.password)) {
                is LoginOutcome.Success -> {
                    val profile = RouterProfile(
                        id = profileId,
                        name = state.name.ifBlank { normalizedUrl },
                        baseUrl = normalizedUrl,
                        username = state.username,
                        lastConnectedAt = System.currentTimeMillis()
                    )
                    profileStore.upsert(profile)
                    credentialStore.savePassword(profileId, state.password)
                    update { it.copy(isLoading = false, loginSucceeded = true) }
                }
                is LoginOutcome.WrongCredentials -> update {
                    it.copy(isLoading = false, errorMessage = outcome.message)
                }
                is LoginOutcome.NetworkFailure -> update {
                    it.copy(isLoading = false, errorMessage = "Connection failed: ${outcome.message}")
                }
                is LoginOutcome.OtherError -> update {
                    it.copy(isLoading = false, errorMessage = outcome.message)
                }
            }
        }
    }

    private fun normalizeBaseUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    private inline fun update(block: (LoginUiState) -> LoginUiState) {
        _uiState.value = block(_uiState.value)
    }
}
