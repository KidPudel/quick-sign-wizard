package com.iggydev.quicksignwizard.presentation

import com.iggydev.quicksignwizard.R

sealed class BottomBarItems(val route: String, val icon: Int, val label: String) {
    object Generation : BottomBarItems(route = "generation_screen", R.drawable.generation_wand, label = "generation")
    object Scanner : BottomBarItems(route = "scanner_screen", R.drawable.qr_scanner, label = "scanner")
}
