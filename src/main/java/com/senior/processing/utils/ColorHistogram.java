package com.senior.processing.utils;

import org.bytedeco.javacpp.helper.opencv_core.AbstractIplImage;
import org.bytedeco.javacpp.opencv_core.*;

import static org.bytedeco.javacpp.helper.opencv_imgproc.cvCalcHist;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvCreateHist;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class ColorHistogram {

    public static CvHistogram getHueHistogram(final IplImage image, final int minSaturation) {

        // Convert RGB to HSV color space
        IplImage hsvImage = AbstractIplImage.create(cvGetSize(image), image.depth(), 3);

        cvCvtColor(image, hsvImage, CV_BGR2HSV);

        // Split the 3 channels into 3 images
        IplImage[] hsvChannels = splitChannels(hsvImage);
        IplImage saturationMask = null;

        if (minSaturation > 0) {
            saturationMask = AbstractIplImage.create(cvGetSize(hsvImage)
                    , IPL_DEPTH_8U, 1);
            cvThreshold(hsvChannels[1], saturationMask, minSaturation, 255
                    , CV_THRESH_BINARY);
        }

        // Compute histogram of the hue channel
        return getHistogram(hsvChannels[0], saturationMask);

    }

    private static CvHistogram getHistogram(final IplImage image, final IplImage mask) {

        // Allocate histogram object
        int dims = 1;
        int[] sizes = {256};
        int histType = CV_HIST_ARRAY;
        float[][] ranges = {{0, 180}};

        CvHistogram hist = cvCreateHist(dims, sizes, histType, ranges, 1);

        // Compute histogram
        int accumulate = 0;
        IplImage[] arr = {image};

        cvCalcHist(arr, hist, accumulate, mask);

        return hist;

    }

    public static IplImage[] splitChannels(final IplImage src) {

        CvSize size = cvGetSize(src);

        IplImage channel0 = AbstractIplImage.create(size, src.depth(), 1);
        IplImage channel1 = AbstractIplImage.create(size, src.depth(), 1);
        IplImage channel2 = AbstractIplImage.create(size, src.depth(), 1);

        cvSplit(src, channel0, channel1, channel2, null);

        return new IplImage[]{channel0, channel1, channel2};
    }
}
