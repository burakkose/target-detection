package com.senior.processing.utils;

import org.bytedeco.javacpp.helper.opencv_core.AbstractIplImage;
import org.bytedeco.javacpp.opencv_core.*;

import static org.bytedeco.javacpp.helper.opencv_imgproc.cvCalcBackProject;
import static org.bytedeco.javacpp.opencv_core.*;

public class ContentFinder {

    public static CvHistogram histogram = null;

    public static IplImage find(IplImage image) {

        // `cvCalcBackProject` must be single channel.
        // Convert each channel image to a to 32 bit floating point image.
        IplImage[] channels = toIplImage32F(ColorHistogram.splitChannels(image));

        // Back project
        IplImage dest = AbstractIplImage.create(cvGetSize(image), IPL_DEPTH_32F, 1);
        cvCalcBackProject(channels, dest, histogram);

        return toIplImage8U(dest);
    }

    private static IplImage[] toIplImage32F(IplImage[] srcArr) {

        IplImage[] arr = new IplImage[srcArr.length];

        for (int i = 0; i < srcArr.length; i++) {
            IplImage src = srcArr[i];
            IplImage dest = AbstractIplImage.create(cvGetSize(src), IPL_DEPTH_32F, src.nChannels());
            cvConvertScale(src, dest, 1, 0);
            arr[i] = dest;
        }

        return arr;
    }

    private static IplImage toIplImage8U(IplImage src) {

        double[] min = {Double.MAX_VALUE};
        double[] max = {Double.MIN_VALUE};

        cvMinMaxLoc(src, min, max);

        double scale = 255 / (max[0] - min[0]);
        double offset = -min[0];

        IplImage dest = AbstractIplImage.create(cvGetSize(src), IPL_DEPTH_8U, src.nChannels());

        cvConvertScale(src, dest, scale, offset);

        return dest;
    }

}
