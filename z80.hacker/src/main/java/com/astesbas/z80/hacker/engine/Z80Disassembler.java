package com.astesbas.z80.hacker.engine;

import static com.astesbas.z80.hacker.util.ConfigFileProperties.ConfigKey.*;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.astesbas.z80.hacker.domain.BinaryData;
import com.astesbas.z80.hacker.domain.Decoder;
import com.astesbas.z80.hacker.domain.Instruction;
import com.astesbas.z80.hacker.domain.PrefixClass;
import com.astesbas.z80.hacker.util.ConfigFileProperties;
import com.astesbas.z80.hacker.util.FileDateUtil;
import com.astesbas.z80.hacker.util.MultiMap;
import com.astesbas.z80.hacker.util.StringUtil;
import com.astesbas.z80.hacker.util.SystemOut;

/**
 * Z80 Disassembler engine.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 02/jun/2017
 */
public class Z80Disassembler implements Runnable {
    
    /**
     * Using a multi-map to group the Z80 instructions into four main groups:
     *     1) Opcodes starting with "CB" byte (bit and rotation ops)
     *     2) Opcodes starting with "DD" byte (almost all IX reg related ops)
     *     3) Opcodes starting with "FD" byte (almost all IY reg related ops)
     *     4) All other opcodes that does not fall in the groups above
    */
    private final MultiMap<PrefixClass, Instruction> instructionsMap = new MultiMap<>();
    
    /** List containing the start points (address) in memory to be disassembled */
    private final List<Integer> startOffList = new  ArrayList<>();
    
    /** The log (.log) file path */
    private Path logPath = Paths.get("./output.log");
    
    /** The output (.asm) file path */
    private Path outputPath = Paths.get("./output.asm");
    
    /** The list (.lst) output file path */
    private Path listPath = Paths.get("./output.lst");
    
    /** The binary data decoder (manages the code and data to be disassembled) */
    private final Decoder decoder;
    
    /** The output processor. Generates assembly source code and list files */
    private final OutputProcessor outputProcessor;
    
    /** List of prefixes of JP instructions that uses pointers to get the address of the jump (e.g. JP (IX)) */
    private static final List<String> INDEXED_JP_PREFIX = Arrays.asList("DD", "E9", "FD");
    
    /** The warning flag */
    private boolean hasWarnings = false;
    
    /** The Z80 instructions file */
    private static final String Z80_INSTRUCTIONS_FILE_NAME = "/z80-instructions-extended.dat";
    
    /**
     * Z80 decoder constructor.
     * The binary data to be disassembled must be provided and cannot be changed.
     */
    public Z80Disassembler(BinaryData binaryData) {
        this.decoder = new Decoder(binaryData);
        this.outputProcessor = new OutputProcessor();
    }   
    
    /**
     * Sets the log file path.
     * @param logFileName the logFile name to set
     */
    public void setLogFile(String logFileName) {
        this.logPath = FileDateUtil.getFilePath(logFileName);
    }   
    
    /**
     * Sets the output file name (disassembled code)
     * @param outputFileName
     *            the output file name
     */
    public void setOutputFile(String outputFileName) {
        this.outputPath = FileDateUtil.getFilePath(outputFileName);
    }   
    
    /**
     * Sets the list file path.
     * @param listFileName
     *            the listFile name
     */
    public void setListFile(String listFileName) {
        this.listPath = FileDateUtil.getFilePath(listFileName);
    }   
    
    /**
     * Pushes a starting address point for disassembler process.
     * @param address the start address for disassembling
     */
    public boolean pushStartAddress(Integer address) {
        if(this.decoder.isValidAddress(address)) {
            return this.startOffList.add(address);
        }   
        return false;
    }   
    
    /**
     * Return the next start address point for disassembling.
     * @return the start address for disassembling
     */
    private Integer popStartAddress() {
        return this.startOffList.isEmpty() ? null:this.startOffList.remove(0);
    }   
    
    /**
     * Return the warning status of disassembler process.
     * @return true if there are any warnings
     */
    public boolean hasWarnings() {
        return this.hasWarnings;
    }
    
    /**
     * Output text to log file.
     * @param format the string formatter
     * @param args the parameters for the log formatter
     */
    private void log(String format, Object... args) {
        try (BufferedWriter writer = Files.newBufferedWriter(this.logPath, CREATE, APPEND)) {
            writer.write(String.format(format, args));
        } catch (IOException ioException) {
            System.err.format("IOException: %s%n", ioException);
            System.exit(-1);
        }   
    }   
    
