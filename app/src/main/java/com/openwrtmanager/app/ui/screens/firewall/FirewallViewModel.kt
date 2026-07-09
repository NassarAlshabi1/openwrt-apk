package com.openwrtmanager.app.ui.screens.firewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwrtmanager.app.data.model.FirewallRule
import com.openwrtmanager.app.data.model.FirewallZone
import com.openwrtmanager.app.data.repository.FirewallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FirewallUiState(
    val isLoading: Boolean = true,
    val zones: List<FirewallZone> = emptyList(),
    val rules: List<FirewallRule> = emptyList(),
    val errorMessage: String? = null,
    val hasPendingChanges: Boolean = false,
    val isApplying: Boolean = false
)

@HiltViewModel
class FirewallViewModel @Inject constructor(
    private val firewallRepository: FirewallRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirewallUiState())
    val uiState: StateFlow<FirewallUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val zonesResult = firewallRepository.getZones()
            val rulesResult = firewallRepository.getRules()
            if (zonesResult.isSuccess && rulesResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    zones = zonesResult.getOrDefault(emptyList()),
                    rules = rulesResult.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = zonesResult.exceptionOrNull()?.message
                        ?: rulesResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun toggleRule(rule: FirewallRule) {
        viewModelScope.launch {
            firewallRepository.setRuleEnabled(rule.sectionId, !rule.enabled)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(hasPendingChanges = true)
                    refresh()
                }
        }
    }

    fun deleteRule(rule: FirewallRule) {
        viewModelScope.launch {
            firewallRepository.deleteRule(rule.sectionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(hasPendingChanges = true)
                    refresh()
                }
        }
    }

    fun addRule(name: String, src: String?, dest: String?, destPort: String?, proto: String?, target: String) {
        viewModelScope.launch {
            firewallRepository.addRule(name, src, dest, destPort, proto, target)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(hasPendingChanges = true)
                    refresh()
                }
        }
    }

    /** 提交并应用改动（reload firewall）——危险操作，UI 需二次确认 */
    fun applyChanges() {
        _uiState.value = _uiState.value.copy(isApplying = true)
        viewModelScope.launch {
            firewallRepository.commitAndApply()
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
            firewallRepository.discardChanges()
            _uiState.value = _uiState.value.copy(hasPendingChanges = false)
            refresh()
        }
    }
}
