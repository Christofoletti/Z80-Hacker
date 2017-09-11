package com.astesbas.z80.hacker.engine;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.astesbas.z80.hacker.domain.Memory;
import com.astesbas.z80.hacker.domain.OpCode;
import com.astesbas.z80.hacker.domain.OpCodeClass;
import com.astesbas.z80.hacker.util.MultiMap;
import com.astesbas.z80.hacker.util.StringUtil;
import com.astesbas.z80.hacker.util.SystemOut;

/**
 * Z80 Disassembler.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * 
 * 10%%:DJNZ %%
 * 18%%:JR %%
 * 20%%:JR NZ,%%
 * 28%%:JR Z,%%
 * 30%%:JR NC,%%
 * 38%%:JR C,%%
 * 
 * C0:RET NZ
 * C8:RET Z
 * C9:RET
 * D0:RET NC
 * D8:RET C
 * E0:RET PO
 * E8:RET PE
 * ED45:RETN
 * ED4D:RETI
 * ED55:RETN
 * ED5D:RETN
 * ED65:RETN
 * ED6D:RETN
 * ED75:RETN
 * ED7D:RETN
 * F0:RET P
 * F8:RET M
 * 
 * C2####:JP NZ,####
 * C3####:JP ####
 * CA####:JP Z,####
 * D2####:JP NC,####
 * DDE9:JP (IX)
 * E2####:JP PO,####
 * DA####:JP C,####
 * E9:JP (HL)
 * EA####:JP PE,####
 * F2####:JP P,####
 * FA####:JP M,####
 * FDE9:JP (IY)
 * 
 * C4####:CALL NZ,####
 * CC####:CALL Z,####
 * CD####:CALL ####
 * D4####:CALL NC,####
 * DC####:CALL C,####
 * E4####:CALL PO,####
 * EC####:CALL PE,####
 * F4####:CALL P,####
 * FC####:CALL M,####

 * @since 02/jun/2017
 */
public class Z80Disassembler implements Runnable {
    
    /**
     * Using a multi-map to group the Z80 opcodes into four main groups:
     *     1) Opcodes starting with "CB" byte (bit and rotation ops)
     *     2) Opcodes starting with "DD" byte (almost all IX reg related ops)
     *     3) Opcodes starting with "FD" byte (almost all IY reg related ops)
     *     4) All other opcodes that does not fall in the groups above
    */
    private final MultiMap<OpCodeClass, OpCode> opCodesMap = new MultiMap<>();
    
    /** List containing the start points (address) in memory to be disassembled */
    private final List<Integer> startOffList = new  ArrayList<>();
    
    /** Mapping for labels entries at given addresses */
    private final Map<Integer, String> labelsMap = new HashMap<>();
    
    /** Mapping of EQU entries */
    private final Map<String, String> equsMap = new HashMap<>();
    
    /** The output (.asm) file path */
    private Path outputPath = Paths.get("./output.asm");
    
    /** The list (.lst) output file path */
    private Path listPath = Paths.get("./output.lst");
    
    /** The log (.log) file path */
    private Path logPath = Paths.get("./output.log");
    
    /** The memory to be disassembled (binary data) */
    private Memory memory = new Memory();
    
    /** Number of byte per line for "db" directives */
    private int dbAlign = 16;
    
    /** Tabulation size to be inserted before instructions */
    private int tabSize = 4;
    
    /** Near label prefix: used as prefix for jr/djnz labels */
    private String nearLabelPrefix = "";
    
    /** Far label prefix: used as prefix for call/jp labels */
    private String farLabelPrefix = "";
    
    /** The low address to be disassembled - addresses smaller than this will not be processed */
    private int lowMem = Memory.START_ADDRESS;
    
    /** The high address to be disassembled - addresses greater than this will not be processed */
    private int highMem = Memory.END_ADDRESS;
    
    // The data byte representation (used only as placeholders for unprocessed/reserved byte positions))
    private static final OpCode DB_BYTE = new OpCode("##", "db ##"); // "unprocessed" bytes will be output as "db ##"
    private static final OpCode BYTE_DATA = new OpCode("##", "[##]"); // placeholder for "data bytes" of instructions
    
    /** List of prefixes of JP instructions that uses pointers to get the address of the jump (e.g. JP (IX)) */
    private static final List<String> INDEXED_JP_PREFIX = Arrays.asList("DD", "E9", "FD");
    
