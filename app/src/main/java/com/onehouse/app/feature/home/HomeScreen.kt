package com.onehouse.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AmarilloEstado
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.OneHouseHeader
import com.onehouse.app.design.OneHouseInfoCard
import com.onehouse.app.design.OneHouseSceneCard
import com.onehouse.app.design.OneHouseSectionTitle
import com.onehouse.app.design.OneHouseStatusItem
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.design.VerdeEstado
import com.onehouse.app.feature.rooms.RoomItem

@Composable
fun HomeScreen(
    favoriteRooms: List<RoomItem>,
    onFavoriteSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(FondoSuperior, FondoInferior, Color.Black)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 24.dp)
    ) {
        OneHouseHeader(title = "OneHouse", subtitle = "Buenos días", badgeText = null)

        Spacer(modifier = Modifier.height(18.dp))

        OneHouseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Climatización central", color = TextoSecundario, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "22,5 °C",
                            color = TextoPrincipal,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OneHouseCard {
                        Text(
                            text = "❄  Frío",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            color = AzulClaro,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = BordeTarjeta)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OneHouseStatusItem(
                        title = "Estado",
                        value = "Encendido",
                        valueColor = VerdeEstado,
                        modifier = Modifier.weight(1f)
                    )
                    OneHouseStatusItem(
                        title = "Consigna",
                        value = "23 °C",
                        modifier = Modifier.weight(1f)
                    )
                    OneHouseStatusItem(
                        title = "Consumo ACS",
                        value = "4,8 kW",
                        valueColor = AmarilloEstado,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OneHouseInfoCard(
                title = "Persianas",
                value = "2 de 4 abiertas",
                detail = "Estado general",
                symbol = "▥",
                modifier = Modifier.weight(1f).height(132.dp)
            )
            OneHouseInfoCard(
                title = "Terraza",
                value = "18 °C",
                detail = "Sin lluvia",
                symbol = "☁",
                modifier = Modifier.weight(1f).height(132.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OneHouseInfoCard(
                title = "Luces",
                value = "3 de 12 encendidas",
                detail = "Estado general",
                symbol = "☀",
                symbolColor = AmarilloEstado,
                modifier = Modifier.weight(1f).height(132.dp)
            )
            OneHouseInfoCard(
                title = "Seguridad",
                value = "Todo correcto",
                detail = "Estado general",
                symbol = "✓",
                valueColor = VerdeEstado,
                symbolColor = VerdeEstado,
                modifier = Modifier.weight(1f).height(132.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        OneHouseSectionTitle(title = "Estancias favoritas")
        Spacer(modifier = Modifier.height(12.dp))

        if (favoriteRooms.isEmpty()) {
            OneHouseCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Marca una estancia con ☆ para verla aquí.",
                    color = TextoSecundario,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            favoriteRooms.take(4).chunked(2).forEachIndexed { index, rowItems ->
                if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { room ->
                        OneHouseSceneCard(
                            name = room.name,
                            symbol = room.symbol,
                            modifier = Modifier.weight(1f),
                            onClick = { onFavoriteSelected(room.name) }
                        )
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Sistema conectado",
            color = VerdeEstado,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
