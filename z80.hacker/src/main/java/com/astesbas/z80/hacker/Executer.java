package com.astesbas.z80.hacker;

import static com.astesbas.z80.hacker.util.ConfigFileReader.ConfigKey.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.astesbas.z80.hacker.domain.Memory;
import com.astesbas.z80.hacker.engine.CmdLineArgumentsInterpreter;
import com.astesbas.z80.hacker.engine.Z80Disassembler;
import com.astesbas.z80.hacker.util.ConfigFileReader;
import com.astesbas.z80.hacker.util.StringUtil;
import com.astesbas.z80.hacker.util.SystemOut;

/**
 * Z80 Hacker - Z80 Disassembler tool.<br/>
 * Usage:<br/>
 *     java -jar z80.disassembler-version.jar -p parametersfile
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 16/jun/2017
 */
public class Executer {
    
    /** The configuration file reader */
    private ConfigFileReader configFileReader = new ConfigFileReader();
    
    /** The memory (binary data) for disassembling */
    private Memory memory = new Memory();
    
    /** The Z80 disassembler */
    private Z80Disassembler z80Disassembler = new Z80Disassembler();
    
    /**
     * 
     */
    public Executer() {
        SystemOut.get().println("Z80 Hacker - Version 0.1");
    }   
    
    /**
     * Return the memory object (binary data to be disassembled).
     * @return the memory
     */
    public Memory getMemory() {
        return this.memory;
    }   
    
    /**
     * @return the disassembler processor
     */
    public Z80Disassembler getDisassembler() {
        return this.z80Disassembler;
    }

    /**
     * Setup the memory (binary data) for disassembling.
     */
    private void setupMemory() {
        
        // the binary file name (initialized with an empty value)
        Optional<String> binaryFileName = Optional.empty();
        
        try {
            
            // get the binary file name, start and end addresses
            binaryFileName = this.configFileReader.getString(BINARY_FILE);
            int startAddress = this.configFileReader.getAddress(BINARY_START).orElse(Memory.START_ADDRESS);
            int endAddress = this.configFileReader.getAddress(BINARY_END).orElse(Memory.END_ADDRESS);
            
            if(binaryFileName.isPresent()) {
                File binaryFile = new File(binaryFileName.get());
                this.memory.loadFromBinaryFile(binaryFile, startAddress, endAddress);
            } else {
                System.err.printf("%nError: property %s not found in config file!%n", BINARY_FILE);
                System.exit(-1);
            }   
            
        } catch (NumberFormatException | IllegalAccessException exeception) {
            System.err.printf(
                String.format("%nError reading binary start/end address parameter: %s", exeception.getMessage())
            );  
            System.exit(-1);
        } catch(IOException ioException) {
            System.err.printf("%nError reading binary file: %n\t%s%n", ioException.getMessage());
            System.exit(-1);
        }   
    }   
    
