package ru.student.mymeteo.data;

import ru.student.mymeteo.domain.ForecastBundle;
import ru.student.mymeteo.domain.GeomagneticSnapshot;
import ru.student.mymeteo.domain.RiskEngine;
import ru.student.mymeteo.domain.UserProfile;
import ru.student.mymeteo.domain.WeatherSnapshot;

public class ForecastRepository {
    private final WeatherApiClient weatherApi = new WeatherApiClient();
    private final SpaceWeatherApiClient spaceWeatherApi = new SpaceWeatherApiClient();
    private final RiskEngine riskEngine = new RiskEngine();

    public ForecastBundle load(UserProfile profile) {
        WeatherSnapshot weather;
        GeomagneticSnapshot geomagnetic;

        try {
            weather = weatherApi.fetch(profile.latitude, profile.longitude);
        } catch (Exception ignored) {
            weather = WeatherSnapshot.demo();
        }

        try {
            geomagnetic = spaceWeatherApi.fetch();
        } catch (Exception ignored) {
            geomagnetic = GeomagneticSnapshot.demo();
        }

        return new ForecastBundle(weather, geomagnetic, riskEngine.calculate(weather, geomagnetic, profile));
    }
}
