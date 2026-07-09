package com.openwrtmanager.app.ui.screens.dhcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwrtmanager.app.data.model.DhcpPool
import com.openwrtmanager.app.data.model.StaticLease
import com.openwrtmanager.app.data.repository.DhcpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DhcpUiState(
    val isLoading: Boolean = true,
    val pools: List<DhcpPool> = emptyList(),
    val staticLeases: List<StaticLease> = emptyList(),
    val errorMessage: String? = null,
    val hasPendingChanges: Boolean = false,
    val isApplying: Boolean = false
)

@HiltViewModel
class DhcpViewModel @Inject constructor(
    private val dhcpRepository: DhcpRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DhcpUiState())
    val uiState: StateFlow<DhcpUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val poolsResult = dhcpRepository.getPools()
            val leasesResult = dhcpRepository.getStaticLeases()
            if (poolsResult.isSuccess && leasesResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pools = poolsResult.getOrDefault(emptyList()),
                    staticLeases = leasesResult.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = poolsResult.exceptionOrNull()?.message
                        ?: leasesResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun togglePoolIgnored(pool: DhcpPool) {
        viewModelScope.launch {
            dhcpRepository.setPoolIgnored(pool.sectionId, !pool.ignore)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(hasPendingChanges = true)
                    refresh()
                }
        }
    }

    fun addStaticLease(name: String, mac: String, ip: String) {
        viewModelScope.launch {
            dhcpRepository.addStaticLease(name, mac, ip)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(hasPendingChanges = true)
                    refresh()
                }
        }
    }

    fun deleteStaticLease(lease: StaticLease) {
        viewModelScope.launch {
            dhcpRepository.deleteStaticLease(lease.sectionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(hasPendingChanges = true)
                    refresh()
                }
        }
    }

    fun applyChanges() {
        _uiState.value = _uiState.value.copy(isApplying = true)
        viewModelScope.launch {
            dhcpRepository.commitAndApply()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isApplying = false, hasPendingChanges = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isApplying = false, errorMessage = err.message)
                }
        }
    }

    fun discardChanges() {
        viewModelScope.launch {
            dhcpRepository.discardChanges()
            _uiState.value = _uiState.value.copy(hasPendingChanges = false)
            refresh()
        }
    }
}
