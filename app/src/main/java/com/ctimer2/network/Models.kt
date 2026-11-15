package com.ctimer2.network

import com.google.gson.annotations.SerializedName

/** GitHub Issue（排行榜一条记录） */
data class IssueDto(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    val user: IssueUserDto? = null,
    val labels: List<IssueLabelDto>? = emptyList(),
    val state: String? = null,
    val created_at: String? = null,
    val html_url: String? = null,
    /** GraphQL 的全局节点 ID（形如 I_kwDOxxx）。彻底删除 Issue 必须用它 */
    @SerializedName("node_id") val nodeId: String? = null,
    /** 如果非 null 说明这条其实是 Pull Request，排行榜要过滤掉 */
    @SerializedName("pull_request") val pullRequest: Any? = null
)

data class IssueUserDto(val login: String? = null)

data class IssueLabelDto(val name: String? = null)

/** 创建 Issue 的请求体 */
data class CreateIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = emptyList()
)

/** 修改 Issue 的请求体（目前只用来把状态改成 closed） */
data class UpdateIssueRequest(val state: String)

/** GraphQL 请求体：{ query, variables } */
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap()
)

/** GraphQL 响应：要么 data 有值，要么 errors 里说明原因 */
data class GraphQLResponse(
    val data: GraphQLDeleteData? = null,
    val errors: List<GraphQLError>? = null
)

data class GraphQLDeleteData(val deleteIssue: GraphQLDeleteResult? = null)

data class GraphQLDeleteResult(val clientMutationId: String? = null)

data class GraphQLError(
    val message: String? = null,
    val type: String? = null
)

/** 从 Issue 解析出来的成绩记录 */
data class RemoteScore(
    val issueNumber: Int,
    val user: String? = null,
    val session: String,
    val timeMs: Long,
    val timestamp: Long,
    val author: String?,
    val issueUrl: String?,
    val createdAt: String?,
    /** GraphQL 节点 ID，删除时用；老数据/解析失败可能为 null */
    val nodeId: String? = null
) {
    /** 同步去重键：同一列表 + 同一时间戳 + 同一成绩 视为同一条 */
    val key: String get() = "${user ?: ""}|$session|$timestamp|$timeMs"

    val displayTime: String
        get() {
            val totalCs = timeMs / 10
            val m = totalCs / 6000
            val s = (totalCs % 6000) / 100
            val cs = totalCs % 100
            return if (m > 0) {
                String.format("%d:%02d.%02d", m, s, cs)
            } else {
                String.format("%d.%02d", s, cs)
            }
        }
}

/** 封装成绩 → Issue 标题 / 正文 */
object ScorePayload {

    private const val MARK_START = "<!--CT2SCORE:"
    private const val MARK_END = ":CT2SCORE-->"

    /** 上传时用的标题，方便在 GitHub 上一眼看出是哪个成绩 */
    fun buildTitle(user: String?, session: String, timeMs: Long): String {
        val t = RemoteScore(0, null, session, timeMs, 0, null, null, null).displayTime
        val prefix = if (user.isNullOrBlank()) "" else "[$user] "
        return "[CTimer2] $prefix$t · $session"
    }

    /** 上传时用的正文：一段可读信息 + 一段机器可解析的 JSON */
    fun buildBody(user: String?, session: String, timeMs: Long, timestamp: Long): String {
        val json = "{\"v\":1,\"app\":\"CTimer2\",\"user\":\"${escape(user ?: "")}\"," +
            "\"session\":\"${escape(session)}\",\"ms\":$timeMs,\"ts\":$timestamp}"
        val t = RemoteScore(0, null, session, timeMs, 0, null, null, null).displayTime
        return buildString {
            append("**CTimer2 云端成绩**\n\n")
            append("| 项目 | 值 |\n|---|---|\n")
            append("| 成绩 | $t |\n")
            append("| 用户 | ${if (user.isNullOrBlank()) "—" else user} |\n")
            append("| 列表 | $session |\n")
            append("| 毫秒 | $timeMs |\n")
            append("| 时间戳 | $timestamp |\n\n")
            append(MARK_START).append(json).append(MARK_END)
        }
    }

    /** 从任意文本里抠出成绩 JSON 并解析，失败返回 null */
    fun parse(
        body: String?,
        issueNumber: Int,
        author: String?,
        url: String?,
        createdAt: String?,
        nodeId: String? = null
    ): RemoteScore? {
        if (body.isNullOrBlank()) return null
        val start = body.indexOf(MARK_START)
        if (start < 0) return null
        val jsonStart = start + MARK_START.length
        val end = body.indexOf(MARK_END, jsonStart)
        if (end < 0) return null
        val json = body.substring(jsonStart, end)
        return runCatching {
            val obj = org.json.JSONObject(json)
            if (obj.optString("app") != "CTimer2") return null
            val ms = obj.optLong("ms", 0L)
            if (ms <= 0) return null
            RemoteScore(
                issueNumber = issueNumber,
                user = obj.optString("user", "").ifBlank { null },
                session = obj.optString("session", "云端成绩").ifBlank { "云端成绩" },
                timeMs = ms,
                timestamp = obj.optLong("ts", 0L),
                author = author,
                issueUrl = url,
                createdAt = createdAt,
                nodeId = nodeId
            )
        }.getOrNull()
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
}
