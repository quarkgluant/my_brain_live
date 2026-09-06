package com.example.mybrainlive.ui

import androidx.compose.runtime.Composable
import com.example.mybrainlive.bluetooth.PluxViewModel

@Composable
fun PluxDashboardScreen(viewModel: PluxViewModel) {
    EegDashboardScreen(viewModel = viewModel)
}
