package com.onehouse.app.feature.terrace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.R
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.feature.rooms.detail.RoomHeader
import com.onehouse.app.feature.weather.WeatherUiState
import com.onehouse.app.feature.weather.rememberWeatherState

private val TerraceBlue = Color(0xFF168EFF)
private val TerraceGreen = Color(0xFF55C865)
private val TerraceCard = Color(0xE60A1926)

@Composable
fun TerraceScreen(onBack: () -> Unit) {
    var lightOn by remember { mutableStateOf(true) }
    val weather = rememberWeatherState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            RoomHeader(title = "Terraza", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            TerraceHeroImage()
            Spacer(Modifier.height(16.dp))
            WeatherCard(weather)
            Spacer(Modifier.height(16.dp))
            ExteriorLightCard(lightOn = lightOn, onChange = { lightOn = it })
            Spacer(Modifier.height(64.dp))
        }
    }
}


@Composable
private fun TerraceHeroImage() {
    Image(
        painter = painterResource(R.drawable.room_terrace),
        contentDescription = "Terraza exterior",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, BordeTarjeta, RoundedCornerShape(26.dp))
    )
}

@Composable
private fun WeatherCard(weather: WeatherUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(TerraceCard)
            .border(1.dp, BordeTarjeta, RoundedCornerShape(26.dp))
            .padding(22.dp)
    ) {
        Text("Clima exterior", color = TextoPrincipal, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        Text("Actualizado ${weather.updatedAt}", color = TextoSecundario, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(weather.temperature, color = TextoPrincipal, fontSize = 58.sp, fontWeight = FontWeight.Light)
                Text(weather.condition, color = TextoPrincipal, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text(weather.location, color = TextoSecundario, fontSize = 14.sp)
            }
            Text(weather.conditionSymbol, color = Color(0xFFFFC329), fontSize = 82.sp)
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(BordeTarjeta))
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            WeatherMetric("♨", "Sensación", weather.feelsLike)
            WeatherMetric("◉", "Humedad", weather.humidityPercent?.let { "$it%" } ?: "--")
            WeatherMetric("≋", "Viento", weather.windSpeedKmh?.let { "%.0f km/h".format(it) } ?: "--")
        }
    }
}

@Composable
private fun WeatherMetric(symbol: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(symbol, color = TextoPrincipal, fontSize = 23.sp)
        Text(label, color = TextoSecundario, fontSize = 11.sp)
        Text(value, color = TextoPrincipal, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExteriorLightCard(lightOn: Boolean, onChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TerraceCard)
            .border(1.dp, BordeTarjeta, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text("Luz exterior", color = TextoPrincipal, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0x1A55C865), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) { Text("♧", color = Color(0xFFFFC04A), fontSize = 34.sp) }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Luz exterior", color = TextoPrincipal, fontSize = 17.sp)
                Text(if (lightOn) "Encendida" else "Apagada", color = if (lightOn) TerraceGreen else TextoSecundario, fontSize = 14.sp)
            }
            Switch(
                checked = lightOn,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TerraceGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF183041)
                )
            )
        }
    }
}
