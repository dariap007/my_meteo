package ru.student.mymeteo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RiskForecast {
    public final RiskLevel level;
    public final int score;
    public final List<String> reasons;
    public final List<String> recommendations;

    public RiskForecast(RiskLevel level, int score, List<String> reasons, List<String> recommendations) {
        this.level = level;
        this.score = score;
        this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
        this.recommendations = Collections.unmodifiableList(new ArrayList<>(recommendations));
    }
}
