package com.senior.processing;

import static com.googlecode.javacv.cpp.opencv_core.cvEllipse;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

class VideoSettings {
	private String	format;
	private int		frameNumber;
	private int		imageWidth;
	private int		ImageHeight;
	private int		sampleFormat;
	private int		sampleRate;
	private long	timestamp;
	private double	frameRate;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public int getFrameNumber() {
		return frameNumber;
	}

	public void setFrameNumber(int frameNumber) {
		this.frameNumber = frameNumber;
	}

	public int getSampleFormat() {
		return sampleFormat;
	}

	public void setSampleFormat(int sampleFormat) {
		this.sampleFormat = sampleFormat;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public double getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(double frameRate) {
		this.frameRate = frameRate;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getImageHeight() {
		return ImageHeight;
	}

	public void setImageHeight(int imageHeight) {
		ImageHeight = imageHeight;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}
}

public class Processing {
	public static void main(String[] args) {
		Processing p = new Processing("/home/senior/Desktop/Yazlab/Jogging.AVI");
		p.getFirstFrames();
		p.framesProcessing();
		p.createNewVideo();
		System.out.println("bitti");
	}

	private final String					videoLocation;
	private LinkedList<IplImage>			oldFrames;
	private LinkedList<IplImage>			newFrames;
	private ArrayList<ArrayList<Integer>>	sourceFile;
	private VideoSettings					settings;

	public Processing(String location) {
		videoLocation = location;
		readSourceFile("/home/senior/Desktop/Yazlab/manual_Jogging1.txt");
	}

	public void getFirstFrames() {

		FrameGrabber videoGrabber = new FFmpegFrameGrabber(videoLocation);

		videoGrabber.setFormat("avi");

		settings = new VideoSettings();

		settings.setFormat(videoGrabber.getFormat());
		settings.setTimestamp(videoGrabber.getTimestamp());
		settings.setFrameRate(videoGrabber.getFrameRate());
		settings.setSampleRate(videoGrabber.getSampleRate());
		settings.setImageWidth(videoGrabber.getImageWidth());
		settings.setImageHeight(videoGrabber.getImageHeight());
		settings.setFrameNumber(videoGrabber.getFrameNumber());
		settings.setSampleFormat(videoGrabber.getSampleFormat());

		oldFrames = new LinkedList<IplImage>();

		try {
			videoGrabber.start();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		IplImage vFrameImage = null;
		int countImages = 1;

		do {
			try {
				vFrameImage = videoGrabber.grab();
				if (vFrameImage != null) {
					oldFrames.add(vFrameImage.clone());
					saveFrames("frame_" + countImages + ".jpg", vFrameImage);
					++countImages;
				}
			} catch (com.googlecode.javacv.FrameGrabber.Exception e) {
				e.printStackTrace();
			}
		} while (vFrameImage != null);
	}

	public void framesProcessing() {
		try {
			newFrames = new LinkedList<IplImage>();
			int count = 0;
			for (IplImage image : oldFrames) {
				cvEllipse(
						image,
						cvPoint(sourceFile.get(count).get(1),
								sourceFile.get(count).get(2)),
						cvSize(sourceFile.get(count).get(3),
								sourceFile.get(count).get(4)), 0, 0, 360,
						CvScalar.GREEN, 1, 8, 0);
				newFrames.add(image.clone());
				saveFrames("newframe_" + count + ".jpg", image);
				++count;
			}
		} catch (java.lang.Exception e) {
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
				} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			recorder.stop();
			recorder.release();
		} catch (com.googlecode.javacv.FrameRecorder.Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private void saveFrames(String frameName, IplImage image) {
		cvSaveImage(frameName, image);
	}

	private void readSourceFile(String fileLocation) {
		sourceFile = new ArrayList<ArrayList<Integer>>();

		Scanner input = null;
		Scanner colReader = null;

		try {
			input = new Scanner(new File(fileLocation));

			while (input.hasNextLine()) {
				colReader = new Scanner(input.nextLine());
				ArrayList<Integer> col = new ArrayList<Integer>();

				while (colReader.hasNextInt()) {
					col.add(colReader.nextInt());
				}

				sourceFile.add(col);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			colReader.close();
			input.close();
		}

	}
}