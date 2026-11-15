package com.ctimer2.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * GitHub Issues 接口。
 * 排行榜的数据载体就是仓库里的 Issue：一条成绩 = 一个 Issue。
 */
interface GitHubApi {

    /** 列出仓库全部 Issue（包含已关闭的），分页拉取 */
    @GET("repos/{owner}/{repo}/issues")
    suspend fun listIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "all",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "created",
        @Query("direction") direction: String = "desc"
    ): List<IssueDto>

    /** 新建一条成绩 Issue */
    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest
    ): IssueDto

    /**
     * 修改一条成绩 Issue。
     * REST 没有可用的「彻底删除 Issue」端点（DELETE /issues/{number} 对个人仓库不生效），
     * 所以删除走两条路：优先 GraphQL deleteIssue 真删除，失败时用这个把状态改成 closed 兜底。
     */
    @PATCH("repos/{owner}/{repo}/issues/{number}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: UpdateIssueRequest
    ): IssueDto

    /**
     * GitHub GraphQL（POST https://api.github.com/graphql）。
     * 彻底删除 Issue 的唯一可靠方式：
     * mutation { deleteIssue(input:{issueId:"<node_id>"}) { clientMutationId } }
     */
    @POST("graphql")
    suspend fun graphql(@Body body: GraphQLRequest): GraphQLResponse
}
