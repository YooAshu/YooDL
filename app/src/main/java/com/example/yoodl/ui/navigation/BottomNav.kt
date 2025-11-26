package com.example.yoodl.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.yoodl.ui.pages.homepage.glassCard

@Composable
fun BottomNav(navController: NavHostController, modifier: Modifier = Modifier) {

    NavigationBar(
        containerColor = Color(0xFF090228),
        modifier = modifier
            .height(70.dp)
            .width(140.dp)
            .padding(5.dp)
            .clip(RoundedCornerShape(100))
            .glassCard(),
        windowInsets = WindowInsets(0)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()

        val currentRoute = navBackStackEntry?.destination?.route
        BottomNavItems.forEach { navItem ->
            NavigationBarItem(
                selected = currentRoute == navItem.route,

                onClick = {
                    navController.navigate(navItem.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label,
                        modifier = Modifier
                            .size(36.dp)
                    )
                },
                alwaysShowLabel = false,

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color.Transparent,

                ),
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (currentRoute == navItem.route) Color.White
                        else Color.Transparent
                    )

            )
        }

    }
}

val BottomNavItems : List<BottomNavItem> = listOf(
    BottomNavItem(
        label = "Home",
        icon = Icons.Default.Home,
        route = Routes.HOME
    ),
    BottomNavItem(
        label = "Downloads",
        icon = Icons.Default.KeyboardArrowDown,
        route = Routes.DOWNLOADS
    )
)


data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)