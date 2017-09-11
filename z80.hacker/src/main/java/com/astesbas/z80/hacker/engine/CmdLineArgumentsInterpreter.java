package com.astesbas.z80.hacker.engine;

import java.util.HashSet;
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
    public static final String DEFAULT_CONFIG_FILE = "./default.cfg";
    
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
        
        // list of processed parameters
        Set<String> processed = new HashSet<>();
        
        try {
            for(int index = 0; index < arguments.length; index++) {
                
                // verify if the parameter was already processed
                String parameter = arguments[index];
                if(!processed.add(parameter)) {
                    System.out.printf("Warning: duplicated parameter %s in the cmd line%n", parameter);
                    continue;
                }   
                
                switch(parameter) {
                    
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
                        this.configFile = new java.io.File(arguments[++index]);
                        SystemOut.vprintf("Project configuration file \"%s\"%n", this.configFile.getPath());
                        break;
                    
                    case "-i":
                    case "--init":
                        this.showErrorMessageAndExit("Feature not implemented yet!");
                        //this.ddFile = new java.io.File(DEFAULT_DD_FILE_NAME);
                        //String userDir = System.getProperty("user.dir");
                        
//                        java.io.File source = new java.io.File("H:\\work-temp\\file");
//                        java.io.File dest = new java.io.File(userDir+DEFAULT_DD_FILE_NAME);
//                        try {
//                            Files.copy(source, dest);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                        
                        break;
                        
                    default:
                        this.showErrorMessageAndExit(
                            String.format("%nInvalid command line parameter \"%s\"", parameter)
                        );
                        break;
                }   
            }   
            
            // verify if the project configuration file is available
            if(this.configFile == null) {
                this.configFile = new java.io.File(DEFAULT_CONFIG_FILE);
                if(this.configFile.exists()) {
                    System.out.printf("Using default project config file \"%s\"%n", this.configFile.getName());
                } else {
                    this.showErrorMessageAndExit("%nProject configuration file not found!");
                }   
            }   
            
        } catch(IndexOutOfBoundsException indexException) {
            this.showErrorMessageAndExit("\nError: missing project configuration file name.");
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
