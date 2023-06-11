package com.iggydev.quicksignwizard.presentation.composables

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iggydev.quicksignwizard.presentation.BottomBarItems

@Composable
fun WizardBottomBar(navigationController: NavController) {

    val currentDestination by remember {
        mutableStateOf(navigationController.currentDestination)
    }

    val navigationBarItems = listOf(
        BottomBarItems.Generation,
        BottomBarItems.Scanner,
    )

    NavigationBar {
        navigationBarItems.forEach { barIcon ->
            val isSelected = currentDestination?.route == barIcon.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { navigationController.navigate(route = barIcon.route) },
                enabled = !isSelected,
                icon = {
                    Icon(
                        painter = painterResource(id = barIcon.icon),
                        contentDescription = "icon",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(40.dp)
                    )
                }, label = {
                    Text(text = barIcon.label)
                })
        }
    }
}