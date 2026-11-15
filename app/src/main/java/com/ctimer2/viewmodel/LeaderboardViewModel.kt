package com.ctimer2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ctimer2.network.GitHubRepository
import com.ctimer2.network.RemoteScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LeaderboardUiState(
    val scores: List<RemoteScore> = emptyList(),
    val loading: Boolean = false,
    val uploading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

/**
 * 云端排行榜状态机：拉取 / 上传都走协程，结果用 StateFlow 推给 UI。
 */
class LeaderboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    /** 从 GitHub Issues 拉取全部成绩并按成绩升序排列 */
    fun refresh(userName: String? = null) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { GitHubRepository.fetchScores(userName) }
            }
            result.fold(
                onSuccess = { list ->
                    _state.update {
                        it.copy(
                            scores = list.sortedBy { s -> s.timeMs },
                            loading = false,
                            error = null,
                            message = if (list.isEmpty()) "云端暂无成绩" else null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = "拉取失败：${e.message ?: "网络异常"}"
                        )
                    }
                }
            )
        }
    }

    /** 手动上传一条成绩（秒数由输入框给出，例如 12.34） */
    fun upload(secondsText: String, session: String, userName: String? = null) {
        val parsed = secondsText.trim().toDoubleOrNull()
        if (parsed == null || parsed <= 0) {
            _state.update { it.copy(error = "成绩格式不对，请输入例如 12.34") }
            return
        }
        val ms = Math.round(parsed * 1000.0)
        val name = session.ifBlank { "云端成绩" }
        _state.update { it.copy(uploading = true, error = null, message = null) }
        viewModelScope.launch {
            val err = withContext(Dispatchers.IO) {
                GitHubRepository.uploadScoreWithRetry(name, ms, System.currentTimeMillis(), userName)
            }
            if (err == null) {
                _state.update { it.copy(uploading = false, message = "已上传") }
                refresh(userName)
            } else {
                _state.update { it.copy(uploading = false, error = "上传失败：$err") }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }
}
