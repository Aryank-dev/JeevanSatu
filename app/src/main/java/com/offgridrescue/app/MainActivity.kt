package com.offgridrescue.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.offgridrescue.app.ui.navigation.JeevanSetuApp
import com.offgridrescue.app.ui.theme.JeevanSetuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JeevanSetuTheme {
                JeevanSetuApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
