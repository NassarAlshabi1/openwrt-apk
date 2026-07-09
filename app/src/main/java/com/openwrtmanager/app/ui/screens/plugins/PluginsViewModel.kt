package com.openwrtmanager.app.ui.screens.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwrtmanager.app.data.model.InstalledPlugin
import com.openwrtmanager.app.data.repository.PluginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PluginsUiState(
    val isLoading: Boolean = true,
    val plugins: List<InstalledPlugin> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val pluginRepository: PluginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginsUiState())
    val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            pluginRepository.getInstalledPlugins()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(isLoading = false, plugins = list)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.message)
                }
        }
    }
}