    /**
     * Get properties for disassembler from config reader. 
     */
    public void setupDisassembler() {
        
        try {
            // get the binary file name, start and end addresses
            String binaryFileName = this.configFileReader.getString(BINARY_FILE).get();
            
            // default base name for output files (asm, obj and log)
            String baseFileName = Executer.getBaseFileName(binaryFileName);
            String outputFileName = this.configFileReader.getString(OUTPUT_FILE).orElse(baseFileName+".asm");
            String objectFileName = this.configFileReader.getString(OBJECT_FILE).orElse(baseFileName+".obj");
            String logtFileName = this.configFileReader.getString(LOG_FILE).orElse(baseFileName+".log");
            
            // get output format properties
            int dbAlign = this.configFileReader.getInteger(DB_ALIGN).orElse(16);
            int tabSize = this.configFileReader.getInteger(TAB_SIZE).orElse(16);
            
            // disassembler process properties
            boolean autoDjnzLabels = this.configFileReader.getBoolean(AUTO_DJNZ_LABELS).orElse(true);
            boolean autoJrLabels = this.configFileReader.getBoolean(AUTO_JR_LABELS).orElse(true);
            boolean autoJpLabels = this.configFileReader.getBoolean(AUTO_JP_LABELS).orElse(true);
            boolean autoCallLabels = this.configFileReader.getBoolean(AUTO_CALL_LABELS).orElse(true);
            int lowMem = this.configFileReader.getAddress(LOW_MEM).orElse(Memory.START_ADDRESS);
            int highMem = this.configFileReader.getAddress(HIGH_MEM).orElse(Memory.END_ADDRESS);
            
            // setup the starting point addresses
            List<String> startingPoints = this.configFileReader.get(START_OFF);
            for(String address:startingPoints) {
                this.z80Disassembler.pushStartAddress(Integer.decode(address));
            }   
            
            // setup the labels
            List<String> labelEntries = this.configFileReader.get(LABEL);
            for(String entry:labelEntries) {
                String[] split = StringUtil.splitInTwo(entry, " ");
                String label = split[0].trim();
                String address = split[1].trim();
                this.z80Disassembler.mapLabel(label, Integer.decode(address));
            }   
            
            // setup the equs
            List<String> equEntries = this.configFileReader.get(EQU);
            for(String entry:equEntries) {
                String[] split = StringUtil.splitInTwo(entry, " ");
                this.z80Disassembler.mapEqu(split[0].trim(), split[1].trim());
            }   
            
            // setup the Z80 disassembler properties
            this.z80Disassembler.setOutputFile(outputFileName);
            this.z80Disassembler.setListFile(objectFileName);
            this.z80Disassembler.setLogFile(logtFileName);
            
            // set the output format properties
            this.z80Disassembler.setDbAlign(dbAlign);
            this.z80Disassembler.setTabSize(tabSize);
            
            // set the disassembler process properties
            this.z80Disassembler.setAutoDjnzLabels(autoDjnzLabels);
            this.z80Disassembler.setAutoJrLabels(autoJrLabels);
            this.z80Disassembler.setAutoJpLabels(autoJpLabels);
            this.z80Disassembler.setAutoCallLabels(autoCallLabels);
            this.z80Disassembler.setLowMem(lowMem);
            this.z80Disassembler.setHighMem(highMem);
            
            // set the memory data
            this.z80Disassembler.setMemoryData(this.memory);
            
        } catch (NumberFormatException | IllegalAccessException exeception) {
            System.err.printf(
                String.format("%nError reading address parameter:%n\t%s", exeception.getMessage())
            );  
            System.exit(-1);
        }   
        
        // load the Z80 op codes from resource file
        try {
            this.z80Disassembler.loadOpCodesFromFile("/z80-opcodes.dat");
        } catch (IllegalArgumentException | IOException exception) {
            System.err.printf("%nError reading op-codes from op-codes.dat file!%n\t%s%n", exception.getMessage());
            System.exit(-1);
        }   
    }   
    
    /**
     * The main function. This is the entry point of Z80 Hacker tool!
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // the project executer
        Executer executer = new Executer();
        
        // ******** the command line interpreter
        CmdLineArgumentsInterpreter cmdLineInterpreter = new CmdLineArgumentsInterpreter();
        cmdLineInterpreter.process(args);
        File configFile = cmdLineInterpreter.getProjectConfigFile();
        
        // ******** read the project configuration file
        executer.loadConfigFromFile(configFile);
        
        // ******** setup the binary data for disassembling (read from a binary file)
        executer.setupMemory();
        
        // ******** setup the binary data for disassembling
        executer.setupDisassembler();
        
        // execute the disassembler process
        executer.getDisassembler().exec();
//        try {
//            executer.getDisassembler().exec();
//        } catch (IOException ioException) {
//            SystemOut.get().printf("%nI/O error! %s!\n\t%s%n",
//                    "FILEEEEE", ioException.getMessage());
//            System.exit(-1);
//        }
//        
    }   
    
    /**
     * Load the project configuration from given file. 
     * @param configFile the config file name
     */
    private void loadConfigFromFile(File configFile) {
        try {
            this.configFileReader.load(configFile);
        } catch (IllegalArgumentException | IOException exception) {
            SystemOut.get().printf("%nError reading configuration file %s!\n\t%s%n",
                configFile.getName(), exception.getMessage());
            System.exit(-1);
        }   
    }   
    
    /**
     * Get base file name. If the file name has 
     * @param fileName the complete file name (with path and extension)
     * @return the base file name (with path)
     */
    private static String getBaseFileName(String fileName) {
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(0, index):fileName;
    }   
}   
