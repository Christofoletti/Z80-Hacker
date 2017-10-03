package com.astesbas.z80.hacker;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.astesbas.z80.hacker.domain.BinaryData;
import com.astesbas.z80.hacker.engine.CmdLineArgumentsInterpreter;
import com.astesbas.z80.hacker.engine.Z80Disassembler;
import com.astesbas.z80.hacker.util.ConfigFileProperties;
import com.astesbas.z80.hacker.util.FileDateUtil;
import com.astesbas.z80.hacker.util.SystemOut;

/**
 * Z80 Hacker - Z80 Disassembler tool.<br/>
 * Usage:<br/>
 *     java -jar z80.disassembler-version.jar -p parametersfile
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.1
 * @since 16/jun/2017
 */
public class Executer {
    
    /**
     * The Z80 hacker tool executer :P
     */
    public Executer() {
        System.out.println("\nZ80 Hacker - Version 1.1\n");
    }   
    
    /**
     * The main function. This is the entry point of Z80 Hacker tool!
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Executer executer = new Executer();
        CmdLineArgumentsInterpreter cmdLineInterpreter = new CmdLineArgumentsInterpreter();
        
        try {
            cmdLineInterpreter.process(args);
        } catch(IllegalArgumentException iaException) {
            executer.showErrorMessageAndExit(iaException.getMessage());
        }   
        
        Optional<java.io.File> configFile = cmdLineInterpreter.getProjectConfigFile();
        if(!configFile.isPresent()) {
            executer.showErrorMessageAndExit("Project configuration file not found!");
        } else if(!configFile.get().exists()) {
            executer.generateDefaultConfigFile(configFile.get().getName());
            System.exit(0);
        }   
        
        SystemOut.vprintf("Project configuration file \"%s\"%n", configFile.get().getPath());
        
        // Read the project configuration file
        ConfigFileProperties properties = executer.loadConfigile(configFile.get());
        
        // The binary data to be disassembled (in general a game ROM or a memory dump)
        BinaryData binaryData = BinaryData.fromProperties(properties);
        
        // The Z80 disassembler engine
        Z80Disassembler z80Disassembler = new Z80Disassembler(binaryData);
        z80Disassembler.setProperties(properties);
        
        // ******** execute the disassembler process
        z80Disassembler.run();
    }   
    
    /**
     * Load the project configuration from file.
     * @param configFile the configuration file name
     * @return ConfigFileProperties properties read from configuration file
     */
    private ConfigFileProperties loadConfigile(java.io.File configFile) {
        
        ConfigFileProperties configFileProperties = new ConfigFileProperties();
        
        try {
            configFileProperties.load(configFile);
        } catch (IllegalArgumentException | java.io.IOException exception) {
            System.out.printf("Error reading configuration file %s!\n\t%s%n",
                configFile.getName(), exception.getMessage());
            System.exit(-1);
        }   
        
        return configFileProperties;
    }   
    
    /**
     * Create a default project configuration file.
     * This action should be done by the executer, but is being done here to avoid a lot of 
     * @param fileName the configuration file name
     */
    private void generateDefaultConfigFile(String fileName) {
        
        InputStream inputStream = this.getClass().getResourceAsStream("/shrubbles.cfg");
        String baseName = FileDateUtil.getBaseFileName(fileName);
        
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            System.out.printf("Creating default project config file \"%s\"...", fileName);
            Path outputPath = Paths.get(fileName);
            
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, CREATE, APPEND)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if(line.contains("shrubbles")) {
                        writer.write(line.replace("shrubbles", baseName));
                    } else if(line.contains("17/jul/1972")) {
                        writer.write(line.replace("17/jul/1972", FileDateUtil.getCurrentTime()));
                    } else {
                        writer.write(line);
                    }   
                    writer.newLine();
                }   
            }   
            
            System.out.println("Ok");
            
        } catch (java.nio.file.InvalidPathException ipException) {
            System.err.println("Error!");
            System.err.format("Invalid name for config file: %s%n", ipException.getMessage());
            System.exit(-1);
        } catch (java.io.IOException ioException) {
            System.err.println("Error!");
            System.err.format("Error creating default config file: %s%n", ioException.getMessage());
            System.exit(-1);
        }   
    }   
    
    /**
     * Show error message and exit.
     * @param errorMessage the error message to show
     */
    private void showErrorMessageAndExit(String errorMessage) {
        System.out.println(errorMessage);
        System.out.println("Try 'z80hacker --help' for more information.");
        System.exit(-1);
    }   
}   
