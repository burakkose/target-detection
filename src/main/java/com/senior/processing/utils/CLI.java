package com.senior.processing.utils;

import org.apache.commons.cli.*;

import java.util.logging.Logger;

public class CLI {

    private static final Logger log = Logger.getLogger(CLI.class.getName());
    private String[] args = null;
    private final Options options = new Options();
    private String inputLocation;
    private String outputLocation;
    private String referenceLocation;

    public CLI(String[] args) {

        this.args = args;

        options.addOption("h", "help", false, "show help.");
        options.addOption("i", "input", true, "Location of input video file");
        options.addOption("o", "output", true, "Location of output video file");
        options.addOption("r", "reference", true, "Location of reference file");

    }

    public void parse() {
        CommandLineParser parser = new BasicParser();

        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h"))
                help();

            if (cmd.hasOption("i")) {
                log.info("Input file =" + cmd.getOptionValue("i"));
                inputLocation = cmd.getOptionValue("i");
            } else {
                log.severe("Missing i option");
                help();
            }

            if (cmd.hasOption("o")) {
                log.info("Output file =" + cmd.getOptionValue("o"));
                outputLocation = cmd.getOptionValue("o");
            } else {
                log.severe("Missing o option");
                help();
            }

            if (cmd.hasOption("r")) {
                log.info("Reference file =" + cmd.getOptionValue("r"));
                referenceLocation = cmd.getOptionValue("r");
            } else {
                log.severe("Missing r option");
                help();
            }

        } catch (ParseException e) {
            log.severe("Failed to parse command line properties");
            help();
        }
    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Main", options);
        System.exit(0);
    }

    public String getInputLocation() {
        return inputLocation;
    }

    public String getOutputLocation() {
        return outputLocation;
    }

    public String getReferenceLocation() {
        return referenceLocation;
    }
}