    /**
     * Output warning text to log file.
     * @param format the string formatter
     * @param args the parameters for the log formatter
     */
    private void warn(String format, Object... args) {
        this.hasWarnings = true;
        this.log(format, args);
    }   
    
    /**
     * Output text to log file and default output stream.
     * @param format the string formatter
     * @param args the parameters for the log formatter
     */
    private void systemOutAndLog(String format, Object... args) {
        this.log(format, args);
        System.out.printf(format, args);
    }   
    
    @Override
    public void run() {
        
        this.systemOutAndLog("Starting disassembler process at %s%n", FileDateUtil.getCurrentTime());
        this.hasWarnings = false;
        
        /** The list of disassembled instructions (output) */
        BinaryData binaryData = this.decoder.getBinaryData();
        
        while(!this.startOffList.isEmpty()) {
            
            int startAddress = this.popStartAddress();
            binaryData.setPointer(startAddress);
            
            // Verify if the current start-off address was already processed
            if(!this.decoder.isDbByte(startAddress)) {
                
                // If the current position holds a parameter byte, then map the label with displacement
                // and log a warning message
                if(this.decoder.isParameterByte(startAddress)) {
                    
                    this.warn("Warning: The start-off address 0x%X conflicts with instruction's data!%n", startAddress);
                    
                    int instructionAddress = this.decoder.getStartAddressOfInstructionAt(startAddress);
                    this.outputProcessor.mapCodeLabel(instructionAddress);
                    this.outputProcessor.mapOffsetCodeLabel(startAddress, startAddress-instructionAddress);
                    
                } else {
                    this.outputProcessor.mapCodeLabel(startAddress);
                }   
                
                continue;
            }   
            
            this.log("Processing start-off address: 0x%X%n", startAddress);
            this.outputProcessor.mapCodeLabel(startAddress);
            
            // Keep disassembling the binary data until at least one of the stop conditions is satisfied 
            do {
                
                // Store the current instruction pointer (this pointer is updated for every instruction processed)
                int instructionAddress = binaryData.getPointer();
                
                // If the current instruction is not a db value, then it is a processed instruction.
                // In this case, process the next start-off address
                if(!this.decoder.isDbByte(instructionAddress)) {
                    break;
                }   
                
                // Find the matching instruction for the next bytes in the binary data.
                // This search method could be optimized, but for this application it is ok!
                Optional<Instruction> match = this.findMatchingInstruction(instructionAddress);
                if(!match.isPresent()) {
                    // The next sequence of bytes does not defines a valid instruction.
                    // Keep the current byte as a "db ##" and go to the next address
                    binaryData.incrementPointer();
                    continue;
                }   
                
                Instruction instruction = match.get();
                byte[] bytes = binaryData.getBytes(instruction.getSize());
                
                // Verify if the current instruction "overlaps" an existing (processed) one.
                // If so, then stop the process, log a warning message and go to the next start-off address
                if(!this.decoder.isAvailable(instructionAddress, instruction.getSize())) {
                    this.warn("Warning: \"Ovelapping\" instruction at address 0x%X%n", instructionAddress);
                    break;
                }   
                
                // Set the instruction and update the binary data pointer 
                this.decoder.setInstruction(instructionAddress, instruction);
                
                String mnemonicMask = instruction.getMnemonicMask();
                String opCode = String.format("%02X", binaryData.get());
                binaryData.incrementPointer(bytes.length);
                
                // Process "special" instructions that may generate a new start-off address
                // or end the current disassembling thread
                if(mnemonicMask.equals("RET") || !this.decoder.isValidAddress(binaryData.getPointer())) {
                    
                    // A RET instruction was found or the end of binary data was reached
                    break;
                    
                } else if(mnemonicMask.contains("CALL") || (mnemonicMask.contains("JP"))) {
                    
                    // Verify if the JP instruction is indexed by a register.
                    // If this is the case, the resulting address of the jump is unavailable
                    if (INDEXED_JP_PREFIX.contains(opCode)) {
                        this.warn("Warning: Found indexed jump instruction at address 0x%X%n", instructionAddress);
                        break;
                    }   
                    
                    // Evaluate the call/jp address and push to start-off list
                    int address = (bytes[1] & 0xFF) | ((bytes[2] << 8) & 0xFFFF);
                    this.pushStartAddress(address);
                    
                    // For unconditional jump, the current disassembling thread must be ended
                    if (opCode.equals("C3")) {
                        break;
                    }   
                    
                } else if (mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
                    
                    // Evaluate the absolute address from relative jump and push it to start-off list
                    int address = instructionAddress + (bytes[1] + 2);
                    this.pushStartAddress(address);
                    
                    // For unconditional relative jump, the current disassembling thread must be ended
                    if (opCode.equals("18")) {
                        break;
                    }   
                }   
                
            } while(true);
        }   
        
        // Post processing: add the data labels references
        this.processDataLabels();
        
        this.systemOutAndLog("Disassembler process finished at %s%n", FileDateUtil.getCurrentTime());
        if(this.hasWarnings) {
            System.out.println("There are warnings. See log file for more information!");
        }   
        
        // Process output files
        this.outputProcessor.processOutputSourceFile(this.outputPath, this.decoder);
        this.outputProcessor.processOutputListFile(this.listPath, this.decoder);
    }   
    
