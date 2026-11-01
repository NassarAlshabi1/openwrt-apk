package com.openwrtmanager.app.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwrtmanager.app.data.model.NetworkMode
import com.openwrtmanager.app.data.model.NetworkModeConfig
import com.openwrtmanager.app.data.repository.NetworkModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetworkModesUiState(
    val mode: NetworkMode = NetworkMode.WDS_BRIDGE,
    val upstreamSsid: String = "",
    val upstreamPassword: String = "",
    val localSsid: String = "",
    val localPassword: String = "",
    val meshId: String = "",
    val meshPassword: String = "",
    val channel: String = "",
    val lanIp: String = "192.168.1.2",
    val gatewayIp: String = "192.168.1.1",
    val showConfirmation: Boolean = false,
    val running: Boolean = false,
    val resultMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class NetworkModesViewModel @Inject constructor(
    private val repository: NetworkModeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NetworkModesUiState())
    val state: StateFlow<NetworkModesUiState> = _state.asStateFlow()

    fun selectMode(mode: NetworkMode) = update { copy(mode = mode, resultMessage = null, errorMessage = null) }
    fun update(block: NetworkModesUiState.() -> NetworkModesUiState) { _state.value = _state.value.block() }

    fun requestApply() = update { copy(showConfirmation = true, resultMessage = null, errorMessage = null) }
    fun cancelApply() = update { copy(showConfirmation = false) }

    fun confirmApply() {
        val s = _state.value
        update { copy(showConfirmation = false, running = true, resultMessage = null, errorMessage = null) }
        viewModelScope.launch {
            repository.apply(
                NetworkModeConfig(
                    mode = s.mode,
                    upstreamSsid = s.upstreamSsid,
                    upstreamPassword = s.upstreamPassword,
                    localSsid = s.localSsid,
                    localPassword = s.localPassword,
                    meshId = s.meshId,
                    meshPassword = s.meshPassword,
                    channel = s.channel,
                    lanIp = s.lanIp,
                    gatewayIp = s.gatewayIp
                )
            ).onSuccess { update { copy(running = false, resultMessage = it) } }
                .onFailure { update { copy(running = false, errorMessage = it.message ?: "Network configuration failed") } }
        }
    }
}
