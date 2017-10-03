package com.astesbas.z80.hacker.engine;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
    
    /** The application help message */
    private static final String[] HELP_MESSAGE = {
        "Usage: z80hacker [options...] [-p {config file}]",
        "Options:",
        " -p, --project filename.cfg   Specifies the Z80 Hacker disassembler project",
        "                              configuration file (see default.cfg)",
        " -i, --init [filename.cfg]    Creates a default disassembler project",
        "                              configuration file (the filename is optional)",
        " -v, --verbose                Outputs process information while disassembling",
        "                              the binary file",
        " -h, --help                   Show this text message"
    };  
    
    /** The disassembler project configuration file */
    private Optional<java.io.File> configFile = Optional.empty();
    
    /**
     * This cmd line arguments interpreter stores only one parameter - the configuration file name.
     * Note: This implementation is specific to the Z80 hAcker tool!
     * 
     * @return the project configuration file
     */
    public Optional<java.io.File> getProjectConfigFile() {
        return this.configFile;
    }   
    
    /**
     * Interpret the command line parameters.
     * @param arguments list of cmd arguments
     */
    public void process(String[] arguments) throws IllegalArgumentException {
        
        // This list stores the processed parameters (used to verify and warn if a parameter is duplicated)
        Set<String> processed = new HashSet<>();
        
        // The default configuration file reference
        java.io.File defaultConfigFile = new java.io.File(DEFAULT_CONFIG_FILE);
        
        for (int index = 0; index < arguments.length; index++) {
            
            // verify if the parameter was already processed
            String parameter = arguments[index];
            if (!processed.add(parameter)) {
                throw new IllegalArgumentException (
                    String.format("Error: duplicated parameter %s in the command line%n", parameter)
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
                        java.io.File projectConfigFile = new java.io.File(arguments[++index]);
                        if(projectConfigFile.exists()) {
                            this.configFile = Optional.of(projectConfigFile);
                        }   
                    } catch (IndexOutOfBoundsException indexException) {
                        throw new IllegalArgumentException("Error: missing project configuration file name.");
                    }   
                    
                    break;
                    
                case "-i":
                case "--init":
                    try {
                        this.configFile = Optional.of(new java.io.File(arguments[++index]));
                    } catch (IndexOutOfBoundsException indexException) {
                        this.configFile = Optional.of(defaultConfigFile);
                    }   
                    if(this.configFile.get().exists()) {
                        throw new IllegalArgumentException(
                            String.format("Could not initialize file \"%s\". File already exists!",
                                    this.configFile.get().getName())
                        );
                    }   
                    break;
                    
                default:
                    throw new IllegalArgumentException(
                        String.format("Invalid command line parameter \"%s\"", parameter)
                    );  
            }   
        }   
        
        // If the user does not specifies the configuration file, tries to use the default configuration file
        if (!this.configFile.isPresent()) {
            if (defaultConfigFile.exists()) {
                this.configFile = Optional.of(defaultConfigFile);
                System.out.printf("Using default project configuration file \"%s\"%n", DEFAULT_CONFIG_FILE);
            }   
        }   
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
