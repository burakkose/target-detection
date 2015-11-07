package com.senior.processing.utils;

public class VideoSettings {

    private String format;
    private int frameNumber;
    private int imageWidth;
    private int ImageHeight;
    private int sampleFormat;
    private int sampleRate;
    private long timestamp;
    private double frameRate;

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public void setFrameNumber(final int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public int getSampleFormat() {
        return sampleFormat;
    }

    public void setSampleFormat(final int sampleFormat) {
        this.sampleFormat = sampleFormat;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(final int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(final double frameRate) {
        this.frameRate = frameRate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public int getImageHeight() {
        return ImageHeight;
    }

    public void setImageHeight(final int imageHeight) {
        ImageHeight = imageHeight;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(final int imageWidth) {
        this.imageWidth = imageWidth;
    }
}
