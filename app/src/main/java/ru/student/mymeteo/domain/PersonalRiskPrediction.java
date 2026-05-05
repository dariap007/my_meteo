package ru.student.mymeteo.domain;

public class PersonalRiskPrediction {
    public final boolean ready;
    public final boolean highRisk;
    public final double probability;
    public final double accuracy;
    public final int trainingSize;
    public final int validationSize;
    public final String strongestFactor;
    public final String explanation;

    public PersonalRiskPrediction(
            boolean ready,
            boolean highRisk,
            double probability,
            double accuracy,
            int trainingSize,
            int validationSize,
            String strongestFactor,
            String explanation
    ) {
        this.ready = ready;
        this.highRisk = highRisk;
        this.probability = probability;
        this.accuracy = accuracy;
        this.trainingSize = trainingSize;
        this.validationSize = validationSize;
        this.strongestFactor = strongestFactor;
        this.explanation = explanation;
    }

    public static PersonalRiskPrediction notReady(int samples, String explanation) {
        return new PersonalRiskPrediction(
                false,
                false,
                0,
                0,
                samples,
                0,
                "",
                explanation
        );
    }
}
