package ru.student.mymeteo.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RiskEngine {
    public RiskForecast calculate(WeatherSnapshot weather, GeomagneticSnapshot geomagnetic, UserProfile profile) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        Set<String> tags = new LinkedHashSet<>();
        List<FactorStatus> factors = new ArrayList<>();

        RiskLevel temperatureLevel = factorLevel(Math.abs(weather.temperatureDelta24h), 7, 10);
        factors.add(new FactorStatus(
                "temperature",
                "Температура",
                round(weather.temperatureDelta24h) + " °C / 24 ч",
                temperatureLevel,
                "Резкие перепады температуры могут усиливать усталость и головную боль."
        ));

        if (Math.abs(weather.temperatureDelta24h) >= 10) {
            score += 28;
            tags.add("temperature");
            reasons.add("Температура меняется более чем на 10 градусов за сутки.");
            recommendations.add("temperature|Запланируйте спокойный режим и избегайте резкой смены температуры.");
        } else if (Math.abs(weather.temperatureDelta24h) >= 7) {
            score += 20;
            tags.add("temperature");
            reasons.add("Ожидается заметный перепад температуры.");
            recommendations.add("temperature|Одевайтесь слоями и держите под рукой воду.");
        }

        RiskLevel pressureLevel = factorLevel(Math.abs(weather.pressureDelta24h), 4.5, 7.5);
        factors.add(new FactorStatus(
                "pressure",
                "Давление",
                round(weather.pressureDelta24h) + " мм рт. ст. / 24 ч",
                pressureLevel,
                "Барометрическая пила часто является важным триггером метеочувствительности."
        ));

        if (Math.abs(weather.pressureDelta24h) >= 7.5) {
            score += 26;
            tags.add("pressure");
            reasons.add("Есть выраженный скачок атмосферного давления.");
            recommendations.add("pressure|Снизьте физические нагрузки и следите за давлением, если врач рекомендовал контроль.");
        } else if (Math.abs(weather.pressureDelta24h) >= 4.5) {
            score += 18;
            tags.add("pressure");
            reasons.add("Атмосферное давление меняется быстрее обычного.");
            recommendations.add("pressure|Старайтесь не перегружать день делами.");
        }

        RiskLevel humidityLevel = weather.currentHumidity >= 80 || weather.currentHumidity <= 25 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        factors.add(new FactorStatus(
                "humidity",
                "Влажность",
                round(weather.currentHumidity) + "%",
                humidityLevel,
                "Слишком высокая или низкая влажность может усиливать дискомфорт."
        ));

        if (weather.currentHumidity >= 80 || weather.currentHumidity <= 25) {
            score += 12;
            tags.add("humidity");
            reasons.add("Влажность выходит за комфортный диапазон.");
            recommendations.add("humidity|Проветривайте помещение и поддерживайте комфортный питьевой режим.");
        }

        RiskLevel kpLevel = geomagnetic.maxKp >= 5 ? RiskLevel.HIGH : geomagnetic.maxKp >= 4 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        factors.add(new FactorStatus(
                "geomagnetic",
                "Kp-индекс",
                round(geomagnetic.maxKp),
                kpLevel,
                "Kp 5 и выше соответствует магнитной буре уровня G1 и выше."
        ));

        if (geomagnetic.maxKp >= 6) {
            score += 24;
            tags.add("geomagnetic");
            reasons.add("Прогнозируется сильная геомагнитная активность.");
            recommendations.add("geomagnetic|При чувствительности к магнитным бурям заранее планируйте отдых.");
        } else if (geomagnetic.maxKp >= 5) {
            score += 18;
            tags.add("geomagnetic");
            reasons.add("Возможна магнитная буря уровня G1.");
            recommendations.add("geomagnetic|Отложите необязательные нагрузки, если обычно реагируете на магнитные бури.");
        } else if (geomagnetic.maxKp >= 4) {
            score += 8;
            tags.add("geomagnetic");
            reasons.add("Геомагнитная активность умеренно повышена.");
        }

        score += profile.riskWeightBonus();
        if (profile.hypertension || profile.hypotension) {
            tags.add("profile-pressure");
            reasons.add("Профиль указывает чувствительность к изменениям давления.");
        }
        if (profile.migraine) {
            tags.add("profile-migraine");
            recommendations.add("profile|При склонности к мигреням держите день предсказуемым: сон, вода, меньше экранной нагрузки.");
        }
        if (profile.joints) {
            tags.add("profile-joints");
            recommendations.add("profile|При боли в суставах избегайте переохлаждения и резких нагрузок.");
        }

        RiskLevel diaryLevel = profile.riskWeightBonus() >= 12 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        factors.add(new FactorStatus(
                "diary",
                "Дневник",
                profile.riskWeightBonus() > 0 ? "учтен профиль" : "наблюдений мало",
                diaryLevel,
                "Записи самочувствия помогают находить личные триггеры и уточнять прогноз."
        ));

        RiskLevel level = RiskLevel.LOW;
        if (score >= 58) {
            level = RiskLevel.HIGH;
        } else if (score >= 30) {
            level = RiskLevel.MEDIUM;
        }

        if (reasons.isEmpty()) {
            reasons.add("Критичных погодных изменений на ближайшие сутки не видно.");
        }
        recommendations.add("disclaimer|Советы носят информационный характер и не заменяют консультацию врача.");

        List<String> cleanRecommendations = deduplicateRecommendations(recommendations);
        int[] trend = buildTrend(score, weather, geomagnetic);
        String[] trendNotes = buildTrendNotes(weather, geomagnetic, trend);
        String notificationText = buildNotification(level, tags, reasons, weather, geomagnetic);

        return new RiskForecast(
                level,
                Math.min(score, 100),
                reasons,
                cleanRecommendations,
                new ArrayList<>(tags),
                factors,
                trend,
                trendNotes,
                notificationText
        );
    }

    private RiskLevel factorLevel(double value, double medium, double high) {
        if (value >= high) {
            return RiskLevel.HIGH;
        }
        if (value >= medium) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private List<String> deduplicateRecommendations(List<String> taggedRecommendations) {
        Set<String> unique = new LinkedHashSet<>();
        for (String recommendation : taggedRecommendations) {
            int separator = recommendation.indexOf('|');
            unique.add(separator >= 0 ? recommendation.substring(separator + 1) : recommendation);
        }
        return new ArrayList<>(unique);
    }

    private int[] buildTrend(int score, WeatherSnapshot weather, GeomagneticSnapshot geomagnetic) {
        int[] values = new int[weather.trendTemperatures.length];
        for (int index = 0; index < values.length; index++) {
            int pointScore = 8;
            if (index > 0) {
                double tempDelta = Math.abs(weather.trendTemperatures[index] - weather.trendTemperatures[index - 1]);
                double pressureDelta = Math.abs(weather.trendPressures[index] - weather.trendPressures[index - 1]);
                if (tempDelta >= 7) {
                    pointScore += tempDelta >= 10 ? 28 : 20;
                }
                if (pressureDelta >= 4.5) {
                    pointScore += pressureDelta >= 7.5 ? 26 : 18;
                }
            } else {
                pointScore = Math.max(pointScore, Math.min(score, 100));
            }
            if (weather.trendHumidity[index] >= 80 || weather.trendHumidity[index] <= 25) {
                pointScore += 12;
            }
            if (geomagnetic.maxKp >= 5) {
                pointScore += 18;
            } else if (geomagnetic.maxKp >= 4) {
                pointScore += 8;
            }
            values[index] = Math.min(pointScore, 100);
        }
        return values;
    }

    private String[] buildTrendNotes(WeatherSnapshot weather, GeomagneticSnapshot geomagnetic, int[] trend) {
        String[] notes = new String[trend.length];
        for (int index = 0; index < trend.length; index++) {
            if (trend[index] < 30) {
                notes[index] = "";
                continue;
            }
            if (index > 0 && Math.abs(weather.trendPressures[index] - weather.trendPressures[index - 1]) >= 4.5) {
                notes[index] = "давление";
            } else if (index > 0 && Math.abs(weather.trendTemperatures[index] - weather.trendTemperatures[index - 1]) >= 7) {
                notes[index] = "температура";
            } else if (geomagnetic.maxKp >= 4) {
                notes[index] = "Kp";
            } else if (weather.trendHumidity[index] >= 80 || weather.trendHumidity[index] <= 25) {
                notes[index] = "влажность";
            } else {
                notes[index] = "индекс " + trend[index];
            }
        }
        return notes;
    }

    private String buildNotification(
            RiskLevel level,
            Set<String> tags,
            List<String> reasons,
            WeatherSnapshot weather,
            GeomagneticSnapshot geomagnetic
    ) {
        String prefix = level == RiskLevel.HIGH ? "Экстренное предупреждение: " : "Информационный прогноз: ";
        if (tags.contains("geomagnetic") && geomagnetic.maxKp >= 5) {
            return prefix + "возможна магнитная буря Kp " + round(geomagnetic.maxKp) + ". Снизьте нагрузку.";
        }
        if (tags.contains("pressure")) {
            return prefix + "ожидается скачок давления " + round(weather.pressureDelta24h) + " мм рт. ст.";
        }
        if (tags.contains("temperature")) {
            return prefix + "ожидается перепад температуры " + round(weather.temperatureDelta24h) + " °C.";
        }
        return prefix + (reasons.isEmpty() ? "проверьте рекомендации в MyMeteo." : reasons.get(0));
    }

    private String round(double value) {
        return String.format(java.util.Locale.getDefault(), "%.1f", value);
    }
}
