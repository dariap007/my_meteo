package ru.student.mymeteo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersonalRiskModel {
    private static final int MIN_SAMPLES = 8;
    private static final int BAD_WELLBEING_LIMIT = 4;

    public PersonalRiskPrediction trainAndPredict(
            List<DiaryEntry> entries,
            WeatherSnapshot weather,
            GeomagneticSnapshot geomagnetic
    ) {
        List<Sample> samples = prepareDataset(entries);
        if (samples.size() < MIN_SAMPLES) {
            return PersonalRiskPrediction.notReady(
                    samples.size(),
                    "Для обучения нужно минимум " + MIN_SAMPLES + " очищенных записей дневника."
            );
        }

        int badCount = 0;
        for (Sample sample : samples) {
            if (sample.badDay) {
                badCount++;
            }
        }
        int goodCount = samples.size() - badCount;
        if (badCount < 2 || goodCount < 2) {
            return PersonalRiskPrediction.notReady(
                    samples.size(),
                    "Нужны и плохие, и обычные дни, чтобы модель нашла различия."
            );
        }

        Collections.sort(samples, (left, right) -> Long.compare(left.createdAt, right.createdAt));
        int split = Math.max(4, Math.round(samples.size() * 0.7f));
        split = Math.min(split, samples.size() - 1);
        List<Sample> train = samples.subList(0, split);
        List<Sample> validation = samples.subList(split, samples.size());

        Model model = trainModel(train);
        int correct = 0;
        for (Sample sample : validation) {
            if (model.predict(sample.features) == sample.badDay) {
                correct++;
            }
        }
        double accuracy = validation.isEmpty() ? 0 : (double) correct / validation.size();

        double[] currentFeatures = new double[]{
                pressureFeature(weather.currentPressure),
                temperatureFeature(weather.currentTemperature),
                humidityFeature(weather.currentHumidity),
                kpFeature(geomagnetic.maxKp)
        };
        double riskScore = model.score(currentFeatures);
        boolean highRisk = model.predict(currentFeatures);
        double probability = clamp(0.5 + (riskScore - model.threshold));
        String factor = factorTitle(model.strongestFactorIndex());
        String explanation = highRisk
                ? "Модель нашла связь прошлых жалоб с фактором: " + factor + "."
                : "По личным записям выраженного повышения риска сейчас не видно.";

        return new PersonalRiskPrediction(
                true,
                highRisk,
                probability,
                accuracy,
                train.size(),
                validation.size(),
                factor,
                explanation
        );
    }

    private List<Sample> prepareDataset(List<DiaryEntry> entries) {
        List<Sample> samples = new ArrayList<>();
        for (DiaryEntry entry : entries) {
            if (entry.wellbeing < 1 || entry.wellbeing > 10) {
                continue;
            }
            if (entry.pressure <= 0 || entry.humidity <= 0) {
                continue;
            }
            samples.add(new Sample(
                    entry.createdAt,
                    new double[]{
                            pressureFeature(entry.pressure),
                            temperatureFeature(entry.temperature),
                            humidityFeature(entry.humidity),
                            kpFeature(entry.kpIndex)
                    },
                    entry.wellbeing <= BAD_WELLBEING_LIMIT
            ));
        }
        return samples;
    }

    private Model trainModel(List<Sample> samples) {
        double[] badMeans = new double[4];
        double[] goodMeans = new double[4];
        int badCount = 0;
        int goodCount = 0;

        for (Sample sample : samples) {
            double[] target = sample.badDay ? badMeans : goodMeans;
            for (int index = 0; index < sample.features.length; index++) {
                target[index] += sample.features[index];
            }
            if (sample.badDay) {
                badCount++;
            } else {
                goodCount++;
            }
        }

        for (int index = 0; index < 4; index++) {
            badMeans[index] = badCount == 0 ? 0 : badMeans[index] / badCount;
            goodMeans[index] = goodCount == 0 ? 0 : goodMeans[index] / goodCount;
        }

        double[] weights = new double[4];
        double total = 0;
        for (int index = 0; index < 4; index++) {
            weights[index] = Math.max(0.05, badMeans[index] - goodMeans[index]);
            total += weights[index];
        }
        for (int index = 0; index < 4; index++) {
            weights[index] /= total;
        }

        double badCenter = dot(weights, badMeans);
        double goodCenter = dot(weights, goodMeans);
        double threshold = (badCenter + goodCenter) / 2.0;
        return new Model(weights, threshold);
    }

    private double pressureFeature(double pressureMmHg) {
        return clamp(Math.abs(pressureMmHg - 760.0) / 20.0);
    }

    private double temperatureFeature(double temperatureCelsius) {
        if (temperatureCelsius < 5) {
            return clamp((5 - temperatureCelsius) / 20.0);
        }
        if (temperatureCelsius > 25) {
            return clamp((temperatureCelsius - 25) / 15.0);
        }
        return 0;
    }

    private double humidityFeature(double humidityPercent) {
        if (humidityPercent < 40) {
            return clamp((40 - humidityPercent) / 20.0);
        }
        if (humidityPercent > 65) {
            return clamp((humidityPercent - 65) / 25.0);
        }
        return 0;
    }

    private double kpFeature(double kpIndex) {
        return clamp((kpIndex - 3.0) / 4.0);
    }

    private String factorTitle(int index) {
        switch (index) {
            case 0:
                return "атмосферное давление";
            case 1:
                return "температура";
            case 2:
                return "влажность";
            case 3:
                return "Kp-индекс";
            default:
                return "погодные условия";
        }
    }

    private double dot(double[] left, double[] right) {
        double result = 0;
        for (int index = 0; index < left.length; index++) {
            result += left[index] * right[index];
        }
        return result;
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    private static class Sample {
        final long createdAt;
        final double[] features;
        final boolean badDay;

        Sample(long createdAt, double[] features, boolean badDay) {
            this.createdAt = createdAt;
            this.features = features;
            this.badDay = badDay;
        }
    }

    private class Model {
        final double[] weights;
        final double threshold;

        Model(double[] weights, double threshold) {
            this.weights = weights;
            this.threshold = threshold;
        }

        boolean predict(double[] features) {
            return score(features) >= threshold;
        }

        double score(double[] features) {
            return dot(weights, features);
        }

        int strongestFactorIndex() {
            int strongest = 0;
            for (int index = 1; index < weights.length; index++) {
                if (weights[index] > weights[strongest]) {
                    strongest = index;
                }
            }
            return strongest;
        }
    }
}
