package com.senior.processing;

import com.senior.processing.utils.ColorHistogram;
import com.senior.processing.utils.ContentFinder;
import com.senior.processing.utils.Score;
import com.senior.processing.utils.VideoSettings;
import org.apache.commons.io.FilenameUtils;
import org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar;
import org.bytedeco.javacpp.helper.opencv_core.AbstractIplImage;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Logger;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_video.cvMeanShift;

public class Processing {

    private static final Logger log = Logger.getLogger(Processing.class.getName());

    private static final int MIN_SATURATION = 65;
    private static final String FILE_ENCODING = "UTF-8";
    private static final String SCORE_FILE_NAME = "Score.txt";
    private static final String FRAME_EXTENSION = ".jpg";

    private final String videoLocation;
    private final String videoPath;
    private final ArrayList<IplImage> oldFrames;
    private final VideoSettings settings;
    private final OpenCVFrameConverter.ToIplImage converter;
    private LinkedList<IplImage> newFrames;
    private LinkedList<ArrayList<Integer>> sourceFile;

    public Processing(final String inputLocation, final String referenceLocation) {
        this.settings = new VideoSettings();
        this.oldFrames = new ArrayList<IplImage>();
        this.newFrames = new LinkedList<IplImage>();
        this.videoLocation = inputLocation;
        this.videoPath = FilenameUtils.getFullPath(inputLocation);
        converter = new OpenCVFrameConverter.ToIplImage();
        readSourceFile(referenceLocation);
    }


    public void separateFrames() throws FrameGrabber.Exception {

        log.info("video separating is starting");

        String videoFormat = FilenameUtils.getExtension(videoLocation);

        final FrameGrabber videoGrabber = new FFmpegFrameGrabber(videoLocation);
        videoGrabber.setFormat(videoFormat);
        videoGrabber.start();


        // Save settings for creating new video from old video
        settings.setFormat(videoGrabber.getFormat());
        settings.setTimestamp(videoGrabber.getTimestamp());
        settings.setFrameRate(videoGrabber.getFrameRate());
        settings.setSampleRate(videoGrabber.getSampleRate());
        settings.setImageWidth(videoGrabber.getImageWidth());
        settings.setImageHeight(videoGrabber.getImageHeight());
        settings.setFrameNumber(videoGrabber.getFrameNumber());
        settings.setSampleFormat(videoGrabber.getSampleFormat());

        int countImages = 1;
        String folder = FilenameUtils.concat(videoPath, "frames");
        new File(folder).mkdir(); // create folder

        Frame vFrameImage;
        do {
            vFrameImage = videoGrabber.grab();
            if (vFrameImage != null) { // add to ArrayList and save to file
                IplImage image = converter.convert(vFrameImage);
                oldFrames.add(image.clone());
                saveFrames(folder + "/frame" + countImages + FRAME_EXTENSION, image);
                ++countImages;
            }
        } while (vFrameImage != null);

        log.info("video separating's done");
    }

