package ru.student.mymeteo.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Locale;

import ru.student.mymeteo.domain.WeatherSnapshot;

public class WeatherApiClient {
    private static final double HPA_TO_MMHG = 0.750061683;
    private final HttpJsonClient client = new HttpJsonClient();

    public WeatherSnapshot fetch(double latitude, double longitude) throws Exception {
        String url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=%s&hourly=%s&forecast_days=3&timezone=auto",
                latitude,
                longitude,
                URLEncoder.encode("temperature_2m,relative_humidity_2m,pressure_msl", "UTF-8"),
                URLEncoder.encode("temperature_2m,relative_humidity_2m,pressure_msl", "UTF-8")
        );
        JSONObject root = new JSONObject(client.get(url));
        JSONObject current = root.getJSONObject("current");
        JSONObject hourly = root.getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temperatures = hourly.getJSONArray("temperature_2m");
        JSONArray humidity = hourly.getJSONArray("relative_humidity_2m");
        JSONArray pressure = hourly.getJSONArray("pressure_msl");
        int nowIndex = findIndexByTime(times, current.optString("time"));
        double currentTemperature = current.optDouble("temperature_2m", temperatures.optDouble(nowIndex, 0));
        double currentHumidity = current.optDouble("relative_humidity_2m", humidity.optDouble(nowIndex, 0));
        double currentPressureHpa = current.optDouble("pressure_msl", pressure.optDouble(nowIndex, 0));

        int window = Math.min(24, temperatures.length() - nowIndex);
        double minTemp = Double.MAX_VALUE;
        double maxTemp = -Double.MAX_VALUE;
        for (int index = 0; index < window; index++) {
            double value = temperatures.optDouble(nowIndex + index, temperatures.optDouble(nowIndex));
            minTemp = Math.min(minTemp, value);
            maxTemp = Math.max(maxTemp, value);
        }

        int pressureIndex = Math.min(nowIndex + 23, pressure.length() - 1);
        double pressureDeltaHpa = pressure.optDouble(pressureIndex, currentPressureHpa) - currentPressureHpa;
        double tomorrowTemperatureDelta = dayDelta(temperatures, nowIndex + 24, nowIndex + 48);
        double tomorrowPressureDeltaHpa = pressure.optDouble(Math.min(nowIndex + 47, pressure.length() - 1), currentPressureHpa)
                - pressure.optDouble(Math.min(nowIndex + 24, pressure.length() - 1), currentPressureHpa);

        int[] trendIndexes = new int[]{
                nowIndex,
                Math.min(nowIndex + 6, temperatures.length() - 1),
                Math.min(nowIndex + 24, temperatures.length() - 1),
                Math.min(nowIndex + 48, temperatures.length() - 1)
        };

        return new WeatherSnapshot(
                currentTemperature,
                maxTemp - minTemp,
                tomorrowTemperatureDelta,
                currentHumidity,
                toMmHg(currentPressureHpa),
                toMmHg(pressureDeltaHpa),
                toMmHg(tomorrowPressureDeltaHpa),
                new String[]{"сейчас", "+6 ч", "завтра", "+2 дня"},
                trendValues(temperatures, trendIndexes, false, currentTemperature),
                trendValues(pressure, trendIndexes, true, currentPressureHpa),
                trendValues(humidity, trendIndexes, false, currentHumidity),
                "Open-Meteo"
        );
    }

    private double toMmHg(double hpa) {
        return hpa * HPA_TO_MMHG;
    }

    private double dayDelta(JSONArray values, int start, int end) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int safeEnd = Math.min(end, values.length());
        for (int index = Math.min(start, values.length() - 1); index < safeEnd; index++) {
            double value = values.optDouble(index, values.optDouble(0));
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (min == Double.MAX_VALUE || max == -Double.MAX_VALUE) {
            return 0;
        }
        return max - min;
    }

    private int findIndexByTime(JSONArray times, String targetTime) {
        for (int index = 0; index < times.length(); index++) {
            if (targetTime.equals(times.optString(index))) {
                return index;
            }
        }
        return 0;
    }

    private double[] trendValues(JSONArray values, int[] indexes, boolean pressureValues, double currentValue) {
        double[] result = new double[indexes.length];
        for (int index = 0; index < indexes.length; index++) {
            double value = index == 0 ? currentValue : values.optDouble(indexes[index], values.optDouble(0));
            result[index] = pressureValues ? toMmHg(value) : value;
        }
        return result;
    }
}
