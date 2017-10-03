package com.astesbas.z80.hacker.engine;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import com.astesbas.z80.hacker.domain.BinaryData;
import com.astesbas.z80.hacker.domain.Decoder;
import com.astesbas.z80.hacker.domain.Instruction;
import com.astesbas.z80.hacker.domain.PrefixClass;
import com.astesbas.z80.hacker.util.FileDateUtil;
import com.astesbas.z80.hacker.util.StringUtil;

/**
 * Output assembly source and list file processor.
 * This is intended to be used by the Z80 Disassembler engine.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 13/sep/2017
 */
public class OutputProcessor {
    
    /** Mapping for labels entries at given addresses */
    private final Map<Integer, String> labelsMap = new HashMap<>();
    
    /** Mapping of EQU entries (key, value) */
    private final Map<String, String> equsMap = new HashMap<>();
    
    /** Number of byte per line for "db" directives */
    private int dbAlign = 16;
    
    /** Tabulation size to be inserted before instructions */
    private int tabSize = 4;
    
    /** Code label prefix: used as prefix for labels that precedes assembly code */
    private String codeLabelPrefix = "";
    
    /** Data label prefix: used as prefix for data byte (db) sections */
    private String dataLabelPrefix = "";
    
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
    
    /**
     * Maps a code label with displacement offset.
     * @param address the binary data address
     * @param offset the offset
     */
    public void mapOffsetCodeLabel(Integer address, Integer offset) {
        if(!this.labelsMap.containsKey(address)) {
            String label = String.format("%s%s + %d", this.codeLabelPrefix, StringUtil.intToHexString(address-offset), offset);
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
     * @param outputPath
     * @param instructionsList
     */
    public void processOutputSourceFile(Path outputPath, Decoder decoder) {
        
        String tab = StringUtil.spaces(this.tabSize);
        
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, CREATE, APPEND)) {
            
            BinaryData binaryData = decoder.getBinaryData();
            List<Instruction> instructionsList = decoder.getInstructionsList();
            
            this.printFileHeader(writer);
            Optional<String> binaryFileName = binaryData.getBinaryFileName();
            if(binaryFileName.isPresent()) {
                writer.write(String.format("; Input file: %s%n", binaryFileName.get()));
            }   
            
            // Write out the equ mapping keys/values
            writer.newLine();
            for(Entry<String, String> equMapEntry:this.equsMap.entrySet()) {
                writer.write(String.format("%-12s EQU %s%n", equMapEntry.getValue()+":", equMapEntry.getKey()));
            }   
            
            // Write the ORG directive
            int startAddress = decoder.getStartAddress();
            int endAddress = decoder.getEndAddress();
            writer.newLine();
            writer.write(tab);
            writer.write(String.format("ORG %s%n", StringUtil.intToHexString(startAddress)));
            
            // writes all instructions from disassembled memory
            for(int address = startAddress; address <= endAddress;) {
                
                // get the current instruction
                Instruction instruction = instructionsList.get(address);
                String mnemonicMask = instruction.getMnemonicMask();
                byte[] bytes = binaryData.getBytes(address, instruction.getSize());
                
                // write label, if applicable
                String label = this.labelsMap.get(address);
                if(label != null) {
                    writer.write(String.format("%n%s:%n", label));
                }   
                
                if(!instruction.isDbByte()) {
                    
                    String mnemonicString = instruction.translate(bytes);
                    
                    // Process relative jump instructions
                    if(mnemonicMask.contains("JR") || mnemonicMask.contains("DJNZ")) {
                        
                        // Evaluate the near (relative) jump address 
                        int nearAddress = address + (bytes[1] + 2);
                        String nearLabel = this.labelsMap.get(nearAddress);
                        if(nearLabel == null) {
                            nearLabel = StringUtil.intToHexString(nearAddress);
                        }   
                        
                        mnemonicString = instruction.translate(bytes, nearLabel);
                        
                    } else if(instruction.hasWordParameter()) {
                        
                        // Evaluate the two bytes address (for prefixed instructions, the address bytes are
                        // shifted one byte ahead)
                        int farAddress = instruction.getPrefixClass().equals(PrefixClass.$$) ?
                                (bytes[1] & 0xFF) | ((bytes[2] << 8) & 0xFFFF):
                                (bytes[2] & 0xFF) | ((bytes[3] << 8) & 0xFFFF);
                        
                        // First, tries to get the label from mapped labels 
                        String farLabel = this.labelsMap.get(farAddress);
                        if(farLabel == null) {
                            farLabel = StringUtil.intToHexString(farAddress);
                            // verify if there is a equ definition for the "translated" address
                            if(this.equsMap.containsKey(farLabel)) {
                                farLabel = this.equsMap.get(farLabel);
                            }   
                        }   
                        
                        mnemonicString = instruction.translate(bytes, farLabel);
                    }   
                    
                    writer.write(String.format("%s%s%n", tab, mnemonicString));
                    
                    // update the current memory address
                    address += instruction.getSize();
                    
                } else {
                    
                    // write the start of data line (db directive plus byte data)
                    byte byteData = binaryData.get(address++);
                    writer.write(String.format("%sdb %4s", tab, StringUtil.byteToHexString(byteData)));  
                    
                    int byteCounter = 0;
                    while(decoder.isDbByte(address)) {
                        
                        // verify if there is a label at the current byte address
                        // if so, then go to the next line, set the label e restart the db section
                        if(this.labelsMap.get(address) != null) {
                            break;
                        }   
                        
                        // get byte from current memory address
                        byteData = binaryData.get(address);
                        writer.write(String.format(", %4s", StringUtil.byteToHexString(byteData)));
                        
                        // output max of dbAlign bytes per line
                        if(++address > endAddress || ++byteCounter > this.dbAlign) {
                            break;
                        }   
                    }   
                    writer.newLine();
                }   
            }   
            
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
     * @param listPath
     * @param decoder
     */
    public void processOutputListFile(Path listPath, Decoder decoder) {
        
        try (BufferedWriter writer = Files.newBufferedWriter(listPath, CREATE, APPEND)) {
            
            BinaryData binaryData = decoder.getBinaryData();
            List<Instruction> instructionsList = decoder.getInstructionsList();
            
            this.printFileHeader(writer);
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
                if(!instruction.isDbByte()) {
                    
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
     * 
     * @param writer
     * @throws IOException
     */
    private void printFileHeader(java.io.Writer writer) throws IOException {
        writer.write("; Generated by Z80 Hacker disassembler tool - version 1.1\n");
        writer.write("; Author: Luciano M. Christofoletti\n");
        writer.write("; www.christofoletti.com.br\n");
        writer.write(String.format("; Created: %s%n", FileDateUtil.getCurrentTime()));
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
