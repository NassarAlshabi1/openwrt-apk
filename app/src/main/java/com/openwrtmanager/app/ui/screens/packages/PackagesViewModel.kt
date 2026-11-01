package com.openwrtmanager.app.ui.screens.packages
import com.openwrtmanager.app.ui.common.AppStrings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwrtmanager.app.data.model.OpkgPackage
import com.openwrtmanager.app.data.model.OpkgTaskState
import com.openwrtmanager.app.data.repository.OpkgRepository
import com.openwrtmanager.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PackagesTab { INSTALLED, UPGRADABLE, SEARCH }

data class PackagesUiState(
    val selectedTab: PackagesTab = PackagesTab.INSTALLED,
    val isLoadingList: Boolean = false,
    val installed: List<OpkgPackage> = emptyList(),
    val upgradable: List<OpkgPackage> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<OpkgPackage> = emptyList(),
    val listErrorMessage: String? = null,

    // 空间检查用
    val rootfsFreeKb: Long = 0L,

    // 点击AppStrings.text("Install")后，先异步查询包详情（体积等），再展示确认对话框
    val pendingInstallTarget: OpkgPackage? = null,
    val pendingInstallInfo: String? = null,
    val isFetchingPendingInstallInfo: Boolean = false,

    // 当前展示的任务日志面板（Install/Uninstall/Upgrade时弹出）
    val taskState: OpkgTaskState = OpkgTaskState.Idle,
    val taskPanelVisible: Boolean = false
)

@HiltViewModel
class PackagesViewModel @Inject constructor(
    private val opkgRepository: OpkgRepository,
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackagesUiState())
    val uiState: StateFlow<PackagesUiState> = _uiState.asStateFlow()

    init {
        loadInstalled()
        loadFreeSpace()
    }

    fun selectTab(tab: PackagesTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        when (tab) {
            PackagesTab.INSTALLED -> loadInstalled()
            PackagesTab.UPGRADABLE -> loadUpgradable()
            PackagesTab.SEARCH -> Unit // 等待用户输入关键字
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun search() {
        val query = _uiState.value.searchQuery
        _uiState.value = _uiState.value.copy(isLoadingList = true, listErrorMessage = null)
        viewModelScope.launch {
            opkgRepository.findPackages(query)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(isLoadingList = false, searchResults = list)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoadingList = false, listErrorMessage = err.message)
                }
        }
    }

    private fun loadInstalled() {
        _uiState.value = _uiState.value.copy(isLoadingList = true, listErrorMessage = null)
        viewModelScope.launch {
            opkgRepository.getInstalledPackages()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(isLoadingList = false, installed = list)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoadingList = false, listErrorMessage = err.message)
                }
        }
    }

    private fun loadUpgradable() {
        _uiState.value = _uiState.value.copy(isLoadingList = true, listErrorMessage = null)
        viewModelScope.launch {
            opkgRepository.getUpgradablePackages()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(isLoadingList = false, upgradable = list)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoadingList = false, listErrorMessage = err.message)
                }
        }
    }

    private fun loadFreeSpace() {
        viewModelScope.launch {
            systemRepository.getSystemStatus().onSuccess { status ->
                _uiState.value = _uiState.value.copy(rootfsFreeKb = status.rootfsFreeKb)
            }
        }
    }

    /** 检查空间是否可能不足（基于已拉取的包详情文本中的 Installed-Size）。信息不足时不报警。 */
    fun isPendingInstallSpaceLikelyInsufficient(): Boolean {
        val freeKb = _uiState.value.rootfsFreeKb
        val info = _uiState.value.pendingInstallInfo ?: return false
        val sizeBytes = opkgRepository.parseInstalledSizeBytes(info) ?: return false
        if (freeKb <= 0) return false
        val sizeKb = sizeBytes / 1024
        return sizeKb > freeKb * 0.9
    }

    /** 用户点击AppStrings.text("Install")时调用：弹出确认对话框并异步拉取包详情（含体积等），用于展示更完整的 opkg 体验 */
    fun requestInstall(pkg: OpkgPackage) {
        _uiState.value = _uiState.value.copy(
            pendingInstallTarget = pkg,
            pendingInstallInfo = null,
            isFetchingPendingInstallInfo = true
        )
        viewModelScope.launch {
            opkgRepository.getPackageInfo(pkg.name)
                .onSuccess { info ->
                    _uiState.value = _uiState.value.copy(isFetchingPendingInstallInfo = false, pendingInstallInfo = info)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isFetchingPendingInstallInfo = false, pendingInstallInfo = null)
                }
        }
    }

    fun cancelPendingInstall() {
        _uiState.value = _uiState.value.copy(pendingInstallTarget = null, pendingInstallInfo = null)
    }

    fun confirmPendingInstall() {
        val pkg = _uiState.value.pendingInstallTarget ?: return
        _uiState.value = _uiState.value.copy(pendingInstallTarget = null, pendingInstallInfo = null)
        installPackage(pkg.name)
    }

    fun runUpdate() = runTask { opkgRepository.update() }

    fun installPackage(name: String) = runTask { opkgRepository.install(name) }

    fun removePackage(name: String) = runTask { opkgRepository.remove(name) }

    fun upgradePackage(name: String) = runTask { opkgRepository.upgrade(name) }

    fun upgradeAll() {
        val names = _uiState.value.upgradable.map { it.name }
        if (names.isEmpty()) return
        runTask { opkgRepository.upgradeAll(names) }
    }

    fun dismissTaskPanel() {
        _uiState.value = _uiState.value.copy(taskPanelVisible = false, taskState = OpkgTaskState.Idle)
        // Close日志面板后Refresh当前 tab，让操作结果反映到列表里
        when (_uiState.value.selectedTab) {
            PackagesTab.INSTALLED -> loadInstalled()
            PackagesTab.UPGRADABLE -> loadUpgradable()
            PackagesTab.SEARCH -> Unit
        }
        loadFreeSpace()
    }

    private fun runTask(flowProvider: () -> kotlinx.coroutines.flow.Flow<OpkgTaskState>) {
        _uiState.value = _uiState.value.copy(taskPanelVisible = true, taskState = OpkgTaskState.Running(emptyList()))
        viewModelScope.launch {
            flowProvider().collect { state ->
                _uiState.value = _uiState.value.copy(taskState = state)
            }
        }
    }
}
