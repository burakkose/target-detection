package com.senior.processing.utils;

/**
 * Created by senior on 07.11.2015.
 */
public class Score {

    // Distance of two points
    public static double calculateScore(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
