package com.iggydev.quicksignwizard.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iggydev.quicksignwizard.presentation.composables.GenerationScreen
import com.iggydev.quicksignwizard.ui.theme.QuickSignWizardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickSignWizardTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO: basic navigation
                    // TODO: generate digital signature
                    // TODO: generate qr code
                    // TODO: attach signature
                    // TODO: scan qr code
                    val navigationController = rememberNavController()
                    NavHost(navController = navigationController, startDestination = Screens.GenerationScreen.route) {
                        composable(route = Screens.GenerationScreen.route) {
                            GenerationScreen(navigationController = navigationController)
                        }
                        composable(route = Screens.ListScreen.route) {
                            GenerationScreen(navigationController = navigationController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    QuickSignWizardTheme {
        Greeting("Android")
    }
}