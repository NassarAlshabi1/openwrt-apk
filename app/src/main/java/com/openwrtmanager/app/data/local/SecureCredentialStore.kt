package com.openwrtmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用 Android Keystore 加密保存每个 RouterProfile 对应的密码。
 * Key 用 profile.id，避免密码和非敏感元数据混在一起存储。
 */
@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_router_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(profileId: String, password: String) {
        prefs.edit().putString(profileId, password).apply()
    }

    fun getPassword(profileId: String): String? = prefs.getString(profileId, null)

    fun deletePassword(profileId: String) {
        prefs.edit().remove(profileId).apply()
    }
}