    /**
     * Z80 Disassembler constructor.
     */
    public Z80Disassembler() {
    }   
    
    /**
     * Maps a label to be used as reference in the disassembled code.
     * If the address is already maps a label, then the new mapping is discarded.
     * 
     * @param address the address to be mapped to the given label
     * @param label the key label
     */
    public void mapLabel(Integer address, String label) {
        if(!this.labelsMap.containsKey(address)) {
            this.labelsMap.put(address, label);
        }   
    }   
    
    /**
     * Maps a label to be used as reference in the disassembled code. The label
     * is defined as the concatenation of the prefeix and start addess.
     * 
     * If the address is already maps a label, then the new mapping is discarded.
     * @param prefix
     * @param address
     */
    private void mapAddressLabel(String prefix, Integer address) {
        this.mapLabel(address, String.format("%s%s", prefix, StringUtil.intToHexString(address)));  
    }   
    
    /**
     * 
     * @param address
     */
    private void removeAddressLabel(Integer address) {
        this.labelsMap.remove(address);  
    }   
    
    /**
     * Maps an EQU directive.
     * @param label the key label
     * @param value the value to be mapped to the given equ label
     */
    public void mapEqu(String label, String value) {
        this.equsMap.put(label, value);
    }   
    
    /**
     * Sets the output file name (disassembled code)
     * @param outputFileName the output file name
     */
    public void setOutputFile(String outputFileName) {
        this.outputPath = this.getFilePath(outputFileName);
    }   
    
    /**
     * Sets the list file path.
     * @param listFileName the listFile name
     */
    public void setListFile(String listFileName) {
        this.listPath = this.getFilePath(listFileName);
    }   
    
    /**
     * Sets the log file path.
     * @param logFileName the logFile name to set
     */
    public void setLogFile(String logFileName) {
        this.logPath = this.getFilePath(logFileName);
    }   
    
    /**
     * Get the Path for the given file name.
     * @param fileName the file
     * @return the path for the file name
     */
    public Path getFilePath(String fileName) {
        Path path = null;
        try {
            path = Paths.get(fileName);
            Files.deleteIfExists(path);
        } catch (IOException | InvalidPathException exception) {
            System.err.printf("%nError getting path to file: \"%s\"\n\t%s%n", fileName, exception.getMessage());
            System.exit(-1);
        }   
        return path;
    }   
    
    /**
     * Sets the memory to be disassembled.<br/>
     * The memory reference is stored instead of copying the original memory to an internal object.
     * This is not a problem since the memory data will not be changed.
     * 
     * @param memory the memory to set
     */
    public void setMemoryData(Memory memory) {
        this.memory = Objects.requireNonNull(memory);
    }      
    
    /**
     * Set the data align size (maximum of 64).
     * 
     * @param dbAlign the dbAlign to set
     * @throws IllegalArgumentException if the given value is outside the permitted range
     */
    public void setDbAlign(int dbAlign) throws IllegalArgumentException {
        try {
            this.dbAlign = validateRange(dbAlign, 1, 64);
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid db align value!\n" + exception.getMessage());
        }   
    }   
    
    /**
     * Set the tabulation size (maximum of 64).
     * 
     * @param tabSize the tabSize to set
     * @throws IllegalArgumentException if the given value is outside the permitted range
     */
    public void setTabSize(int tabSize) throws IllegalArgumentException  {
        try {
            this.tabSize = validateRange(tabSize, 0, 32);
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid tab size value!\n" + exception.getMessage());
        }   
    }   
    
    /**
     * @param nearLabelPrefix the near label prefix
     */
    public void setNearLabelPrefix(String nearLabelPrefix) {
        this.nearLabelPrefix = Objects.requireNonNull(nearLabelPrefix);
    }   
    
    /**
     * @param farLabelPrefix the far label prefix
     */
    public void setFarLabelPrefix(String farLabelPrefix) {
        this.farLabelPrefix = Objects.requireNonNull(farLabelPrefix);
    }   
    
    /**
     * Set the low memory address. The address must be between Memory.START_ADDRESS and high memory.
     * @param lowMem the lowMem to set
     */
    public void setLowMem(int lowMem) {
        try {
            this.lowMem = validateRange(lowMem, Memory.START_ADDRESS, this.highMem);
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid lower memory address paramter!\n" + exception.getMessage());
        }   
    }   
    
