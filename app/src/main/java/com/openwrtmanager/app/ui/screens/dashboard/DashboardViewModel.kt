package com.openwrtmanager.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwrtmanager.app.data.model.SystemStatus
import com.openwrtmanager.app.data.repository.SessionRepository
import com.openwrtmanager.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val status: SystemStatus? = null,
    val errorMessage: String? = null,
    val rebootRequested: Boolean = false,
    val rebootInProgress: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val systemRepository: SystemRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            systemRepository.getSystemStatus()
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(isLoading = false, status = status)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.message)
                }
        }
    }

    fun requestReboot() {
        _uiState.value = _uiState.value.copy(rebootRequested = true)
    }

    fun cancelReboot() {
        _uiState.value = _uiState.value.copy(rebootRequested = false)
    }

    fun confirmReboot() {
        _uiState.value = _uiState.value.copy(rebootRequested = false, rebootInProgress = true)
        viewModelScope.launch {
            systemRepository.reboot()
            _uiState.value = _uiState.value.copy(rebootInProgress = false)
        }
    }

    fun logout() {
        sessionRepository.logout()
    }
}
