package com.iggydev.quicksignwizard.presentation

sealed class Screens(val route: String) {
    object GenerationScreen : Screens(route = "generation_screen")
    object ScannerScreen : Screens(route = "scanner_screen")
    object ListScreen : Screens(route = "list_screen")
}
