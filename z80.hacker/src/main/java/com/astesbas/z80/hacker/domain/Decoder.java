package com.astesbas.z80.hacker.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 8-bit/64Kb binary data decoder.
 * This class manages the binary data and decode instructions.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 16/sep/2017
 */
public class Decoder {
    
    /** The binary data to be disassembled (code and data) */
    private final BinaryData binaryData;
    
    /** The list of disassembled instructions (decoded instructions) */
    private final List<Instruction> instructionsList;
    
    /** The lower bound address to be disassembled - addresses smaller than this will not be processed */
    private int startAddress = BinaryData.START_ADDRESS;
    
    /** The upper bound address to be disassembled - addresses greater than this will not be processed */
    private int endAddress = BinaryData.END_ADDRESS;
    
    /**
     * The binary data decoder.
     * This class holds the binary data and the corresponding disassembled instructions.
     * @param binaryData
     */
    public Decoder(BinaryData binaryData) {
        
        this.binaryData = Objects.requireNonNull(binaryData);
        this.instructionsList = new  ArrayList<>(BinaryData.MAX_SIZE);
        
        // Initializes the instruction's list with bytes from memory (from low to high addresses)
        // For disassembling purposes, a byte is output to the source file as a "db ##".
        // During the disassembling process, a "db instruction" may be replaced by a reference to 
        // Z80 instruction or a data byte (the "parameter" part of an instruction)
        for(int i = BinaryData.START_ADDRESS; i <= BinaryData.END_ADDRESS; i++) {
            this.instructionsList.add(i, Instruction.DB_BYTE);
        }   
    }   
    
    /**
     * @return the binary data object
     */
    public BinaryData getBinaryData() {
        return this.binaryData;
    }   
    
    /**
     * @return the instructions list
     */
    public List<Instruction> getInstructionsList() {
        return Collections.unmodifiableList(this.instructionsList);
    }   
    
    /**
     * @return the start address of disassembled code
     */
    public int getStartAddress() {
        return this.startAddress;
    }   
    
    /**
     * Set the start address. The address must be between Memory.START_ADDRESS and high memory.
     * @param startAddress the start address value to set
     */
    public void setStartAddress(int startAddress) {
        try {
            this.startAddress = validateRange(startAddress, BinaryData.START_ADDRESS, this.endAddress);
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid start address parameter!\n" + exception.getMessage());
        }   
    }   
    
    /**
     * @return the end address of disassembled code
     */
    public int getEndAddress() {
        return this.endAddress;
    }   
    
    /**
     * Set the upper bound address. The address must be between low memory and Memory.END_ADDRESS.
     * @param endAddress the end address value to set
     */
    public void setEndAddress(int endAddress) {
        try {
            this.endAddress = validateRange(endAddress, this.startAddress, BinaryData.END_ADDRESS);
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid end address parameter!\n" + exception.getMessage());
        }   
    }   
    
    /**
     * Verify if the byte at the given address is a data byte (DB_BYTE).
     * @param address
     * @return true if the byte at the given address is a data byte
     */
    public boolean isDbByte(int address) {
        return this.instructionsList.get(address).isDbByte();
    }   
    
    /**
     * Verify if the byte at the given address is part of an instruction (opcode or parameter).
     * @param address
     * @return true if the byte at the given address is part of an instruction
     */
    public boolean isParameterByte(int address) {
        return this.instructionsList.get(address).isParameter();
    }   
    
    /**
     * Set the instruction for the given address.
     * @param address
     * @param instruction
     */
    public void setInstruction(int address, Instruction instruction) {
        
        int instructionSize = instruction.getSize();
        byte[] bytes = this.binaryData.getBytes(address, instructionSize);
        
        if(instruction.matches(bytes) && this.isAvailable(address, instructionSize)) {
            
            // Set the first position equals to the instruction
            this.instructionsList.set(address, instruction);
            
            // Set the other bytes of the instruction as parameters (if applicable)
            for(int k = 1; k < instructionSize; k++) {
                this.instructionsList.set(address+k, Instruction.PARAMTER);
            }   
            
        } else {
            throw new IllegalArgumentException(
                String.format("Could not set instruction %s at 0x%04X!" + instruction.getMnemonicMask(), address)
            );  
        }   
    }      
    
    /**
     * Verify if the bytes in the range [address, address+length] is available
     * to "hold" an instruction (not decoded yet).
     * 
     * @param address the start address
     * @param length the number of bytes
     * @return true if there is no byte "allocated" in the range
     */
    public boolean isAvailable(int address, int length) {
        for(int position = address; position < address+length; position++) {
            if(!this.isDbByte(position)) {
                return false;
            }   
        }   
        return true;
    }   
    
    /**
     * Return the address of the first byte of an already "translated" instruction.
     * Example:
     * Translated instruction at 08000H is: LD BC,0AA55H (byte sequence 01 55 AA)
     * Calling this method with address 08002H will return 08000H as the address instruction.
     * Calling this method for addresses 08000H and 08001H will also return the address 08000H.
     * 
     * @param address
     * @return
     */
    public int getStartAddressOfInstructionAt(int address) {
        
        if(!this.isDbByte(address)) {
            
            // Find the first byte of the processed instruction
            int startAddress = address;
            while(this.isParameterByte(startAddress) && this.isValidAddress(startAddress)) {
                startAddress--;
            }   
            
            return startAddress;
            
        } else {
            throw new IllegalArgumentException(String.format("There is no instruction at address 0x%04X" + address));
        }
    }   
    
    /**
     * Verify if the given address is in the valid disassembling range.
     * @param address the address to verify
     */
    public boolean isValidAddress(int address) {
        return (address >= this.startAddress && address <= this.endAddress);
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
