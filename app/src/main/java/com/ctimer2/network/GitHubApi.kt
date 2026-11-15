package com.ctimer2.network

import retrofit2.http.Body
import retrofit2.http.GET
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
}
