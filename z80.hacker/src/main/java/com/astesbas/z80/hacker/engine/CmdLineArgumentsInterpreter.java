package com.astesbas.z80.hacker.engine;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.astesbas.z80.hacker.util.FileDateUtil;
import com.astesbas.z80.hacker.util.SystemOut;

/**
 * Command line arguments interpreter.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 16/jun/2017
 */
public class CmdLineArgumentsInterpreter {
    
    /** The default project configuration file name */
    public static final String DEFAULT_CONFIG_FILE = "default.cfg";
    
    private static final String[] HELP_MESSAGE = {
        "Usage: z80hacker [options...] [-p {config file}]",
        "Options:",
        " -p, --project filename.cfg   Specifies the Z80 Hacker disassembler project",
        "                              configuration file (see default.cfg)",
        " -i, --init filename.cfg      Creates a default disassembler project",
        "                              configuration file",
        " -v, --verbose                Outputs process information while disassembling",
        "                              the binary file",
        " -h, --help                   Show this text message"
    };
    
    /** The disassembler descriptor file */
    private java.io.File configFile;
    
    /**
     * This cmd line arguments interpreter stores only one parameter - the configuration file name.
     * Note: This implementation is specific to the Z80 hAcker tool!
     * 
     * @return the project configuration file
     */
    public java.io.File getProjectConfigFile() {
        return this.configFile;
    }   
    
    /**
     * Interpret the command line parameters.
     * @param arguments list of cmd arguments
     */
    public void process(String[] arguments) {
        
        // This list stores the processed parameters (used to verify and warn if a parameter is duplicated)
        Set<String> processed = new HashSet<>();
        
        for (int index = 0; index < arguments.length; index++) {
            
            // verify if the parameter was already processed
            String parameter = arguments[index];
            if (!processed.add(parameter)) {
                this.showErrorMessageAndExit(
                    String.format("Error: duplicated parameter %s in the cmd line%n", parameter)
                );  
            }   
            
            switch (parameter) {
            
                    case "-h":
                case "--help":
                    this.printUsageMessage();
                    System.exit(0);
                    break;
                    
                case "-v":
                case "--verbose":
                    System.out.println("Setting verbose mode ON");
                    SystemOut.setVerbose(true);
                    break;
                    
                case "-p":
                case "--project":
                    try {
                        this.configFile = new java.io.File(arguments[++index]);
                    } catch (IndexOutOfBoundsException indexException) {
                        this.showErrorMessageAndExit("\nError: missing project configuration file name.");
                    }   
                    SystemOut.vprintf("Project configuration file \"%s\"%n", this.configFile.getPath());
                    break;
                    
                case "-i":
                case "--init":
                    try {
                        this.generateDefaultConfigFile(arguments[++index]);
                    } catch (IndexOutOfBoundsException indexException) {
                        this.generateDefaultConfigFile(DEFAULT_CONFIG_FILE);
                    }   
                    System.exit(0);
                    break;
                    
                default:
                    this.showErrorMessageAndExit(String.format("%nInvalid command line parameter \"%s\"", parameter));
                    break;
            }   
        }   
        
        // If the user does not specifies the configuration file, tries to use the default config file
        if (this.configFile == null) {
            this.configFile = new java.io.File(DEFAULT_CONFIG_FILE);
            if (this.configFile.exists()) {
                System.out.printf("Using default project config file \"%s\"%n", this.configFile.getName());
            }   
        }   
        
        if(!this.configFile.exists()) {
            this.showErrorMessageAndExit("Project configuration file not found!");
        }   
    }   
    
    private void generateDefaultConfigFile(String fileName) {
        
        InputStream inputStream = this.getClass().getResourceAsStream("/shrubbles.cfg");
        String baseName= FileDateUtil.getBaseFileName(fileName);
        Path outputPath = Paths.get(fileName);
        String line;
        
        System.out.printf("Creating default project config file %s...", outputPath.getFileName());
        
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, CREATE, APPEND)) {
                while ((line = bufferedReader.readLine()) != null) {
                    writer.write(line.replace("shrubbles", baseName));
                    writer.newLine();
                }   
            }   
            
            System.out.println("Ok");
            
        } catch (IOException ioException) {
            System.err.println("Error!");
            System.err.format("Error creating default config file: %s%n", ioException.getMessage());
            System.exit(-1);
        }   
    }   
    
    /**
     * Show error and hint messages and exit.
     * @param errorMessage
     */
    private void showErrorMessageAndExit(String errorMessage) {
        System.out.println(errorMessage);
        System.out.println("Try 'z80hacker --help' for more information.");
        System.exit(-1);
    }   
    
    /**
     * Print usage message.
     */
    private void printUsageMessage() {
        for(String line:HELP_MESSAGE) {
            System.out.println(line);
        }   
    }   
}
