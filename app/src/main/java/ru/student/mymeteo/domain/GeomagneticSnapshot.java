package ru.student.mymeteo.domain;

public class GeomagneticSnapshot {
    public final double maxKp;
    public final String source;

    public GeomagneticSnapshot(double maxKp, String source) {
        this.maxKp = maxKp;
        this.source = source;
    }

    public static GeomagneticSnapshot demo() {
        return new GeomagneticSnapshot(4.0, "Демо-данные");
    }
}
