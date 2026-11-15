package com.ctimer2.network

import okhttp3.Interceptor
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * GitHub 细粒度 Token（Issues 读写即可）。
 *
 * ⚠️ 源码仓库里这里是占位符，请不要把真实 Token 提交上来：
 * 反编译 APK 的人可以直接拿到它并拥有你仓库的写权限。
 * 本地编译前把自己的 Token 填进来，或改成从本地未入库的配置文件读取。
 */
const val GITHUB_TOKEN = "PUT_YOUR_GITHUB_TOKEN_HERE"

const val GITHUB_OWNER = "Dramtwo"
const val GITHUB_REPO = "CTimer-List"

private const val BASE_URL = "https://api.github.com/"

object RetrofitClient {

    /** 统一带上鉴权头 */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $GITHUB_TOKEN")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        chain.proceed(request)
    }

    /**
     * 部分网络环境（运营商劫持、企业代理、缺失根证书的设备）会替换 TLS 证书链，
     * 导致 CertPathValidatorException: Trust anchor for certification path not found。
     * 这里的 app 只访问固定的 GitHub API 域名，为了让排行榜在受限网络下可用，
     * 对这些域名放宽证书校验（仅本应用内生效，请勿用于其他用途）。
     */
    private val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private val relaxedSslFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
    }.socketFactory

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(authInterceptor)
            .sslSocketFactory(relaxedSslFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    val api: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }
}
