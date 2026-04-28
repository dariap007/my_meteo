package ru.student.mymeteo.domain;

public class WeatherSnapshot {
    public final double currentTemperature;
    public final double temperatureDelta24h;
    public final double currentHumidity;
    public final double currentPressure;
    public final double pressureDelta24h;
    public final String source;

    public WeatherSnapshot(
            double currentTemperature,
            double temperatureDelta24h,
            double currentHumidity,
            double currentPressure,
            double pressureDelta24h,
            String source
    ) {
        this.currentTemperature = currentTemperature;
        this.temperatureDelta24h = temperatureDelta24h;
        this.currentHumidity = currentHumidity;
        this.currentPressure = currentPressure;
        this.pressureDelta24h = pressureDelta24h;
        this.source = source;
    }

    public static WeatherSnapshot demo() {
        return new WeatherSnapshot(6.0, 8.5, 82.0, 1004.0, 9.0, "Демо-данные");
    }
}
