package com.onehouse.app.feature.weather

interface WeatherRepository {
    suspend fun loadWeather(): WeatherUiState
}

class DemoWeatherRepository : WeatherRepository {
    override suspend fun loadWeather(): WeatherUiState = WeatherUiState()
}
