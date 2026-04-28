package ru.student.mymeteo.data;

import org.json.JSONArray;

import ru.student.mymeteo.domain.GeomagneticSnapshot;

public class SpaceWeatherApiClient {
    private static final String NOAA_KP_FORECAST =
            "https://services.swpc.noaa.gov/products/noaa-planetary-k-index-forecast.json";

    private final HttpJsonClient client = new HttpJsonClient();

    public GeomagneticSnapshot fetch() throws Exception {
        JSONArray root = new JSONArray(client.get(NOAA_KP_FORECAST));
        double maxKp = 0;
        for (int rowIndex = 1; rowIndex < root.length(); rowIndex++) {
            JSONArray row = root.optJSONArray(rowIndex);
            if (row == null) {
                continue;
            }
            for (int column = 0; column < row.length(); column++) {
                Object value = row.opt(column);
                double numeric = parseNumber(value);
                if (numeric >= 0 && numeric <= 9) {
                    maxKp = Math.max(maxKp, numeric);
                }
            }
        }
        return new GeomagneticSnapshot(maxKp, "NOAA SWPC");
    }

    private double parseNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).replace("Kp=", "").trim());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }
}
