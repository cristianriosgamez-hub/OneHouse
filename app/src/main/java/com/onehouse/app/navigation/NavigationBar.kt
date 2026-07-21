package com.onehouse.app.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.TextoSecundario

enum class OneHouseSection(
    val title: String,
    val symbol: String
) {
    HOME(
        title = "Inicio",
        symbol = "⌂"
    ),
    ROOMS(
        title = "Estancias",
        symbol = "▦"
    ),
    WEATHER(
        title = "Tiempo",
        symbol = "☁"
    ),
    CONSUMPTION(
        title = "Consumos",
        symbol = "▥"
    ),
    MORE(
        title = "Más",
        symbol = "•••"
    )
}

@Composable
fun OneHouseNavigationBar(
    selectedSection: OneHouseSection,
    onSectionSelected: (OneHouseSection) -> Unit
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color(0xFF06111D),
        contentColor = AzulClaro,
        tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
    ) {
        OneHouseSection.entries.forEach { section ->

            val selected = selectedSection == section

            NavigationBarItem(
                selected = selected,
                onClick = {
                    onSectionSelected(section)
                },
                icon = {
                    Text(
                        text = section.symbol,
                        fontSize = if (section == OneHouseSection.MORE) {
                            18.sp
                        } else {
                            23.sp
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                label = {
                    Text(
                        text = section.title,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AzulClaro,
                    selectedTextColor = AzulClaro,
                    indicatorColor = BordeTarjeta,
                    unselectedIconColor = TextoSecundario,
                    unselectedTextColor = TextoSecundario
                )
            )
        }
    }
}