    /**
     * Set the low memory address. The address must be between low memory and Memory.END_ADDRESS.
     * @param highMem the highMem to set
     */
    public void setHighMem(int highMem) {
        try {
            this.highMem = validateRange(highMem, this.lowMem, Memory.END_ADDRESS);
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid high memory address paramter!\n" + exception.getMessage());
        }   
    }   
    
    /**
     * 
     * @param address the address to verify
     */
    private boolean isInMemoryRange(int address) {
        return (address >= this.lowMem && address <= this.highMem);
    }
    
    /**
     * Pushes a starting address point for disassembler process.
     * @param address the start address for disassembling
     */
    public void pushStartAddress(Integer address) {
        try {
            this.startOffList.add(validateRange(address, this.lowMem, this.highMem));
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid start-off address paramter!\n" + exception.getMessage());
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
    
    /**
     * @throws IOException 
     * 
     */
    @Override
    public void run() {
        
        this.log("Starting disassembler process at %s%n", new Date(System.currentTimeMillis()));
        this.log("Output file: %s%n", this.outputPath);
        
        /** The list of disassembled instructions (output) */
        List<OpCode> instructionsList = new  ArrayList<>(Memory.MEMORY_SIZE);
        
        // initializes the instructions list with bytes from memory (from low to high addresses)
        // for disassembling purposes, a byte is output to the source file as a "db ##"
        // during the disassembling process, a byte may be replaced by a reference to an instruction (OpCode object)
        // or a data byte (part of an instruction)
        for(int i = Memory.START_ADDRESS; i <= Memory.END_ADDRESS; i++) {
            instructionsList.add(i, DB_BYTE);
        }   
        
        // keep running until the start-off list is empty 
        while(!this.startOffList.isEmpty()) {
            
            // set the memory pointer equals to the start address
            int startAddress = this.popStartAddress();
            this.memory.setPointer(startAddress);
            this.log("Processing start-off address: 0x%X%n", startAddress);
            
            boolean processNextInstruction = true;
            while(processNextInstruction) {
                
                // get the current position in memory to be disassembled
                int memoryPointer = this.memory.getPointer();
                
                // verify if the current memory address was already processed
                // this condition may be true if a jump instruction to the "middle" of an already
                // processed instruction occurs
                if(instructionsList.get(memoryPointer).equals(BYTE_DATA)) {
                    this.removeAddressLabel(memoryPointer);
                    this.log("Warning: Unprocessed instruction at address 0x%X%n", memoryPointer);
                    break;
                } else if(!instructionsList.get(memoryPointer).equals(DB_BYTE)) {
                    // if the current instruction is not a byte data nor a db value,
                    // it is a processed instruction. In this case, process the next start-off address
                    break;
                }   
                
                // get the list of op codes for the current byte (prefix)
                byte currentByte = this.memory.get();
                String opCodePrefix = String.format("%02X", currentByte);
                OpCodeClass opCodeClass = OpCodeClass.of(opCodePrefix);
                
                // search for the op code that matches the next instruction (and prefix)
                // this search method could be optimized, but for this application it is ok!
                for(OpCode opcode:this.opCodesMap.get(opCodeClass)) {
                    
                    // find a matching opcode for the next bytes in memory
                    byte[] bytes = this.memory.getBytes(opcode.getSize());
                    if(!opcode.matches(bytes)) {
                        continue;
                    }   
                    
//                    // verify if the other bytes that composes the instruction was already processed
//                    for(int k = 1; k < bytes.length; k++) {
//                        if(!instructionsList.get(memoryPointer+k).equals(DB_BYTE)) {
//                            this.log("Warning: Unprocessed instruction at address 0x%X%n", memoryPointer);
//                            break;
//                        }   
//                    }   
                    
                    SystemOut.vprintf("%04X: %02X %s%n", memoryPointer, bytes[0], opcode.translate(bytes));
                    
                    // set the opcode for the current memory position and set the byte data placeholder
                    // for next memory positions that defines the instruction (if applicable)
                    instructionsList.set(memoryPointer, opcode);
                    for(int k = 1; k < bytes.length; k++) {
                        instructionsList.set(memoryPointer+k, BYTE_DATA);
                    }   
                    
                    // update the memory pointer to the next instruction  
                    this.memory.incrementPointer(bytes.length);
                    
                    // process jump instructions
                    String mnemonicMask = opcode.getMnemonicMask();
                    if(mnemonicMask.contains("CALL") || (mnemonicMask.contains("JP"))) {
                        
                        // verify if the JP instruction is indexed by a register
                        // in this case, the resulting address of the jump is unknown
                        if(INDEXED_JP_PREFIX.contains(opCodePrefix)) {
                            this.log("Warning: Found indexed jump instruction at address 0x%X%n", memoryPointer);
                            processNextInstruction = false;
                            break;
                        }   
                        
                        // evaluate the call/jp address and push to start-off list
                        int address = (bytes[1] & 0xFF) | ((bytes[2] << 8) & 0xFFFF);
                        if(this.isInMemoryRange(address)) {
                            this.pushStartAddress(address);
                            if(!instructionsList.get(address).equals(BYTE_DATA)) {
                                this.mapAddressLabel(this.farLabelPrefix, address);
                            } else {
                                this.log("Warning: could not define label for jump instruction at address 0x%X%n", address);
                            }   
                        }   
                        
                        // for unconditional jump, the current disassembling thread must be ended
                        if(opCodePrefix.equals("C3")) {
                            processNextInstruction = false;
                            break;
                        }   
                        
                    } else if(mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
                        
                        // evaluate the absolute address from relative jump and push it to start-off list
                        int address = memoryPointer + (bytes[1] + 2);
                        if(this.isInMemoryRange(address)) {
                            this.pushStartAddress(address);
                            if(!instructionsList.get(address).equals(BYTE_DATA)) {
                                this.mapAddressLabel(this.nearLabelPrefix, address);
                            } else {
                                this.log("Warning: could not define label for jump instruction at address 0x%X%n", address);
                            }   
                        }   
                    }   
                    
                    // update the stop criteria for this thread
                    processNextInstruction = !(mnemonicMask.contains("RET") || this.memory.getPointer() > this.highMem);
                    break;
                }   
                
                // the current byte could not be found in the op code list, leave it as a "db" and
                // go to the next memory position
                if(memoryPointer == this.memory.getPointer()) {
                    System.out.printf("%04X: db 0%XH%n", memoryPointer, currentByte);
                    this.memory.incrementPointer();
                }   
            }
            
        }
        
        // process output source code file
        this.processOutputSourceFile(instructionsList);
        this.processOutputListFile(instructionsList);
    }   
    
    /**
     * 
     * @param instructionsList
     */
    private void processOutputSourceFile(List<OpCode> instructionsList) {
        
        // append log file for writing (create a new file if necessary)
        try (BufferedWriter writer = Files.newBufferedWriter(this.outputPath, CREATE, APPEND)) {
            
            writer.write("; Z80 Hacker disassembler tool - version 0.1\n");
            writer.write("; Author: Luciano M. Christofoletti\n");
            writer.write(String.format("; Date: %s%n", java.time.LocalDateTime.now()));
            Optional<String> binaryFileName = this.memory.getBinaryFileName();
            if(binaryFileName.isPresent()) {
                writer.write(String.format("; Input file: %s%n", binaryFileName.get()));
            }   
            
            //writer.write(String.format("%-" + this.tabSize + " ORG s", StringUtil.intToHexString(this.lowMem)));
            //writer.write(String.format("%n    ORG %s%n%n", StringUtil.intToHexString(this.lowMem)));
            writer.write(String.format("%n%"+this.tabSize+"sORG %s%n", "", StringUtil.intToHexString(this.lowMem)));
            
            // writes all instructions from disassembled memory
            for(int address = this.lowMem; address <= this.highMem;) {
                
                // get the current instruction
                OpCode instruction = instructionsList.get(address);
                String mnemonicMask = instruction.getMnemonicMask();
                byte[] bytes = this.memory.getBytes(address, instruction.getSize());
                
                // write label, if applicable
                String label = this.labelsMap.get(address);
                if(label != null) {
                    writer.write(String.format("%n%s:%n", label));
                }   
                
                // process instruction opcode
                if(!instruction.equals(DB_BYTE)) {
                    
                    // output byte sequence and mnemonic of current instruction
                    if(mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
                        
                        int nearAddress = address + (bytes[1] + 2);
                        String nearLabel = this.labelsMap.get(nearAddress);
                        if(nearLabel == null) {
                            nearLabel = StringUtil.intToHexString(nearAddress);
                        }   
                        
                        writer.write(String.format(
                            "%"+this.tabSize+ "s%s%n", "", instruction.translate(bytes, nearLabel))
                        );  
//                        writer.write(instruction.translate(bytes, jumpLabel));
//                        writer.newLine();
                        
                    } else if(mnemonicMask.contains("CALL") || mnemonicMask.contains("JP")) {
                        
                        int farAddress = (bytes[1] & 0xFF) | ((bytes[2] << 8) & 0xFFFF);
                        String farLabel = this.labelsMap.get(farAddress);
                        if(farLabel == null) {
                            farLabel = this.equsMap.get(farAddress);
                            if(farLabel == null) {
                                farLabel = StringUtil.intToHexString(farAddress);
                            }   
                        }   
                        
                        writer.write(String.format(
                            "%"+this.tabSize+ "s%s%n", "", instruction.translate(bytes, farLabel))
                        );  
                        
                    } else {
                        //writer.write(String.format("%s%n", instruction.translate(bytes)));
                        writer.write(String.format(
                            "%"+this.tabSize+ "s%s%n", "", instruction.translate(bytes))
                        );  
                    }   
                    
                    // update the current memory address
                    address += instruction.getSize();
                    
                } else {
                    
                    // write the start of data line (db directive plus byte data)
                    //writer.newLine();
                    byte data = this.memory.get(address++);
                    writer.write(String.format(
                        "%"+this.tabSize+ "sdb %4s", "", StringUtil.byteToHexString(data))
                    );  
                    
                    int byteCounter = 0;
                    while(instructionsList.get(address).equals(DB_BYTE)) {
                        
                        // get byte from current memory address
                        data = this.memory.get(address);
                        writer.write(String.format(", %4s", StringUtil.byteToHexString(data)));
                        
                        // output max of db align bytes per line
                        if(++address > this.highMem || ++byteCounter > this.dbAlign) {
                            break;
                        }   
                    }   
                    
                    // goto to the next line :P
                    writer.newLine();
                }   
            }   
            
//            this.memory.setPointer(this.lowMem);
//            
//            for(int address = this.lowMem; address <= this.highMem;) {
//                
//                // write label, if applicable
//                String label = this.labelsMap.get(address);
//                if(label != null) {
//                    writer.write(String.format("%n%s:%n", label));
//                }   
//                
//                OpCode instruction = instructionsList.get(address);
//                if(instruction != BYTE_DATA) {
//                    
//                    // get memory data
//                    byte[] bytes = this.memory.getBytes(instruction.getSize());
//                    
//                    // get instruction and label information
//                    String mnemonicMask = instruction.getMnemonicMask();
//                    //String label = this.labelsMap.get(memoryPosition);
//                    
//                    if(mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
//                        int jumpAddress = this.memory.getPointer() + (bytes[1] + 2);
//                        writer.write("    " + instruction.translate(bytes, StringUtil.intToHexString(jumpAddress)) + "\n");
//                    }
////                    } else if(mnemonicMask.contains("CALL") || mnemonicMask.contains("JP")) {
////                        int callAddress = (bytes[1] & 0xFF) | ((bytes[2] << 8) & 0xFFFF);
////                        writer.write("    " + instruction.translate(bytes, StringUtil.intToHexString(callAddress)) + "\n");
//                    else {
//                        writer.write("    " + instruction.translate(bytes) + "\n");
//                    }   
//                    //writer.write("    " + instruction.translate(bytes) + "\n");
////                    byte[] bytes = this.memory.getBytes(instruction.getSize());
////                    writer.write("    " + instruction.translate(bytes) + "\n");
//                    this.memory.incrementPointer(instruction.getSize()); 
//                    
//                } else {
//                    this.memory.incrementPointer();
//                }   
//                
//                // update the memory index
//                address += instruction.getSize();
//            }
            
        } catch (IOException ioException) {
            System.err.format("Error writing output source file: %s%n", ioException.getMessage());
            System.exit(-1);
        }   
        
        
    }   
    
    /**
     * Writes the list file. The list file contains the processed instructions with addresses and binary data
     * shown in hexadecimal format (without labels).
     * This file does not contains compilable code. It is intended for output validation/analysis only.
     * 
     * @param instructionsList the processed instructions list
     */
    private void processOutputListFile(List<OpCode> instructionsList) {
        
        // append log file for writing (create a new file if necessary)
        try (BufferedWriter writer = Files.newBufferedWriter(this.listPath, CREATE, APPEND)) {
            
            writer.write("; Z80 Hacker disassembler tool - version 0.1\n");
            writer.write("; Author: Luciano M. Christofoletti\n");
            writer.write(String.format("; Date: %s%n", java.time.LocalDateTime.now()));
            Optional<String> binaryFileName = this.memory.getBinaryFileName();
            if(binaryFileName.isPresent()) {
                writer.write(String.format("; Input file: %s%n", binaryFileName.get()));
            }   
            
            // writes all instructions from disassembled memory
            for(int address = this.lowMem; address <= this.highMem;) {
                
                // write current memory address
                writer.newLine();
                writer.write(String.format("%s: ", StringUtil.intToHexString(address)));
                
                // get the current instruction
                OpCode instruction = instructionsList.get(address);
                
                // process instruction opcode
                if(!instruction.equals(DB_BYTE)) {
                    
                    // output byte sequence and mnemonic of current instruction
                    byte[] bytes = this.memory.getBytes(address, instruction.getSize());
                    writer.write(String.format("%12s: ", StringUtil.bytesToHex(bytes, " ")));
                    
                    // output the instruction's mnemonic
                    String mnemonicMask = instruction.getMnemonicMask();
                    if(mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
                        int jumpAddress = address + (bytes[1] + 2);
                        writer.write(instruction.translate(bytes, StringUtil.intToHexString(jumpAddress)));
                    } else {
                        writer.write(String.format("%s", instruction.translate(bytes)));
                    }   
                    
                    // update the current memory address
                    address += instruction.getSize();
                    
                } else {
                    
                    int byteCounter = 0;
                    do {
                        
                        // get byte from current memory address
                        byte data = this.memory.get(address);
                        writer.write(String.format("%4s ", StringUtil.byteToHexString(data)));
                        
                        // output max of db align bytes per line
                        if(++address > this.highMem || ++byteCounter > this.dbAlign) {
                            break;
                        }   
                        
                    } while(instructionsList.get(address).equals(DB_BYTE));
                    
                }   
            }   
            
        } catch (IOException ioException) {
            System.err.format("Error writing output source file: %s%n", ioException.getMessage());
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
     * Loads the op codes data from file (patterns and attributes).
     * 
     * @param file
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    public synchronized void loadOpCodesFromFile(String fileName)
            throws IOException, IllegalArgumentException {
        InputStream stream = this.getResourceAsStream(fileName);
    	this.loadOpCodesFromStream(stream);
    }   
    
    /**
     * Loads the op codes data from input stream (patterns and attributes).
     * 
     * @param inputStream the input stream
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    public synchronized void loadOpCodesFromStream(InputStream inputStream)
            throws IOException, IllegalArgumentException {
        
        // line data read from text file
        String line;
        
        // the current line number (used for error messages)
        int lineNumber = 0;
        int instructionsCounter = 0;
        
        // InputStreamReader reads bytes and decodes them into characters using a specified charset
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            SystemOut.vprint("Loading op-codes from resource file...");
            
            while ((line = bufferedReader.readLine()) != null) {
                
                // update the line number counter
                lineNumber++;
                
                // discard comment lines and empty lines
                line = StringUtil.clean(line, ';');
                if (line.isEmpty()) {
                    continue;
                }   
                
                // get the opcode and the mnemonic string format 
                String lineSplit[] = line.split(":");
                if (lineSplit.length > 1) {
                    
                    // get the op-code/mnemonic string representations
                    String opCodeString = lineSplit[0].trim();
                    String mnemonicString = lineSplit[1].trim();
                    
                    // create the OpCode for the instruction and get the group prefix
                    OpCode opCode = new OpCode(opCodeString, mnemonicString);
                    
                    // add the opcode to the corresponding op-code list (mapped by prefix)
                    //this.opCodesMap.get(opCode.getPrefix()).add(opCode);
                    this.opCodesMap.map(opCode.getOpCodeClass(), opCode);
                    
                    // update the processed instruction counter
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
            //  a NullPointerException may occur if the op-codes.dat cannot be found in the classpath
            throw new IOException("System could not find the op-codes resource file in the classpath!");
        }
    }   
	
    /**
     * 
     * @param value
     * @param min
     * @param max
     * @return
     * @throws IllegalArgumentException
     */
    private static int validateRange(int value, int min, int max) throws IllegalArgumentException {
        if(value >= min && value <= max) {
            return value;
        } else {
            throw new IllegalArgumentException(
                String.format("\tValue %d is outside allowed range [%d, %d]", value, min, max)
            );
        }   
    }   
}