    /**
     * Post processing: add the data labels
     */
    private void processDataLabels() {
        
        // Post processing: add the data labels references
        List<Instruction> instructionsList = this.decoder.getInstructionsList();
        BinaryData binaryData = this.decoder.getBinaryData();
        
        int address = this.decoder.getStartAddress();
        Instruction instruction = instructionsList.get(address);
        
        while(address < this.decoder.getEndAddress()) {
            
            Instruction nextInstruction = instructionsList.get(address+1);
            
            if(!instruction.isDbByte() && nextInstruction.isDbByte()) {
                this.outputProcessor.mapDataLabel(address+1);
            } else if (instruction.getMnemonicMask().contains("LD") && instruction.hasWordParameter()) {
                
                byte[] bytes = binaryData.getBytes(address, instruction.getSize());
                int labelAddress = (bytes[1] & 0xFF) | ((bytes[2] << 8) & 0xFFFF);
                Instruction targetInstruction = instructionsList.get(labelAddress);
                
                if(this.decoder.isValidAddress(labelAddress) && targetInstruction.isDbByte()) {
                    this.outputProcessor.mapDataLabel(labelAddress);
                }   
            }   
            
            instruction = nextInstruction;
            address++;
        }   
    }   
    
    /**
     * Find the matching instruction at the given address.
     * @param address the address of binary data
     * @return the matching instruction
     */
    private Optional<Instruction> findMatchingInstruction(int address) {
        
        BinaryData binaryData = this.decoder.getBinaryData();
        PrefixClass prefixClass = PrefixClass.of(binaryData.get(address));
        
        for(Instruction instruction:this.instructionsMap.get(prefixClass)) {
            byte[] bytes = binaryData.getBytes(instruction.getSize());
            if(instruction.matches(bytes)) {
                return Optional.of(instruction);
            }   
        }   
        
        return Optional.empty();
    }   
    
