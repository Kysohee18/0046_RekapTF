package id.ac.rekaptf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.ac.rekaptf.navigation.RekapApp
import id.ac.rekaptf.ui.components.theme.RekapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RekapTheme {
                RekapApp()
            }
        }
    }
}
