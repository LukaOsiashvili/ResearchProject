package com.example.phoneapp;

import java.util.LinkedList;
import java.util.Queue;

public class EmotionalStateAnalyzer {
    private static final int WINDOW_SIZE = 5; // Number of samples to analyze
    private Queue<Float> heartRateWindow = new LinkedList<>();
    private Queue<Float> movementIntensityWindow = new LinkedList<>();

    private float heartRate;
    private float[] ACC;

    // Thresholds for emotional state detection
    private static final float ANXIETY_HR_THRESHOLD = 90.0f;
    private static final float ANXIETY_MOVEMENT_THRESHOLD = 15.0f;
    private static final float ANXIETY_HR_VARIABILITY_THRESHOLD = 5.0f;
    private static final float STRESS_HR_THRESHOLD = 85.0f;
    private static final float STRESS_MOVEMENT_THRESHOLD = 10.0f;


    private static final float CALM_HR_THRESHOLD = 75.0f;

    private static final float CALM_MOVEMENT_THRESHOLD = 5.0f;

    public float getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(float heartRate) {
        this.heartRate = heartRate;
    }

    public float[] getACC() {
        return ACC;
    }

    public void setACC(float[] ACC) {
        this.ACC = ACC;
    }

//  Notice: Because of using the emulators to run the application, implementing
//  some parts is not possible due to virtual machine constraints.
//  Some code is commented and basic simpler approach is used, so that the application
//  can run and we can see how it works.
//  The commented out code is the part of the project and may be used for some functionalities,
//  but - as mentioned previously - it requires physical device.
//
    public void addHeartRateData(float heartRate) {
        heartRateWindow.offer(heartRate);
        if (heartRateWindow.size() > WINDOW_SIZE) {
            heartRateWindow.poll();
        }
    }

    public void addAccelerometerData(float[] acceleration) {
        // Calculate movement intensity using magnitude of acceleration
        float magnitude = (float) Math.sqrt(
                acceleration[0] * acceleration[0] +
                        acceleration[1] * acceleration[1] +
                        acceleration[2] * acceleration[2]
        );

        movementIntensityWindow.offer(magnitude);
        if (movementIntensityWindow.size() > WINDOW_SIZE) {
            movementIntensityWindow.poll();
        }
    }

    public boolean hasEnoughData() {
        return heartRateWindow.size() == WINDOW_SIZE &&
                movementIntensityWindow.size() == WINDOW_SIZE;
    }

//    Simple Implementation to Run and See the Basic Working Principle of Application
    public EmotionalState analyzeEmotionalState() {

        if(getHeartRate() > ANXIETY_HR_THRESHOLD) {
            return EmotionalState.ANXIOUS;
        }

        if(getHeartRate() > STRESS_HR_THRESHOLD) {
            return EmotionalState.STRESSED;
        }

        if(getHeartRate() < CALM_HR_THRESHOLD) {
            return EmotionalState.CALM;
        }

        return EmotionalState.NORMAL;
    }

    //    The Original Code for Intended Functionality

//    public EmotionalState analyzeEmotionalState() {
//        if (!hasEnoughData()) {
//            return EmotionalState.UNKNOWN;
//        }
//
//        float avgHeartRate = calculateAverage(heartRateWindow);
//        float avgMovement = calculateAverage(movementIntensityWindow);
//        float hrVariability = calculateVariability(heartRateWindow);
//
//        // Detect anxiety (high heart rate + high movement + high variability)
//        if (avgHeartRate > ANXIETY_HR_THRESHOLD &&
//                avgMovement > ANXIETY_MOVEMENT_THRESHOLD &&
//                hrVariability > ANXIETY_HR_VARIABILITY_THRESHOLD) {
//            return EmotionalState.ANXIOUS;
//        }
//
//        // Detect stress (elevated heart rate + moderate movement)
//        if (avgHeartRate > STRESS_HR_THRESHOLD &&
//                avgMovement > STRESS_MOVEMENT_THRESHOLD) {
//            return EmotionalState.STRESSED;
//        }
//
//        // Detect calmness (normal heart rate + low movement)
//        if (avgHeartRate < CALM_HR_THRESHOLD && avgMovement < CALM_MOVEMENT_THRESHOLD) {
//            return EmotionalState.CALM;
//        }
//
//        return EmotionalState.NORMAL;
//    }

    private float calculateAverage(Queue<Float> values) {
        return (float) values.stream()
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);
    }

    private float calculateVariability(Queue<Float> values) {
        float avg = calculateAverage(values);
        float sumSquaredDiff = 0;

        for (float value : values) {
            float diff = value - avg;
            sumSquaredDiff += diff * diff;
        }

        return (float) Math.sqrt(sumSquaredDiff / values.size());
    }
}

enum EmotionalState {
    UNKNOWN,
    NORMAL,
    ANXIOUS,
    STRESSED,
    CALM
}