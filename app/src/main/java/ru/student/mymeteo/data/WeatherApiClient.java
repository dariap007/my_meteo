package ru.student.mymeteo.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Locale;

import ru.student.mymeteo.domain.WeatherSnapshot;

public class WeatherApiClient {
    private final HttpJsonClient client = new HttpJsonClient();

    public WeatherSnapshot fetch(double latitude, double longitude) throws Exception {
        String url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&hourly=%s&forecast_days=3&timezone=auto",
                latitude,
                longitude,
                URLEncoder.encode("temperature_2m,relative_humidity_2m,pressure_msl", "UTF-8")
        );
        JSONObject root = new JSONObject(client.get(url));
        JSONObject hourly = root.getJSONObject("hourly");
        JSONArray temperatures = hourly.getJSONArray("temperature_2m");
        JSONArray humidity = hourly.getJSONArray("relative_humidity_2m");
        JSONArray pressure = hourly.getJSONArray("pressure_msl");

        int window = Math.min(24, temperatures.length());
        double minTemp = Double.MAX_VALUE;
        double maxTemp = -Double.MAX_VALUE;
        for (int index = 0; index < window; index++) {
            double value = temperatures.optDouble(index, temperatures.optDouble(0));
            minTemp = Math.min(minTemp, value);
            maxTemp = Math.max(maxTemp, value);
        }

        int pressureIndex = Math.min(23, pressure.length() - 1);
        double currentPressure = pressure.optDouble(0, 0);
        double pressureDelta = pressure.optDouble(pressureIndex, currentPressure) - currentPressure;

        return new WeatherSnapshot(
                temperatures.optDouble(0, 0),
                maxTemp - minTemp,
                humidity.optDouble(0, 0),
                currentPressure,
                pressureDelta,
                "Open-Meteo"
        );
    }
}
