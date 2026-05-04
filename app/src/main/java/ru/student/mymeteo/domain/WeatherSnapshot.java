package ru.student.mymeteo.domain;

public class WeatherSnapshot {
    public final double currentTemperature;
    public final double temperatureDelta24h;
    public final double tomorrowTemperatureDelta;
    public final double currentHumidity;
    public final double currentPressure;
    public final double pressureDelta24h;
    public final double tomorrowPressureDelta;
    public final String[] trendLabels;
    public final double[] trendTemperatures;
    public final double[] trendPressures;
    public final double[] trendHumidity;
    public final String source;

    public WeatherSnapshot(
            double currentTemperature,
            double temperatureDelta24h,
            double tomorrowTemperatureDelta,
            double currentHumidity,
            double currentPressure,
            double pressureDelta24h,
            double tomorrowPressureDelta,
            String[] trendLabels,
            double[] trendTemperatures,
            double[] trendPressures,
            double[] trendHumidity,
            String source
    ) {
        this.currentTemperature = currentTemperature;
        this.temperatureDelta24h = temperatureDelta24h;
        this.tomorrowTemperatureDelta = tomorrowTemperatureDelta;
        this.currentHumidity = currentHumidity;
        this.currentPressure = currentPressure;
        this.pressureDelta24h = pressureDelta24h;
        this.tomorrowPressureDelta = tomorrowPressureDelta;
        this.trendLabels = trendLabels.clone();
        this.trendTemperatures = trendTemperatures.clone();
        this.trendPressures = trendPressures.clone();
        this.trendHumidity = trendHumidity.clone();
        this.source = source;
    }

    public static WeatherSnapshot demo() {
        return new WeatherSnapshot(
                6.0,
                8.5,
                5.0,
                82.0,
                753.0,
                6.8,
                3.0,
                new String[]{"сейч.", "+6ч", "завтр.", "+2"},
                new double[]{6.0, 9.3, 12.1, 8.4},
                new double[]{753.0, 756.4, 759.8, 754.0},
                new double[]{82.0, 76.0, 70.0, 79.0},
                "Демо-данные"
        );
    }
}
