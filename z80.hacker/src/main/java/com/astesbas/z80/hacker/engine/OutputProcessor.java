/**
 * 
 */
package com.astesbas.z80.hacker.engine;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Optional;

import com.astesbas.z80.hacker.domain.BinaryData;
import com.astesbas.z80.hacker.domain.Decoder;
import com.astesbas.z80.hacker.domain.Instruction;
import com.astesbas.z80.hacker.domain.PrefixClass;
import com.astesbas.z80.hacker.util.FileDateUtil;
import com.astesbas.z80.hacker.util.StringUtil;

/**
 * Output source and list file processor.
 * This is intended to be used by the Z80 Disassembler engine.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @since 13/sep/2017
 */
public class OutputProcessor {
    
    /** Mapping for labels entries at given addresses */
    private final Map<Integer, String> labelsMap = new HashMap<>();
    
    /** Mapping of EQU entries (key, value) */
    private final Map<String, String> equsMap = new HashMap<>();
    
    /** The list of disassembled instructions (output) */
    //private final List<Instruction> instructionsList = new ArrayList<>();
    
    /** The binary data to be disassembled (memory dump/rom file/etc) */
//    private final BinaryData binaryData;
    
    /** Number of byte per line for "db" directives */
    private int dbAlign = 16;
    
    /** Tabulation size to be inserted before instructions */
    private int tabSize = 4;
    
    /** Code label prefix: used as prefix for labels that precedes assembly code */
    private String codeLabelPrefix = "";
    
    /** Data label prefix: used as prefix for data byte (db) sections */
    private String dataLabelPrefix = "";
    
//    // The data byte representation (used only as placeholders for unprocessed/reserved byte positions))
//    private static final Instruction DB_BYTE = new Instruction("##", "db ##"); // "unprocessed" bytes will be output as "db ##"
//    private static final Instruction BYTE_DATA = new Instruction("##", "[##]"); // placeholder for "data bytes" of instructions
    
    /**
     * 
     * @param binaryData
     */
    //protected OutputProcessor(BinaryData binaryData) {
        
//        this.binaryData = binaryData;
        
//        // Initializes the instruction's list with bytes from memory.
//        // For disassembling purposes, a byte is output to the source file as a "db ##".
//        // During the disassembling process, a "db instruction" may be replaced by a reference to 
//        // Z80 instruction or a data byte (the "parameter" part of an instruction)
//        for(int i = BinaryData.START_ADDRESS; i <= BinaryData.END_ADDRESS; i++) {
//            this.instructionsList.add(i, DB_BYTE);
//        }   
//    }   
    
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
     * Maps a code label to be used as reference in the disassembled code.
     * If the address is already maps a label, then the new mapping is discarded.
     * 
     * @param address the address to be mapped to the given label
     */
    public void mapCodeLabel(Integer address) {
        if(!this.labelsMap.containsKey(address)) {
            this.mapAddressLabel(address, this.codeLabelPrefix);
        }   
    }   
    
    /**
     * Maps a data label to be used as reference in the disassembled code.
     * If the address is already maps a label, then the new mapping is discarded.
     * 
     * @param address the address to be mapped to the given label
     */
    public void mapDataLabel(Integer address) {
        if(!this.labelsMap.containsKey(address)) {
            this.mapAddressLabel(address, this.dataLabelPrefix);
        }   
    }   
    
    /**
     * Maps a label to be used as reference in the disassembled code. The label
     * is defined as the concatenation of the prefeix and start address.
     * 
     * If the address is already maps a label, then the new mapping is discarded.
     * @param address
     * @param prefix
     */
    public void mapAddressLabel(Integer address, String prefix) {
        this.mapLabel(address, String.format("%s%s", prefix, StringUtil.intToHexString(address)));  
    }   
    
    /**
     * Set the prefix for code labels.
     * @param codeLabelPrefix the data label prefix
     */
    public void setCodeLabelPrefix(String codeLabelPrefix) {
        this.codeLabelPrefix = Objects.requireNonNull(codeLabelPrefix);
    }   
    
    /**
     * Set the prefix for data labels.
     * @param dataLabelPrefix the data label prefix
     */
    public void setDataLabelPrefix(String dataLabelPrefix) {
        this.dataLabelPrefix = Objects.requireNonNull(dataLabelPrefix);
    }   
    
//    /**
//     * Maps an address label at the last memory position where exists an instruction, starting at the given address.
//     * @param prefix
//     * @param startAddress
//     */
//    public void mapAddressLabelWithDisplacement(String prefix, int startAddress) {
//        
//        // Find the first byte of the processed instruction
//        int instructionAddress = startAddress;
//        while(this.instructionsList.get(instructionAddress).equals(BYTE_DATA) && instructionAddress > 0) {
//            instructionAddress--;
//        }   
//        
//        // Map the new label and displacement
//        this.mapAddressLabel(prefix, instructionAddress);
//        this.mapOffset(instructionAddress, instructionAddress-startAddress);
//    }   
    
