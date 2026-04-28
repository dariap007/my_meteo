package ru.student.mymeteo.domain;

import java.util.ArrayList;
import java.util.List;

public class RiskEngine {
    public RiskForecast calculate(WeatherSnapshot weather, GeomagneticSnapshot geomagnetic, UserProfile profile) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (Math.abs(weather.temperatureDelta24h) >= 10) {
            score += 28;
            reasons.add("Температура меняется более чем на 10 градусов за сутки.");
            recommendations.add("Запланируйте более спокойный режим и избегайте резкой смены температуры.");
        } else if (Math.abs(weather.temperatureDelta24h) >= 7) {
            score += 20;
            reasons.add("Ожидается заметный перепад температуры.");
            recommendations.add("Одевайтесь слоями и держите под рукой воду.");
        }

        if (Math.abs(weather.pressureDelta24h) >= 10) {
            score += 26;
            reasons.add("Есть выраженный скачок атмосферного давления.");
            recommendations.add("Снизьте физические нагрузки и следите за давлением, если врач рекомендовал контроль.");
        } else if (Math.abs(weather.pressureDelta24h) >= 6) {
            score += 18;
            reasons.add("Атмосферное давление меняется быстрее обычного.");
            recommendations.add("Старайтесь не перегружать день делами.");
        }

        if (weather.currentHumidity >= 80 || weather.currentHumidity <= 25) {
            score += 12;
            reasons.add("Влажность выходит за комфортный диапазон.");
            recommendations.add("Проветривайте помещение и поддерживайте комфортный питьевой режим.");
        }

        if (geomagnetic.maxKp >= 6) {
            score += 24;
            reasons.add("Прогнозируется сильная геомагнитная активность.");
            recommendations.add("При чувствительности к магнитным бурям заранее планируйте отдых.");
        } else if (geomagnetic.maxKp >= 5) {
            score += 18;
            reasons.add("Возможна магнитная буря уровня G1.");
            recommendations.add("Отложите необязательные нагрузки, если обычно реагируете на магнитные бури.");
        } else if (geomagnetic.maxKp >= 4) {
            score += 8;
            reasons.add("Геомагнитная активность умеренно повышена.");
        }

        score += profile.riskWeightBonus();
        if (profile.hypertension || profile.hypotension) {
            reasons.add("Профиль указывает чувствительность к изменениям давления.");
        }
        if (profile.migraine) {
            recommendations.add("При склонности к мигреням держите день предсказуемым: сон, вода, меньше экранной нагрузки.");
        }
        if (profile.joints) {
            recommendations.add("При боли в суставах избегайте переохлаждения и резких нагрузок.");
        }

        RiskLevel level = RiskLevel.LOW;
        if (score >= 58) {
            level = RiskLevel.HIGH;
        } else if (score >= 30) {
            level = RiskLevel.MEDIUM;
        }

        if (reasons.isEmpty()) {
            reasons.add("Критичных погодных изменений на ближайшие сутки не видно.");
        }
        recommendations.add("Советы не заменяют консультацию врача. Лекарства принимайте только по назначению специалиста.");

        return new RiskForecast(level, Math.min(score, 100), reasons, recommendations);
    }
}
