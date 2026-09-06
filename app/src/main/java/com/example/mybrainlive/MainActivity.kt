package com.example.mybrainlive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.mybrainlive.bluetooth.EegViewModel
import com.example.mybrainlive.ui.EegDashboardScreen
import com.example.mybrainlive.ui.theme.MyBrainLiveTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EegViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyBrainLiveTheme {
                EegDashboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}
