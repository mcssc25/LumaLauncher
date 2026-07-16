package com.lumalauncher.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherRepositoryTest {
    @Test
    fun mapsCommonWeatherCodes() {
        assertEquals("Clear", WeatherRepository.weatherCondition(0).first)
        assertEquals("Rain", WeatherRepository.weatherCondition(63).first)
        assertEquals("Snow", WeatherRepository.weatherCondition(75).first)
        assertEquals("Thunderstorms", WeatherRepository.weatherCondition(95).first)
    }
}
