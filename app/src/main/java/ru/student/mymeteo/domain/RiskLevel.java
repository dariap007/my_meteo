package ru.student.mymeteo.domain;

public enum RiskLevel {
    LOW("Спокойный день", "#6FAF7A"),
    MEDIUM("Будьте внимательны", "#D9A441"),
    HIGH("Высокий метеориск", "#C85C5C");

    public final String title;
    public final String colorHex;

    RiskLevel(String title, String colorHex) {
        this.title = title;
        this.colorHex = colorHex;
    }
}