    public void framesProcessing() throws FileNotFoundException, UnsupportedEncodingException {
        log.info("frame processing is starting");

        String folder = FilenameUtils.concat(videoPath, "newFrames");
        new File(folder).mkdir();

        newFrames = new LinkedList<IplImage>();

        CvRect targetRect = new CvRect();

        IplImage result;
        IplImage hsvTargetImage;
        IplImage saturationChannel;

        CvHistogram templateHueHist;
        CvTermCriteria termCriteria;

        CvConnectedComp searchResults = new CvConnectedComp();

        templateHueHist = updateHistogram(targetRect);

        // Search termination criteria
        termCriteria = new CvTermCriteria();
        termCriteria.max_iter(10);
        termCriteria.epsilon(0.01);
        termCriteria.type(CV_TERMCRIT_ITER);

        PrintWriter writer = new PrintWriter(FilenameUtils.concat(videoPath, SCORE_FILE_NAME)
                , FILE_ENCODING);

        int count = 1;
        double total = 0;

        for (IplImage targetImage : oldFrames) {

            hsvTargetImage = AbstractIplImage.create(cvGetSize(targetImage), targetImage.depth(), 3);
            cvCvtColor(targetImage, hsvTargetImage, CV_BGR2HSV);

            // Identify pixels with low saturation
            saturationChannel = ColorHistogram.splitChannels(hsvTargetImage)[1];
            cvThreshold(saturationChannel, saturationChannel, MIN_SATURATION, 255, CV_THRESH_BINARY);

            // back-projection
            ContentFinder.histogram = templateHueHist;
            result = ContentFinder.find(hsvTargetImage);

            // Eliminate low saturation pixels
            cvAnd(result, saturationChannel, result, null);

            cvMeanShift(result, targetRect, termCriteria, searchResults);
            targetRect = searchResults.rect();

            // Draw green rectangle
            int g_x = targetRect.x();
            int g_y = targetRect.y();
            int g_width = targetRect.width();
            int g_height = targetRect.height();
            cvRectangle(targetImage
                    , cvPoint(g_x - g_width, g_y - g_height)
                    , cvPoint(g_x + g_width, g_y + g_height),
                    AbstractCvScalar.GREEN, 1, 8, 0);

            // Draw red rectangle
            ArrayList<Integer> array = sourceFile.get(count - 1);
            int r_x = array.get(1);
            int r_y = array.get(2);
            int r_width = array.get(3);
            int r_height = array.get(4);
            cvRectangle(targetImage
                    , cvPoint(r_x - r_width, r_y - r_height)
                    , cvPoint(r_x + r_width, r_y + r_height)
                    , AbstractCvScalar.RED, 1, 8, 0);

            // save new frame
            newFrames.add(targetImage.clone());
            saveFrames(folder + "/frame" + count + FRAME_EXTENSION, targetImage);

            double currentFrameScore = Score.calculateScore(r_x, r_y, g_x, g_y);
            total += currentFrameScore;

            writer.println(currentFrameScore);
            ++count;
        }
        writer.println("Average = " + total / count); //average
        writer.close();

        log.info("frame processing's done");
    }

    public void createNewVideo(final String videoName) throws FrameRecorder.Exception {
        log.info("New video is creating");

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(videoName
                , settings.getImageWidth(), settings.getImageHeight());

        recorder.setFormat(settings.getFormat());
        recorder.setFrameRate(settings.getFrameRate());
        recorder.setTimestamp(settings.getTimestamp());
        recorder.setSampleRate(settings.getSampleRate());
        recorder.setFrameNumber(settings.getFrameNumber());
        recorder.setSampleFormat(settings.getSampleFormat());

        recorder.start();
        for (IplImage image : newFrames) {
            try {
                recorder.record(converter.convert(image));
            } catch (final org.bytedeco.javacv.FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
        recorder.stop();
        recorder.release();

        log.info("New video's created");
    }

    private CvHistogram updateHistogram(CvRect targetRect) {

        IplImage templateImage = oldFrames.get(0).clone();
        ArrayList<Integer> array = sourceFile.get(0);

        final int x = array.get(1);
        final int y = array.get(2);
        final int width = array.get(3);
        final int height = array.get(4);

        targetRect.x(x);
        targetRect.y(y);
        targetRect.width(width);
        targetRect.height(height);

        IplROI roi = new IplROI();

        roi.xOffset(x);
        roi.yOffset(y);
        roi.height(height);
        roi.width(width);

        templateImage.roi(roi);

        return ColorHistogram.getHueHistogram(templateImage, MIN_SATURATION);

    }

    private void saveFrames(final String frameName, final IplImage image) {
        cvSaveImage(frameName, image);

    }

    private void readSourceFile(final String fileLocation) {
        sourceFile = new LinkedList<ArrayList<Integer>>();

        Scanner input = null;
        Scanner colReader = null;

        try {
            input = new Scanner(new File(fileLocation));

            while (input.hasNextLine()) {
                colReader = new Scanner(input.nextLine());
                final ArrayList<Integer> col = new ArrayList<Integer>();

                while (colReader.hasNextInt()) {
                    col.add(colReader.nextInt());
                }

                sourceFile.add(col);
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            assert colReader != null;
            colReader.close();
            input.close();
        }

    }
}
