package com.ctimer2.network

/**
 * 排行榜仓库：把「上传一条成绩 / 拉取全部云端成绩」收口在这里，
 * TimerViewModel（自动同步）和 LeaderboardViewModel（排行榜页面）共用。
 */
object GitHubRepository {

    private val api get() = RetrofitClient.api

    /**
     * 拉取云端成绩（自动翻页，最多 5 页 = 500 条）。
     * @param filterUser 非空时只返回这个用户名上传的成绩；为空则返回全部
     */
    suspend fun fetchScores(filterUser: String? = null): List<RemoteScore> {
        val all = mutableListOf<RemoteScore>()
        var page = 1
        while (page <= 5) {
            val issues = api.listIssues(
                owner = GITHUB_OWNER,
                repo = GITHUB_REPO,
                page = page
            )
            if (issues.isEmpty()) break
            issues.forEach { issue ->
                if (issue.pullRequest != null) return@forEach
                val parsed = ScorePayload.parse(
                    body = issue.body,
                    issueNumber = issue.number,
                    author = issue.user?.login,
                    url = issue.html_url,
                    createdAt = issue.created_at
                )
                if (parsed != null) all += parsed
            }
            if (issues.size < 100) break
            page++
        }
        val distinct = all.distinctBy { it.key }
        if (filterUser.isNullOrBlank()) return distinct
        return distinct.filter { it.user.equals(filterUser, ignoreCase = true) }
    }

    /** 上传一条成绩；失败直接抛异常，方便调用方拿到真实原因 */
    suspend fun uploadScore(session: String, timeMs: Long, timestamp: Long, user: String? = null) {
        api.createIssue(
            owner = GITHUB_OWNER,
            repo = GITHUB_REPO,
            body = CreateIssueRequest(
                title = ScorePayload.buildTitle(user, session, timeMs),
                body = ScorePayload.buildBody(user, session, timeMs, timestamp),
                labels = listOf("score")
            )
        )
    }

    /** 带重试的上传：最多 3 次，间隔 1s / 2s；返回最后一次的异常信息（null 表示成功） */
    suspend fun uploadScoreWithRetry(
        session: String,
        timeMs: Long,
        timestamp: Long,
        user: String? = null
    ): String? {
        var lastMessage: String? = null
        repeat(3) { attempt ->
            val result = runCatching { uploadScore(session, timeMs, timestamp, user) }
            if (result.isSuccess) return null
            val t = result.exceptionOrNull()
            lastMessage = describe(t)
            if (attempt < 2) kotlinx.coroutines.delay(1000L * (attempt + 1))
        }
        return lastMessage
    }

    /** 把异常翻译成人能看懂的一句话 */
    fun describe(t: Throwable?): String {
        if (t == null) return "未知错误"
        val http = t as? retrofit2.HttpException
        if (http != null) {
            return when (http.code()) {
                401 -> "Token 无效或已过期（401）"
                403 -> "Token 权限不足，需 Issues 读写（403）"
                404 -> "仓库或资源不存在（404）"
                422 -> "数据被拒绝，标签可能不存在（422）"
                429 -> "请求过于频繁，稍后再试（429）"
                else -> "HTTP ${http.code()}"
            }
        }
        val msg = t.message ?: return t::class.java.simpleName
        return when {
            msg.contains("timeout", true) -> "连接超时"
            msg.contains("Unable to resolve host", true) -> "无法解析域名（网络不可用）"
            msg.contains("SSL", true) || msg.contains("certificate", true) -> "SSL 握手失败"
            else -> msg
        }
    }
}
