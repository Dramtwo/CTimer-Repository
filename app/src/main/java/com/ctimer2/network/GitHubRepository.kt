package com.ctimer2.network

/**
 * 排行榜仓库：把「上传一条成绩 / 拉取全部云端成绩」收口在这里，
 * TimerViewModel（自动同步）和 LeaderboardViewModel（排行榜页面）共用。
 */
object GitHubRepository {

    private val api get() = RetrofitClient.api

    /**
     * 拉取云端成绩（自动翻页，最多 5 页 = 500 条）。
     * 只拉 open 状态的 Issue：被关闭（兜底删除）的成绩就不会再出现在榜单里。
     * @param filterUser 非空时只返回这个用户名上传的成绩；为空则返回全部
     */
    suspend fun fetchScores(filterUser: String? = null): List<RemoteScore> {
        val all = mutableListOf<RemoteScore>()
        var page = 1
        while (page <= 5) {
            val issues = api.listIssues(
                owner = GITHUB_OWNER,
                repo = GITHUB_REPO,
                state = "open",
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
                    createdAt = issue.created_at,
                    nodeId = issue.nodeId
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

    /**
     * 彻底删除云端一条成绩。
     *
     * REST 的 DELETE /issues/{number} 对个人账号仓库不生效，所以分两级：
     * 1. GraphQL `deleteIssue` mutation —— 真正把 Issue 删掉（需要 nodeId）；
     * 2. 兜底：PATCH 把 state 改成 closed。榜单只拉 open 状态的 Issue，
     *    因此关闭后这条成绩同样不会再出现在排行榜里。
     *
     * @return null 表示已删除（或已关闭）；非 null 是失败原因
     */
    suspend fun deleteScore(issueNumber: Int, nodeId: String?): String? {
        var lastError: String? = null

        // 第一级：GraphQL 真删除
        if (!nodeId.isNullOrBlank()) {
            val gql = runCatching {
                api.graphql(
                    GraphQLRequest(
                        query = "mutation(\$id:ID!){ deleteIssue(input:{issueId:\$id}) { clientMutationId } }",
                        variables = mapOf("id" to nodeId)
                    )
                )
            }
            val body = gql.getOrNull()
            when {
                gql.isSuccess && body?.data?.deleteIssue != null -> return null
                gql.isSuccess && body?.errors?.isNotEmpty() == true ->
                    lastError = body.errors.firstOrNull()?.message ?: "GraphQL 删除被拒绝"
                gql.isFailure -> lastError = describe(gql.exceptionOrNull())
            }
        } else {
            lastError = "缺少 Issue 节点 ID"
        }

        // 第二级：PATCH 关闭（榜单只显示 open，关闭等价于移出榜单）
        val patch = runCatching {
            api.updateIssue(
                owner = GITHUB_OWNER,
                repo = GITHUB_REPO,
                number = issueNumber,
                body = UpdateIssueRequest(state = "closed")
            )
        }
        if (patch.isSuccess) return null
        val patchError = describe(patch.exceptionOrNull())
        return "$patchError（GraphQL 也失败：$lastError）"
    }

    /**
     * 带重试的删除：最多 3 次，间隔 1s / 2s。
     * 返回 null 表示成功，非 null 是最后一次失败原因。
     * 404 / 410（Issue 已不存在）视为删除成功 —— 目标状态已达成，不必报错。
     */
    suspend fun deleteScoreWithRetry(issueNumber: Int, nodeId: String?): String? {
        var lastMessage: String? = null
        repeat(3) { attempt ->
            var httpMissing = false
            val result = runCatching { deleteScore(issueNumber, nodeId) }
            val err = result.getOrNull()
            if (result.isSuccess && err == null) return null
            if (result.isSuccess && err != null) {
                lastMessage = err
                // 已经不存在了就别重试
                if (err.contains("404") || err.contains("410")) return null
            } else {
                val t = result.exceptionOrNull()
                val http = t as? retrofit2.HttpException
                if (http != null && (http.code() == 404 || http.code() == 410)) httpMissing = true
                lastMessage = describe(t)
                if (httpMissing) return null
            }
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
                410 -> "该成绩已不存在（410）"
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
