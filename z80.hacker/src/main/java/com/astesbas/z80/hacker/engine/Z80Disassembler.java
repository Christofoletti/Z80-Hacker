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
    private Decoder decoder;
    
    /** The output processor. Generates assembly source code and list files */
    private final OutputProcessor outputProcessor = new OutputProcessor();
    
    /** List of prefixes of JP instructions that uses pointers to get the address of the jump (e.g. JP (IX)) */
    private static final List<String> INDEXED_JP_PREFIX = Arrays.asList("DD", "E9", "FD");
    
    /**
     * Z80 Disassembler constructor.
     */
    public Z80Disassembler(BinaryData binaryData) {
        this.decoder = new Decoder(binaryData);
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
    public void pushStartAddress(Integer address) {
        if(this.decoder.isValidAddress(address)) {
            this.startOffList.add(address);
        }   
    }   
    
    /**
     * Return the next start address point for disassembling.
     * @return the start address for disassembling
     */
    private Integer popStartAddress() {
        return this.startOffList.isEmpty() ? null:this.startOffList.remove(0);
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
    
    @Override
    public void run() {
        
        this.log("Starting disassembler process at %s%n", FileDateUtil.getCurrentTime());
        
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
                    
                    this.log("Warning: The start-off address 0x%X conflicts with instruction's data!%n", startAddress);
                    
                    int instructionAddress = this.decoder.getStartAddressOfInstructionAt(startAddress);
                    this.outputProcessor.mapCodeLabel(instructionAddress);
                    this.outputProcessor.mapOffsetCodeLabel(startAddress, instructionAddress-startAddress);
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
                    // Keep the current byte as a "db ##" and go to the next memoy address
                    System.out.printf("%04X: db 0%XH%n", instructionAddress, binaryData.get());
                    binaryData.incrementPointer();
                    continue;
                }   
                
                Instruction instruction = match.get();
                String opCode = String.format("%02X", binaryData.get());
                byte[] bytes = binaryData.getBytes(instruction.getSize());
                
                // Verify if the current instruction "overlaps" an existing (processed) one.
                // If so, then stop the process, log a warning message and go to the next start-off address
                if(!this.decoder.isAvailable(instructionAddress, instruction.getSize())) {
                    this.log("Warning: \"Ovelapping\" instruction at address 0x%X%n", instructionAddress);
                    break;
                }   
                
                SystemOut.vprintf("%04X: %02X %s%n", instructionAddress, bytes[0], instruction.translate(bytes));
                
                // Set the instruction and update the binary data pointer 
                this.decoder.setInstruction(instructionAddress, instruction);
                binaryData.incrementPointer(bytes.length);
                
                // Process "special" instructions that may generate a new start-off address or end the current
                // disassembling thread
                String mnemonicMask = instruction.getMnemonicMask();
                if(mnemonicMask.equals("RET") || !this.decoder.isValidAddress(binaryData.getPointer())) {
                    
                    // A RET instruction was found or the end of memory was reached
                    break;
                    
                } else if(mnemonicMask.contains("CALL") || (mnemonicMask.contains("JP"))) {
                    
                    // Verify if the JP instruction is indexed by a register.
                    // If this is the case, the resulting address of the jump is unknown
                    if (INDEXED_JP_PREFIX.contains(opCode)) {
                        this.log("Warning: Found indexed jump instruction at address 0x%X%n", instructionAddress);
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
                    
                    // Evaluate the absolute address from relative jump and push  it to start-off list
                    int address = instructionAddress + (bytes[1] + 2);
                    this.pushStartAddress(address);
                }   
                
            } while(true);
            
        }   
        
        // Post processing: add the data labels references
        int address = this.decoder.getStartAddress();
        List<Instruction> instructionsList = this.decoder.getInstructionsList();
        Instruction instruction = instructionsList.get(address);
        while(address++ < this.decoder.getEndAddress()) {
            Instruction nextInstruction = instructionsList.get(address);
            if(!instruction.equals(Decoder.DB_BYTE) && nextInstruction.equals(Decoder.PARAMTER)) {
                this.outputProcessor.mapDataLabel(address);
            }   
            instruction = nextInstruction;
        }   
        
        this.log("Disassembler process finished at %s%n", FileDateUtil.getCurrentTime());
        
        // Process output files
        //this.processOutputSourceFile(instructionsList);
        this.outputProcessor.processOutputListFile(this.listPath, this.decoder);
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

//    /**
//     * 
//     * @param instructionsList
//     */
//    private void processOutputSourceFile(List<Instruction> instructionsList) {
//        
//        try (BufferedWriter writer = Files.newBufferedWriter(this.outputPath, CREATE, APPEND)) {
//            
//            writer.write("; **** Generated by Z80 Hacker disassembler tool - version 0.1 ****\n");
//            writer.write("; \n");
//            writer.write("; Author: Luciano M. Christofoletti\n");
//            writer.write("; www.christofoletti.com.br\n");
//            writer.write(String.format("; Date: %s%n", java.time.LocalDateTime.now()));
//            Optional<String> binaryFileName = this.binaryData.getBinaryFileName();
//            if(binaryFileName.isPresent()) {
//                writer.write(String.format("; Input file: %s%n", binaryFileName.get()));
//            }   
//            
//            // Write out the equ mapping keys/values
//            writer.write(String.format("%"+this.tabSize+ "s%n", ""));
//            for(Entry<String, String> equMapEntry:this.equsMap.entrySet()) {
//                writer.write(String.format("%-12s EQU %s%n", equMapEntry.getValue()+":", equMapEntry.getKey()));
//            }   
//            
//            // Write the ORG directive
//            writer.write(String.format("%n%"+this.tabSize+"sORG %s%n%n", "", StringUtil.intToHexString(this.lowMem)));
//            
//            // writes all instructions from disassembled memory
//            for(int address = this.lowMem; address <= this.highMem;) {
//                
//                // get the current instruction
//                Instruction instruction = instructionsList.get(address);
//                String mnemonicMask = instruction.getMnemonicMask();
//                String opCodeMask = instruction.getByteMask();
//                byte[] bytes = this.binaryData.getBytes(address, instruction.getSize());
//                
//                // write label, if applicable
//                String label = this.labelsMap.get(address);
//                if(label != null) {
//                    writer.write(String.format("%n%s:%n", label));
//                }   
//                
//                // process instruction opcode
//                if(!instruction.equals(DB_BYTE)) {
//                    
//                    // output byte sequence and mnemonic of current instruction
//                    if(mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
//                        
//                        int nearAddress = address + (bytes[1] + 2);
//                        String nearLabel = this.labelsMap.get(nearAddress);
//                        if(nearLabel == null) {
//                            nearLabel = StringUtil.intToHexString(nearAddress);
//                        }   
//                        
//                        writer.write(String.format(
//                            "%"+this.tabSize+ "s%s%n", "", instruction.translate(bytes, nearLabel))
//                        );  
////                        writer.write(instruction.translate(bytes, jumpLabel));
////                        writer.newLine();
//                        
//                    } else if(mnemonicMask.contains("CALL") || 
//                             (mnemonicMask.contains("JP") && bytes.length > 2) ||
//                             (mnemonicMask.contains("LD") && opCodeMask.contains("####"))) {
//                        
//                        // Evaluate the two bytes address (for prefixed instructions, the address bytes are
//                        // shifted one byte ahead)
//                        int farAddress = instruction.getPrefixClass().equals(PrefixClass.$$) ?
//                                (bytes[1] & 0xFF) | ((bytes[2] << 8) & 0xFFFF):
//                                (bytes[2] & 0xFF) | ((bytes[3] << 8) & 0xFFFF);
//                        
//                        // First, tries to get the label from mapped labels 
//                        String farLabel = this.labelsMap.get(farAddress);
//                        if(farLabel == null) {
//                            farLabel = StringUtil.intToHexString(farAddress);
//                            // verify if there is a equ definition for the "translated" address
//                            if(this.equsMap.containsKey(farLabel)) {
//                                farLabel = this.equsMap.get(farLabel);
//                            }   
//                        }   
//                        
//                        writer.write(String.format(
//                            "%"+this.tabSize+ "s%s%n", "", instruction.translate(bytes, farLabel))
//                        );  
//                        
//                    } else {
//                        
//                        //writer.write(String.format("%s%n", instruction.translate(bytes)));
//                        writer.write(String.format(
//                            "%"+this.tabSize+ "s%s%n", "", instruction.translate(bytes))
//                        );  
//                    }   
//                    
//                    // update the current memory address
//                    address += instruction.getSize();
//                    
//                } else {
//                    
//                    // write the start of data line (db directive plus byte data)
//                    //writer.newLine();
//                    byte data = this.binaryData.get(address++);
//                    writer.write(String.format(
//                        "%"+this.tabSize+ "sdb %4s", "", StringUtil.byteToHexString(data))
//                    );  
//                    
//                    int byteCounter = 0;
//                    while(instructionsList.get(address).equals(DB_BYTE)) {
//                        
//                        // verify if there is a label at the current byte address
//                        // if so, then go to the next line, set the label e restart the db section
//                        if(this.labelsMap.get(address) != null) {
//                            break;
//                        }   
//                        
//                        // get byte from current memory address
//                        data = this.binaryData.get(address);
//                        writer.write(String.format(", %4s", StringUtil.byteToHexString(data)));
//                        
//                        // output max of db align bytes per line
//                        if(++address > this.highMem || ++byteCounter > this.dbAlign) {
//                            break;
//                        }   
//                    }   
//                    
//                    // goto to the next line :P
//                    writer.newLine();
//                }   
//            }   
//            
//        } catch (IOException ioException) {
//            System.err.format("Error writing output source file: %s%n", ioException.getMessage());
//            System.exit(-1);
//        }   
//        
//        
//    }   
    
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
            
            // Setup the formatting output properties
            this.outputProcessor.setDbAlign(properties.getInteger(DB_ALIGN).orElse(16));
            this.outputProcessor.setTabSize(properties.getInteger(TAB_SIZE).orElse(4));
            this.outputProcessor.setCodeLabelPrefix(properties.getString(CODE_LABEL_PREFIX).orElse(""));
            this.outputProcessor.setDataLabelPrefix(properties.getString(DATA_LABEL_PREFIX).orElse(""));
            
            // Setup the user defined labels at given addresses
            for (String entry : properties.getListOf(LABEL)) {
                String[] split = StringUtil.splitInTwo(entry, " ");
                this.outputProcessor.mapLabel(Integer.decode(split[1].trim()), split[0].trim());
            }   
            
            // Setup the user defined equs
            for (String entry : properties.getListOf(EQU)) {
                String[] split = StringUtil.splitInTwo(entry, " ");
                this.outputProcessor.mapEqu(split[0].trim(), split[1].trim());
            }   
            
            // Setup disassembler limits
            this.decoder.setStartAddress(properties.getAddress(START_ADDRESS).orElse(BinaryData.START_ADDRESS));
            this.decoder.setEndAddress(properties.getAddress(END_ADDRESS).orElse(BinaryData.END_ADDRESS));
            
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            System.err.printf(
                String.format("Error reading config file parameter:%n\t%s", exception.getMessage())
            );  
            System.exit(-1);
        }   
        
        // Setup the starting point addresses
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
        
        // load the Z80 instructions set from resource file
        try {
            this.loadInstructionsFromFile("/z80-instructions.dat");
        } catch (IllegalArgumentException | IOException exception) {
            System.err.printf("Error reading data from z80-instructions.dat file!%n\t%s%n", exception.getMessage());
            System.exit(-1);
        }   
    }   
    
    /**
     * Get input stream resource for given file name.
     * @param fileName name of the resource
     * @return input stream resource
     */
    public InputStream getResourceAsStream(String fileName) {
        return this.getClass().getResourceAsStream(fileName);
    }   
    
    /**
     * Loads the Z80 instruction's data from file (binary and mnemonic representations).
     * 
     * @param fileName the instruction file name
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    public synchronized void loadInstructionsFromFile(String fileName)
            throws IOException, IllegalArgumentException {
        InputStream stream = this.getResourceAsStream(fileName);
    	this.loadInstructionsFromStream(stream);
    }   
    
    /**
     * Loads the Z80 instructions data from input stream (patterns and attributes).
     * 
     * @param inputStream the input stream
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    private synchronized void loadInstructionsFromStream(InputStream inputStream)
            throws IOException, IllegalArgumentException {
        
        String line;
        int lineNumber = 0;
        int instructionsCounter = 0;
        
        // InputStreamReader reads bytes and decodes them into characters using a specified charset
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            SystemOut.vprint("Loading Z80 instructions information from resource file...");
            
            while ((line = bufferedReader.readLine()) != null) {
                
                lineNumber++;
                
                // Discard comment lines and empty lines
                line = StringUtil.clean(line, ';');
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
                    this.instructionsMap.map(instruction.getPrefixClass(), instruction);
                    instructionsCounter++;
                    
                } else {
                    SystemOut.vprintln("Error!");
                    throw new IllegalArgumentException(
                        String.format("Error processing instruction \"%s\" at line %d%n", line, lineNumber)
                    );  
                }   
            }   
            
            SystemOut.vprintln("Ok");
            SystemOut.vprintf("Total of instructions read: %d\n", instructionsCounter);
            
        } catch(NullPointerException ioException) {
            //  a NullPointerException may occur if the z80-instructions.dat cannot be found in the classpath
            throw new IOException("System could not find the Z80 instructions resource file in the classpath!");
        }   
    }   
    
}