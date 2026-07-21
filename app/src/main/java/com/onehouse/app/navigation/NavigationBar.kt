package com.onehouse.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.TextoSecundario

enum class OneHouseSection(
    val title: String,
    val symbol: String
) {
    HOME("Inicio", "⌂"),
    ROOMS("Estancias", "▦"),
    WEATHER("Tiempo", "☁"),
    CONSUMPTION("Consumos", "▥"),
    MORE("Más", "•••")
}

@Composable
fun OneHouseNavigationBar(
    selectedSection: OneHouseSection,
    onSectionSelected: (OneHouseSection) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF071521), Color(0xFF040C14))
                )
            ),
        containerColor = Color.Transparent,
        contentColor = AzulClaro,
        tonalElevation = 0.dp
    ) {
        OneHouseSection.entries.forEach { section ->
            val selected = selectedSection == section
            val iconContainerSize by animateDpAsState(
                targetValue = if (selected) 42.dp else 34.dp,
                animationSpec = tween(220),
                label = "navIconContainerSize"
            )
            val iconColor by animateColorAsState(
                targetValue = if (selected) AzulClaro else TextoSecundario,
                animationSpec = tween(220),
                label = "navIconColor"
            )

            NavigationBarItem(
                selected = selected,
                onClick = { onSectionSelected(section) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(iconContainerSize)
                            .background(
                                brush = if (selected) {
                                    Brush.linearGradient(
                                        listOf(
                                            AzulClaro.copy(alpha = 0.24f),
                                            Color(0xFF4C7DFF).copy(alpha = 0.16f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                },
                                shape = RoundedCornerShape(15.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = section.symbol,
                            color = iconColor,
                            fontSize = if (section == OneHouseSection.MORE) 17.sp else 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                label = {
                    Text(
                        text = section.title,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AzulClaro,
                    selectedTextColor = AzulClaro,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = TextoSecundario,
                    unselectedTextColor = TextoSecundario
                )
            )
        }
    }
}
