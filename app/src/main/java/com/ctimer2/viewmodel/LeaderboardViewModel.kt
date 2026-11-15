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
    /** 正在删除的 Issue 编号集合，UI 据此在对应行显示进度并禁用按钮 */
    val deleting: Set<Int> = emptySet(),
    /** null = 所有用户；否则只看这个用户名上传的成绩 */
    val selectedUser: String? = null,
    val message: String? = null,
    val error: String? = null
) {
    /** 云端全量成绩里出现过的用户名，供右上角下拉选择 */
    val availableUsers: List<String>
        get() = scores.mapNotNull { it.user?.takeIf { u -> u.isNotBlank() } }
            .distinct()
            .sorted()

    /** 按当前选择过滤后要显示的列表 */
    val visibleScores: List<RemoteScore>
        get() = selectedUser?.let { u -> scores.filter { it.user.equals(u, ignoreCase = true) } }
            ?: scores
}

/**
 * 云端排行榜状态机：拉取 / 上传都走协程，结果用 StateFlow 推给 UI。
 */
class LeaderboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    /**
     * 从 GitHub Issues 拉取**全部**成绩并按成绩升序排列。
     * 不再按登录用户名过滤：所有用户的数据都拿下来，由 UI 的用户下拉框筛选，
     * 避免换设备 / 换用户名后看不到先前上传的成绩（成绩"丢失"的错觉）。
     */
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { GitHubRepository.fetchScores(null) }
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
                refresh()
            } else {
                _state.update { it.copy(uploading = false, error = "上传失败：$err") }
            }
        }
    }

    /**
     * 删除云端一条成绩：DELETE 掉对应的 GitHub Issue，成功后从列表里移除。
     * 失败不做本地移除（保持和云端一致），并把原因显示在提示条上。
     */
    fun deleteScore(score: RemoteScore, userName: String? = null) {
        val number = score.issueNumber
        if (number <= 0) {
            _state.update { it.copy(error = "这条记录缺少 Issue 编号，无法删除") }
            return
        }
        _state.update { it.copy(deleting = it.deleting + number, error = null, message = null) }
        viewModelScope.launch {
            val err = withContext(Dispatchers.IO) {
                GitHubRepository.deleteScoreWithRetry(number, score.nodeId)
            }
            if (err == null) {
                _state.update { st ->
                    st.copy(
                        deleting = st.deleting - number,
                        scores = st.scores.filterNot { it.issueNumber == number },
                        message = "已从 GitHub 删除成绩 ${score.displayTime}",
                        error = null
                    )
                }
            } else {
                _state.update { st ->
                    st.copy(
                        deleting = st.deleting - number,
                        error = "删除失败：$err"
                    )
                }
            }
        }
    }

    /** 右上角下拉框选择用户；null = 所有用户 */
    fun selectUser(user: String?) {
        _state.update { it.copy(selectedUser = user, message = null, error = null) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }
}
