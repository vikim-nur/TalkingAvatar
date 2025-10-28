package kg.nurtelecom.o.talkingavatar.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (isAuthRequest(originalRequest.url.toString())) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking {
            try {
                tokenManager.getAccessToken()
            } catch (e: Exception) {
                null
            }
        }


        val authenticatedRequest = originalRequest.newBuilder()
            .header(TOKEN_HEADER_KEY, "Bearer $token")
            .build()
        return chain.proceed(authenticatedRequest)
    }

    private fun isAuthRequest(url: String): Boolean {
        return url.contains("/auth/") ||
                url.contains("/login") ||
                url.contains("/refresh-token")
    }
    companion object {
        const val TOKEN_HEADER_KEY = "Authorization"
    }
}