    /**
     * Set properties for disassembler read from configuration file.
     */
    public void setProperties(ConfigFileProperties properties) {
        
        try {
            
            try {
                // Get the binary file name and default base name for output files (asm, lst and log)
                String binaryFileName = properties.getString(BINARY_FILE).get();
                String baseFileName = FileDateUtil.getBaseFileName(binaryFileName);
                
                this.setOutputFile(properties.getString(OUTPUT_FILE).orElse(baseFileName+".asm"));
                this.setListFile(properties.getString(LIST_FILE).orElse(baseFileName+".lst"));
                this.setLogFile(properties.getString(LOG_FILE).orElse(baseFileName+".log"));
                
            } catch(java.util.NoSuchElementException exception) {
                System.err.printf(String.format("Missing required parameter: %s", BINARY_FILE));
                System.exit(-1);
            }   
            
            // Set the formatting output properties
            this.outputProcessor.setDbAlign(properties.getInteger(DB_ALIGN).orElse(16));
            this.outputProcessor.setTabSize(properties.getInteger(TAB_SIZE).orElse(4));
            this.outputProcessor.setCodeLabelPrefix(properties.getString(CODE_LABEL_PREFIX).orElse(""));
            this.outputProcessor.setDataLabelPrefix(properties.getString(DATA_LABEL_PREFIX).orElse(""));
            StringUtil.setHexValueFormat(properties.getString(HEX_FORMAT).orElse("0%sH"));
            
            // Set the user defined labels at given addresses
            for (String entry : properties.getListOf(LABEL)) {
                String[] split = StringUtil.splitInTwo(entry.replaceAll("\t", " "), " ");
                if(split.length > 1) {
                    this.outputProcessor.mapLabel(Integer.decode(split[1].trim()), split[0].trim());
                } else {
                    throw new IllegalArgumentException(String.format("Invalid label entry: [%s]%n", entry));
                }   
            }   
            
            // Set the user defined equs
            for (String entry : properties.getListOf(EQU)) {
                String[] split = StringUtil.splitInTwo(entry.replaceAll("\t", " "), " ");
                if(split.length > 1) {
                    this.outputProcessor.mapEqu(split[0].trim(), split[1].trim());
                } else {
                    throw new IllegalArgumentException(String.format("Invalid equ entry: [%s]%n", entry));
                }   
            }   
            
            // Set disassembler limits
            this.decoder.setStartAddress(properties.getAddress(START_ADDRESS).orElse(BinaryData.START_ADDRESS));
            this.decoder.setEndAddress(properties.getAddress(END_ADDRESS).orElse(BinaryData.END_ADDRESS));
            
            // Set the status of the flag to output source using undocumented Z80 instructions
            boolean loadUndocumentedInstructions = properties.getBoolean(UNDOCUMENTED_INSTRUCTIONS).orElse(false);
            
            // Load the Z80 instructions set from resource file
            try {
                this.loadInstructionsFromFile(Z80_INSTRUCTIONS_FILE_NAME, loadUndocumentedInstructions);
            } catch (IllegalArgumentException | IOException exception) {
                System.err.printf("Error reading data from %s file!%n\t%s%n",
                    Z80_INSTRUCTIONS_FILE_NAME, exception.getMessage());
                System.exit(-1);
            }   
            
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            System.err.printf(
                String.format("Error reading config file parameter:%n\t%s", exception.getMessage())
            );  
            System.exit(-1);
        }   
        
        // Set the starting point addresses
        for (String address : properties.getListOf(START_OFF)) {
            try {
                this.pushStartAddress(Integer.decode(address));
            } catch (NumberFormatException nfe) {
                System.err.printf(
                    String.format("Error setting start-off address parameter: %s%n\t%s", address, nfe.getMessage())
                );  
                System.exit(-1);
            }   
        }   
    }   
    
    /**
     * Loads the Z80 instruction's data from file (binary and mnemonic representations).
     * 
     * @param fileName the instruction file name
     * @param loadUndocumented flag to indicate the loading of undocumented Z80 instructions
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    private synchronized void loadInstructionsFromFile(String fileName, boolean loadUndocumented)
            throws IOException, IllegalArgumentException {
        InputStream stream = this.getClass().getResourceAsStream(fileName);
    	this.loadInstructionsFromStream(stream, loadUndocumented);
    }   
    
    /**
     * Loads the Z80 instructions data from input stream (patterns and attributes).
     * 
     * @param inputStream the input stream
     * @param loadUndocumented flag to indicate the loading of undocumented Z80 instructions
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    private synchronized void loadInstructionsFromStream(InputStream inputStream, boolean loadUndocumented)
            throws IOException, IllegalArgumentException {
        
        String line;
        int lineNumber = 0;
        int instructionsCounter = 0;
        
        // InputStreamReader reads bytes and decodes them into characters using a specified charset
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            System.out.printf("Loading Z80 instructions information from resource file...");
            
            while ((line = bufferedReader.readLine()) != null) {
                
                lineNumber++;
                
                // Discard comment lines and empty lines
                line = StringUtil.clean(line, '\'');
                if (line.isEmpty()) {
                    continue;
                }   
                
                // Get the instruction's binary matcher and the mnemonic string 
                String lineSplit[] = line.split(":");
                if (lineSplit.length > 1) {
                    
                    // get the byte/mnemonic masks
                    String byteMask = lineSplit[0].trim();
                    String mnemonicMask = lineSplit[1].trim();
                    
                    // Create the instruction and map it according to the prefix class
                    Instruction instruction = new Instruction(byteMask, mnemonicMask);
                    if(!(instruction.isUndocumented() && !loadUndocumented)) {
                        this.instructionsMap.map(instruction.getPrefixClass(), instruction);
                        instructionsCounter++;
                    }   
                    
                } else {
                    System.out.printf("Error!%n");
                    throw new IllegalArgumentException(
                        String.format("Error processing instruction \"%s\" at line %d%n", line, lineNumber)
                    );  
                }   
            }   
            
            System.out.printf("Ok%n");
            SystemOut.vprintf("Total of instructions read: %d\n", instructionsCounter);
            
        } catch(NullPointerException ioException) {
            //  a NullPointerException may occur if the z80-instructions.dat cannot be found in the classpath
            throw new IOException("System could not find the Z80 instructions resource file in the classpath!");
        }   
    }   
    
}