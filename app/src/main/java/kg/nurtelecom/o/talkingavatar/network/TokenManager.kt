package kg.nurtelecom.o.talkingavatar.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

class TokenManager(
    private val context: Context
) : KoinComponent {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { p ->
            p[ACCESS_TOKEN_KEY] = accessToken
            p[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun updateTokens(
        accessToken: String? = null,
        refreshToken: String? = null,
        removeIfNull: Boolean = false
    ) {
        context.dataStore.edit { p ->
            if (accessToken != null) {
                p[ACCESS_TOKEN_KEY] = accessToken
            } else if (removeIfNull) {
                p.remove(ACCESS_TOKEN_KEY)
            }

            if (refreshToken != null) {
                p[REFRESH_TOKEN_KEY] = refreshToken
            } else if (removeIfNull) {
                p.remove(REFRESH_TOKEN_KEY)
            }
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()

    suspend fun clearTokens() {
        context.dataStore.edit { p ->
            p.remove(ACCESS_TOKEN_KEY)
            p.remove(REFRESH_TOKEN_KEY)
        }
    }

    fun getAccessTokenFlow(): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }

    fun getRefreshTokenFlow(): Flow<String?> =
        context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }
}
