package ru.student.mymeteo.domain;

public class ForecastBundle {
    public final WeatherSnapshot weather;
    public final GeomagneticSnapshot geomagnetic;
    public final RiskForecast risk;

    public ForecastBundle(WeatherSnapshot weather, GeomagneticSnapshot geomagnetic, RiskForecast risk) {
        this.weather = weather;
        this.geomagnetic = geomagnetic;
        this.risk = risk;
    }
}
