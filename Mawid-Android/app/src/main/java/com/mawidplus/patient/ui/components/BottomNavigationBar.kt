package com.mawidplus.patient.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mawidplus.patient.R
import com.mawidplus.patient.ui.navigation.Routes
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest

private data class BottomNavEntry(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
)

private val bottomNavEntries = listOf(
    BottomNavEntry(Routes.HOME, Icons.Filled.Home, R.string.nav_home),
    BottomNavEntry(Routes.SEARCH, Icons.Filled.Search, R.string.nav_search),
    BottomNavEntry(Routes.APPOINTMENTS, Icons.Filled.CalendarToday, R.string.nav_appointments),
    BottomNavEntry(Routes.PROFILE, Icons.Filled.Person, R.string.nav_profile)
)

@Composable
fun BottomNavigationBar(
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        containerColor = SurfaceContainerLowest.copy(alpha = 0.92f),
        tonalElevation = 0.dp
    ) {
        bottomNavEntries.forEach { entry ->
            val isSelected = selectedRoute == entry.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(entry.route) },
                icon = {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = stringResource(entry.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(entry.labelRes),
                        fontFamily = PublicSans,
                        color = if (isSelected) Primary else OnSurface.copy(alpha = 0.6f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    unselectedIconColor = OnSurface.copy(alpha = 0.6f),
                    selectedTextColor = Primary,
                    unselectedTextColor = OnSurface.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
