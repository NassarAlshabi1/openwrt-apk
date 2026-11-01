package com.openwrtmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.openwrtmanager.app.data.remote.UbusClientManager
import com.openwrtmanager.app.ui.AppNavHost
import com.openwrtmanager.app.ui.common.LanguageManager
import com.openwrtmanager.app.ui.theme.OpenWrtManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ubusClientManager: UbusClientManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.applySavedLanguage(this)
        enableEdgeToEdge()
        setContent {
            OpenWrtManagerTheme {
                AppNavHost(ubusClientManager = ubusClientManager)
            }
        }
    }
}
