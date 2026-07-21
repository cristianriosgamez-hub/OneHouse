package com.onehouse.app.feature.weather

data class WeatherMetric(
    val title: String,
    val value: String,
    val status: String,
    val symbol: String
)

data class DailyForecast(
    val day: String,
    val symbol: String,
    val high: String,
    val low: String,
    val precipitation: String
)

data class WeatherUiState(
    val location: String = "L'Hospitalet de Llobregat",
    val updatedAt: String = "09:41",
    val temperature: String = "26.4°C",
    val condition: String = "Soleado",
    val feelsLike: String = "27.8°C",
    val high: String = "29.6°",
    val low: String = "18.7°",
    val indoorTemperature: String = "23.5°C",
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("Humedad", "61%", "Moderada", "◉"),
        WeatherMetric("Precipitación", "0.0 mm", "Hoy", "☂"),
        WeatherMetric("Viento", "14 km/h", "Oeste (O)", "≋"),
        WeatherMetric("Calidad del aire", "32", "Buena", "◌"),
        WeatherMetric("Índice UV", "6", "Alto", "☀"),
        WeatherMetric("Visibilidad", "16 km", "Excelente", "◉")
    ),
    val forecast: List<DailyForecast> = listOf(
        DailyForecast("Hoy", "☀", "29°", "18°", "0%"),
        DailyForecast("Mañana", "⛅", "28°", "17°", "10%"),
        DailyForecast("Sábado", "☂", "24°", "16°", "60%"),
        DailyForecast("Domingo", "🌤", "26°", "16°", "20%"),
        DailyForecast("Lunes", "☀", "27°", "17°", "5%")
    )
)
