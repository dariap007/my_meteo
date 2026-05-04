package ru.student.mymeteo.domain;

public class FactorStatus {
    public final String tag;
    public final String title;
    public final String value;
    public final RiskLevel level;
    public final String explanation;

    public FactorStatus(String tag, String title, String value, RiskLevel level, String explanation) {
        this.tag = tag;
        this.title = title;
        this.value = value;
        this.level = level;
        this.explanation = explanation;
    }
}
