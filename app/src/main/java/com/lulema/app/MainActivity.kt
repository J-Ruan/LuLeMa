package com.lulema.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lulema.app.ui.ArchiveApp

class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  installSplashScreen()
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  setContent { val vm:ArchiveViewModel=viewModel(); ArchiveApp(vm) }
 }
}
