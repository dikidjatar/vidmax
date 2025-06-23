package com.djatar.vidmax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.djatar.vidmax.ui.AppEntry
import com.djatar.vidmax.ui.theme.VidMaxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        VidMaxApp.context = this.baseContext

        setContent {
            VidMaxTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    AppEntry(paddingValues = innerPadding)
                }
            }
        }
    }
}