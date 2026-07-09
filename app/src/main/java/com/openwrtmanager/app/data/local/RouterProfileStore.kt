package com.openwrtmanager.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openwrtmanager.app.data.model.RouterProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "router_profiles")
private val PROFILES_KEY = stringPreferencesKey("profiles_json")

/**
 * 保存"多个路由器连接配置"的列表（不含密码，密码在 SecureCredentialStore）。
 * 用一个 JSON 数组整体存取，profile 数量不会很大（用户不会加几百台路由器），
 * 没必要上 Room。
 */
@Singleton
class RouterProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    val profiles: Flow<List<RouterProfile>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PROFILES_KEY] ?: return@map emptyList()
        runCatching {
            json.decodeFromString<List<RouterProfile>>(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun getAll(): List<RouterProfile> = profiles.first()

    suspend fun upsert(profile: RouterProfile) {
        val current = getAll().toMutableList()
        val idx = current.indexOfFirst { it.id == profile.id }
        if (idx >= 0) current[idx] = profile else current.add(profile)
        persist(current)
    }

    suspend fun delete(profileId: String) {
        val current = getAll().filterNot { it.id == profileId }
        persist(current)
    }

    private suspend fun persist(list: List<RouterProfile>) {
        context.dataStore.edit { prefs ->
            prefs[PROFILES_KEY] = json.encodeToString(list)
        }
    }
}
