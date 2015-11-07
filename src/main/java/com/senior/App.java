package com.senior;

import com.senior.processing.Processing;
import com.senior.processing.utils.CLI;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Hello world!
 */
class App {

    private static final Logger log = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {

        CLI cli = new CLI(args);
        cli.parse();
        String inputLocation = cli.getInputLocation();
        String outputLocation = cli.getOutputLocation();
        String referenceLocation = cli.getReferenceLocation();

        final Processing p = new Processing(inputLocation, referenceLocation);
        try {
            log.info("Processing is starting");
            p.separateFrames();
            p.framesProcessing();
            p.createNewVideo(outputLocation);
            log.info("Everything's OK");
        } catch (FrameGrabber.Exception e) {
            log.severe(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            log.severe(e.getMessage());
        } catch (FrameRecorder.Exception e) {
            log.severe(e.getMessage());
        } catch (FileNotFoundException e) {
            log.severe(e.getMessage());
        }
    }
}