    /**
     * Maps a code label with displacement offset.
     * @param address the binary data address
     * @param offset the offset
     */
    public void mapOffsetCodeLabel(Integer address, Integer offset) {
        if(!this.labelsMap.containsKey(address)) {
            String label = String.format("%s%s + %d", this.codeLabelPrefix, StringUtil.intToHexString(address), offset);
            this.labelsMap.put(address, label);
        }   
    }   
    
    /**
     * Maps an EQU directive (label, value).
     * @param label the key label
     * @param value the value to be mapped to the given equ label
     */
    public void mapEqu(String label, String value) {
        this.equsMap.put(value, label);
    }   
    
    /**
     * 
     * @param address
     * @return
     */
    public String getLabel(Integer address) {
        String label =  this.labelsMap.get(address);
        if(label == null) {
            label = StringUtil.intToHexString(address);
        }   
        return label;
    }   
    
//    /**
//     * Return a label in the format "0HHHH + offset". If offset is not available, return the address as string.
//     * @param address
//     * @return the address label with offset
//     */
//    public String getOffsetLabel(Integer address) {
//        String label = this.getLabel(address);
////        if(this.offsetMap.containsKey(address)) {
////            label = label + " + " + this.offsetMap.get(address);
////        }   
//        return label;
//    }   

    /**
     * Set the data align size (maximum of 64).
     * 
     * @param dbAlign
     *            the dbAlign to set
     * @throws IllegalArgumentException
     *             if the given value is outside the permitted range
     */
    public void setDbAlign(int dbAlign) throws IllegalArgumentException {
        try {
            this.dbAlign = validateRange(dbAlign, 1, 64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid db align value!\n" + exception.getMessage());
        }
    }

    /**
     * Set the tabulation size (maximum of 64).
     * 
     * @param tabSize
     *            the tabSize to set
     * @throws IllegalArgumentException
     *             if the given value is outside the permitted range
     */
    public void setTabSize(int tabSize) throws IllegalArgumentException {
        try {
            this.tabSize = validateRange(tabSize, 0, 32);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid tab size value!\n" + exception.getMessage());
        }
    }
    
    /**
     * 
     * @param instructionsList
     */
    private void processOutputSourceFile() {
        
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
        
        
    }   
    
    /**
     * Writes the list file. The list file contains the processed instructions with addresses and binary data
     * shown in hexadecimal format (without labels).
     * This file does not contains compilable code. It is intended for output validation/analysis only.
     * 
     * @param listPath
     * @param decoder
     */
    public void processOutputListFile(Path listPath, Decoder decoder) {
        
        try (BufferedWriter writer = Files.newBufferedWriter(listPath, CREATE, APPEND)) {
            
            BinaryData binaryData = decoder.getBinaryData();
            List<Instruction> instructionsList = decoder.getInstructionsList();
            
            writer.write("; **** Generated by Z80 Hacker disassembler tool - version 0.1 ****\n");
            writer.write("; Author: Luciano M. Christofoletti\n");
            writer.write(String.format("; Date: %s%n", FileDateUtil.getCurrentTime()));
            Optional<String> binaryFileName = binaryData.getBinaryFileName();
            if(binaryFileName.isPresent()) {
                writer.write(String.format("; Input file: %s%n", binaryFileName.get()));
            }   
            
            // Writes all instructions from disassembled memory
            for(int address = decoder.getStartAddress(); address <= decoder.getEndAddress();) {
                
                writer.newLine();
                writer.write(String.format("%s: ", StringUtil.intToHexString(address)));
                
                Instruction instruction = instructionsList.get(address);
                
                // If the current instruction is not a byte value, output the opcode mnemonic 
                if(!instruction.equals(Decoder.DB_BYTE)) {
                    
                    // Output byte sequence that defines the current instruction
                    byte[] bytes = binaryData.getBytes(address, instruction.getSize());
                    writer.write(String.format("%-12s: ", StringUtil.bytesToHex(bytes, " ")));
                    
                    // Output the instruction's mnemonic
                    String mnemonicMask = instruction.getMnemonicMask();
                    if(mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
                        String label = StringUtil.intToHexString(address + (bytes[1] + 2));
                        writer.write(instruction.translate(bytes, label));
                    } else {
                        writer.write(String.format("%s", instruction.translate(bytes)));
                    }   
                    
                    address += instruction.getSize();
                    
                } else {
                    
                    writer.write(StringUtil.spaces(12)+": ");
                    
                    int byteCounter = 0;
                    do {
                        
                        byte data = binaryData.get(address);
                        writer.write(String.format("%02X ", data));
                        
                        // Output max of db align bytes per line
                        if(++address > decoder.getEndAddress() || ++byteCounter > this.dbAlign) {
                            break;
                        }   
                        
                    } while(decoder.isDbByte(address));
                    
                }   
            }   
            
        } catch (IOException ioException) {
            System.err.format("Error writing output list file: %s%n", ioException.getMessage());
            System.exit(-1);
        }   
    }   
    
    /**
     * Validate an integer value according to the given min and max parameters.
     * 
     * @param value the valur to be validated
     * @param min the minimum value allowed
     * @param max the maximum value allowed
     * @return the value if it is inside the valid range
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
