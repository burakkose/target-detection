package com.senior.processing;

import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_ITER;
import static org.bytedeco.javacpp.opencv_core.cvAnd;
import static org.bytedeco.javacpp.opencv_core.cvCloneImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRectangle;
import static org.bytedeco.javacpp.opencv_highgui.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;
import static org.bytedeco.javacpp.opencv_video.cvMeanShift;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import org.bytedeco.javacpp.opencv_core.CvHistogram;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvTermCriteria;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.IplROI;
import org.bytedeco.javacpp.opencv_imgproc.CvConnectedComp;
import org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar;
import org.bytedeco.javacpp.helper.opencv_core.AbstractIplImage;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameGrabber;

public class Processing {
	public static void main(final String[] args) {
		final Processing p = new Processing(
				"/home/senior/Desktop/Yazlab/Jogging.AVI");
		p.separateFrames();
		p.framesProcessing();
		p.createNewVideo();
		System.out.println("finish");
	}

	private final String					videoLocation;
	private ArrayList<IplImage>				oldFrames;
	private LinkedList<IplImage>			newFrames;
	private VideoSettings					settings;
	private LinkedList<ArrayList<Integer>>	sourceFile;

	public Processing(final String location) {
		videoLocation = location;
		readSourceFile("/home/senior/Desktop/Yazlab/manual_Jogging1.txt");
	}

	public void separateFrames() {

		final FrameGrabber videoGrabber = new FFmpegFrameGrabber(videoLocation);

		videoGrabber.setFormat("avi");

		settings = new VideoSettings();
		oldFrames = new ArrayList<IplImage>();

		try {
			videoGrabber.start();

			// Set settings for create new video from processing frames
			settings.setFormat(videoGrabber.getFormat());
			settings.setTimestamp(videoGrabber.getTimestamp());
			settings.setFrameRate(videoGrabber.getFrameRate());
			settings.setSampleRate(videoGrabber.getSampleRate());
			settings.setImageWidth(videoGrabber.getImageWidth());
			settings.setImageHeight(videoGrabber.getImageHeight());
			settings.setFrameNumber(videoGrabber.getFrameNumber());
			settings.setSampleFormat(videoGrabber.getSampleFormat());

			IplImage vFrameImage = null;

			int countImages = 1;
			new File("frames").mkdir(); // create folder

			do {
				try {
					vFrameImage = videoGrabber.grab();
					if (vFrameImage != null) { // add ArrayList and save to file
						oldFrames.add(vFrameImage.clone());
						saveFrames("frames/frame_" + countImages + ".jpg",
								vFrameImage);
						++countImages;
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}

			} while (vFrameImage != null);

		} catch (final Exception e1) {
			e1.printStackTrace();
		}

	}

	public void framesProcessing() {
		try {

			int count = 1;
			double total = 0;
			final int minSaturation = 65;

			new File("newframes").mkdir();

			newFrames = new LinkedList<IplImage>(); // list for processing
													// frames
			CvRect targetRect = new CvRect();

			IplImage result = null;
			IplImage hsvTargetImage = null;
			IplImage saturationChannel = null;

			CvHistogram templateHueHist = null; // Color histogram
			CvTermCriteria termCriteria = null; // term criteria

			CvConnectedComp searchResults = new CvConnectedComp(); // fo
																	// meanshift
																	// algorithm

			templateHueHist = updateHistogram(targetRect, 0); // first frame
																// target
																// position

			// Search termination criteria
			termCriteria = new CvTermCriteria();
			termCriteria.max_iter(10);
			termCriteria.epsilon(0.01);
			termCriteria.type(CV_TERMCRIT_ITER);

			PrintWriter writer = new PrintWriter("Euclidean.txt", "UTF-8");

			for (IplImage targetImage : oldFrames) {

				hsvTargetImage = AbstractIplImage.create(
						cvGetSize(targetImage), targetImage.depth(), 3);
				cvCvtColor(targetImage, hsvTargetImage, CV_BGR2HSV);

				// Identify pixels with low saturation
				saturationChannel = ColorHistoram.splitChannels(hsvTargetImage)[1];
				cvThreshold(saturationChannel, saturationChannel,
						minSaturation, 255, CV_THRESH_BINARY);

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
				cvRectangle(targetImage,
						cvPoint(g_x - g_width, g_y - g_height),
						cvPoint(g_x + g_width, g_y + g_height),
						AbstractCvScalar.GREEN, 1, 8, 0);

				// Draw red rectangle
				ArrayList<Integer> array = sourceFile.get(count - 1);
				int r_x = array.get(1);
				int r_y = array.get(2);
				int r_width = array.get(3);
				int r_height = array.get(4);
				cvRectangle(targetImage,
						cvPoint(r_x - r_width, r_y - r_height),
						cvPoint(r_x + r_width, r_y + r_height),
						AbstractCvScalar.RED, 1, 8, 0);

				// save new frame
				newFrames.add(targetImage.clone());
				saveFrames("newframes/newframe_" + count + ".jpg", targetImage);

				double cost = Math.sqrt(Math.pow(r_x - g_x, 2)
						+ Math.pow(r_y - g_y, 2)); // for success score
				total += cost; // total score

				writer.println(cost); // current frame cost

				System.gc();
				++count;
			}
			writer.println(total); // total score
			writer.close();
		} catch (final java.lang.Exception e) {
			e.printStackTrace();
		}

	}

	public void createNewVideo() {
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("newvideo.avi",
				settings.getImageWidth(), settings.getImageHeight());

		recorder.setFormat(settings.getFormat());
		recorder.setFrameRate(settings.getFrameRate());
		recorder.setTimestamp(settings.getTimestamp());
		recorder.setSampleRate(settings.getSampleRate());
		recorder.setFrameNumber(settings.getFrameNumber());
		recorder.setSampleFormat(settings.getSampleFormat());

		try {
			recorder.start();
			for (IplImage image : newFrames) {
				try {
					recorder.record(image);
				} catch (final org.bytedeco.javacv.FrameRecorder.Exception e) {
					e.printStackTrace();
				}
			}
			recorder.stop();
			recorder.release();
		} catch (final org.bytedeco.javacv.FrameRecorder.Exception e1) {
			e1.printStackTrace();
		}

	}

	private CvHistogram updateHistogram(CvRect targetRect, int index) {

		IplImage templateImage = cvCloneImage(oldFrames.get(index));
		ArrayList<Integer> array = sourceFile.get(index);

		final int x = array.get(1);
		final int y = array.get(2);
		final int width = array.get(3) + 3;
		final int height = array.get(4) + 17;

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

		final int minSaturation = 65;

		return ColorHistoram.getHueHistogram(templateImage, minSaturation);

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
			colReader.close();
			input.close();
		}

	}
}
