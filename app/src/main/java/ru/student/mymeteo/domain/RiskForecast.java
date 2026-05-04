package ru.student.mymeteo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RiskForecast {
    public final RiskLevel level;
    public final int score;
    public final List<String> reasons;
    public final List<String> recommendations;
    public final List<String> tags;
    public final List<FactorStatus> factors;
    public final int[] trend;
    public final String[] trendNotes;
    public final String notificationText;

    public RiskForecast(
            RiskLevel level,
            int score,
            List<String> reasons,
            List<String> recommendations,
            List<String> tags,
            List<FactorStatus> factors,
            int[] trend,
            String[] trendNotes,
            String notificationText
    ) {
        this.level = level;
        this.score = score;
        this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
        this.recommendations = Collections.unmodifiableList(new ArrayList<>(recommendations));
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.factors = Collections.unmodifiableList(new ArrayList<>(factors));
        this.trend = trend.clone();
        this.trendNotes = trendNotes.clone();
        this.notificationText = notificationText;
    }
}
