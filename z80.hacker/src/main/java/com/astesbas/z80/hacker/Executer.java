package com.astesbas.z80.hacker;

import static com.astesbas.z80.hacker.util.ConfigFileProperties.ConfigKey.*;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.astesbas.z80.hacker.domain.Memory;
import com.astesbas.z80.hacker.engine.CmdLineArgumentsInterpreter;
import com.astesbas.z80.hacker.engine.Z80Disassembler;
import com.astesbas.z80.hacker.util.ConfigFileProperties;
import com.astesbas.z80.hacker.util.StringUtil;

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
    
    /** The memory (binary data) for disassembling */
    private Memory memory = new Memory();
    
    /** The Z80 disassembler */
    private Z80Disassembler z80Disassembler = new Z80Disassembler();
    
    /**
     * 
     */
    public Executer() {
        System.out.println("Z80 Hacker - Version 0.1\n");
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
    private void setupMemory(ConfigFileProperties properties) {
        
        try {
            
            // get the binary file name, start and end addresses
            Optional<String> binaryFileName = properties.getString(BINARY_FILE);
            int startAddress = properties.getAddress(BINARY_START).orElse(Memory.START_ADDRESS);
            int endAddress = properties.getAddress(BINARY_END).orElse(Memory.END_ADDRESS);
            
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
     * Set properties for disassembler read from configuration file. 
     */
    private void setupDisassembler(ConfigFileProperties properties) {
        
        try {
            // get the binary file name, start and end addresses
            String binaryFileName = properties.getString(BINARY_FILE).get();
            
            // default base name for output files (asm, lst and log)
            String baseFileName = Executer.getBaseFileName(binaryFileName);
            this.z80Disassembler.setOutputFile(properties.getString(OUTPUT_FILE).orElse(baseFileName+".asm"));
            this.z80Disassembler.setListFile(properties.getString(LIST_FILE).orElse(baseFileName+".lst"));
            this.z80Disassembler.setLogFile(properties.getString(LOG_FILE).orElse(baseFileName+".log"));
            
            // set numeric values
            this.z80Disassembler.setDbAlign(properties.getInteger(DB_ALIGN).orElse(16));
            this.z80Disassembler.setTabSize(properties.getInteger(TAB_SIZE).orElse(4));
            
            // disassembler process properties
            this.z80Disassembler.setDataLabelPrefix(properties.getString(DATA_LABEL_PREFIX).orElse(""));
            this.z80Disassembler.setNearLabelPrefix(properties.getString(NEAR_LABEL_PREFIX).orElse(""));
            this.z80Disassembler.setFarLabelPrefix(properties.getString(FAR_LABEL_PREFIX).orElse(""));
            this.z80Disassembler.setLowMem(properties.getAddress(LOW_MEM).orElse(Memory.START_ADDRESS));
            this.z80Disassembler.setHighMem(properties.getAddress(HIGH_MEM).orElse(Memory.END_ADDRESS));
            
            // setup the starting point addresses
            for(String address:properties.getListOf(START_OFF)) {
                this.z80Disassembler.pushStartAddress(Integer.decode(address));
            }   
            
            // setup the labels at given addresses
            for(String entry:properties.getListOf(LABEL)) {
                String[] split = StringUtil.splitInTwo(entry, " ");
                this.z80Disassembler.mapLabel(Integer.decode(split[1].trim()), split[0].trim());
            }   
            
            // setup the equs
            for(String entry:properties.getListOf(EQU)) {
                String[] split = StringUtil.splitInTwo(entry, " ");
                this.z80Disassembler.mapEqu(split[0].trim(), split[1].trim());
            }   
            
            // set the memory data
            this.z80Disassembler.setMemoryData(this.memory);
            
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            System.err.printf(
                String.format("Error reading numeric parameter:%n\t%s", exception.getMessage())
            );  
            System.exit(-1);
        } catch(java.util.NoSuchElementException exception) {
            System.err.printf(String.format("Missing required parameter: %s", BINARY_FILE));  
            System.exit(-1);
        }
        
        // load the Z80 op codes from resource file
        try {
            this.z80Disassembler.loadOpCodesFromFile("/z80-opcodes.dat");
        } catch (IllegalArgumentException | IOException exception) {
            System.err.printf("Error reading op-codes from op-codes.dat file!%n\t%s%n", exception.getMessage());
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
        ConfigFileProperties properties = executer.loadConfigFromFile(configFile);
        
        // ******** setup the binary data for disassembling
        executer.setupDisassembler(properties);
        
        // ******** setup the binary data for disassembling (read from a binary file)
        executer.setupMemory(properties);
        
        // ******** execute the disassembler process
        executer.getDisassembler().run();
    }   
    
    /**
     * Load the project configuration from given file. 
     * @param configFile the config file name
     * @return ConfigFileProperties properties read from configuration file
     */
    private ConfigFileProperties loadConfigFromFile(File configFile) {
        
        ConfigFileProperties configFileProperties = new ConfigFileProperties();
        
        try {
            configFileProperties.load(configFile);
        } catch (IllegalArgumentException | IOException exception) {
            System.out.printf("%nError reading configuration file %s!\n\t%s%n",
                configFile.getName(), exception.getMessage());
            System.exit(-1);
        }   
        
        return configFileProperties;
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
