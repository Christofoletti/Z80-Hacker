package com.astesbas.z80.hacker.domain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * 8-bit/64Kb memory emulation. This is a very simplified representation of memory.
 * There is no concept of pages, slots, RAM or ROM type, etc.
 * For this application, this simplified model is enough :P
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 02/jun/2017
 */
public class Memory {
    
    /** Maximum addressed memory size (2^16 for 8-bit processors) */
    public static final int MEMORY_SIZE = 1 << 16;
    
    /** The first memory position */
    public static final int START_ADDRESS = 0x0000;
    
    /** The last memory position */
    public static final int END_ADDRESS = MEMORY_SIZE - 1;
    
    /** The default memory value */
    public static final byte DEFAULT_BYTE_VALUE = 0x00;
    
    /** Byte array (the memory space) */
    private final byte[] data = new byte[MEMORY_SIZE];
    
    /** The current position for getting a byte from byte array */
    private int pointer = 0;
    
    /**
     * Constructor: initializes all memory positions with default byte value.
     */
    public Memory() {
        this.clear();
    }   
    
    /**
     * Fill memory with a byte value.
     * @param value
     */
    public void clear(byte value) {
        Arrays.fill(this.data, value);
    }   
    
    /**
     * Fill memory with default byte value.
     */
    public void clear() {
        this.clear(DEFAULT_BYTE_VALUE);
    }
    
    /**
     * Get byte from given address in memory.
     * Note: the address parameter is 
     * 
     * @param address the address of memory
     * @return the byte in the given address
     */
    public byte get(int address) {
        return this.data[address & 0xFFFF];
    }   
    
    /**
     * Get the byte at the current memory position.
     * @return
     */
    public byte get() {
        return this.data[this.pointer];
    }   
    
    /**
     * Return an array of bytes of size "count", starting from given address.
     * @param address the start address in memory
     * @param count the number of bytes to be fetched
     * @return the array of bytes
     */
    public byte[] getBytes(int address, int count) {
        
        byte[] bytes = new byte[count];
        for(int k=0; k < count; k++) {
            bytes[k] = this.get(address+k);
        }
        
        return bytes;
    }   
    
    /**
     * Return an array of bytes of size "count", starting from current address pointer position.
     * @param count the number of bytes to be fetched
     * @return the array of bytes
     */
    public byte[] getBytes(int count) {
        return this.getBytes(this.pointer, count);
    }   
    
    /**
     * Get the next memory position and increment the memory pointer.
     * @return the next byte in memory
     */
    public byte next() {
        this.incrementPointer(1);
        return this.data[this.pointer];
    }   
    
    /**
     * Get the current pointer address.
     * @return the pointer address
     */
    public int getPointer() {
        return this.pointer;
    }   
    
    /**
     * Set the pointer to the given address.
     * @param address
     */
    public void setPointer(int address) {
        this.pointer = (address & 0xFFFF);
    }   
    
    /**
     * Increment the current pointer address by a given amount
     * @param increment the increment count
     */
    public void incrementPointer(int amount) {
        this.setPointer(this.pointer + amount);
    }   
    
    /**
     * Load data from binary file. The start address is the address where the
     * first byte of the binary file will be placed. The end address is the last
     * position in memory to be filled with the binary data
     * 
     * @param binaryFile the binary file
     * @param start address where the first byte of the binary file will be placed
     * @param end address of the last position in memory to be filled with the binary data
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if occurs any error reading the file
     */
    public void loadFromBinaryFile(java.io.File binaryFile, int start, int end) 
            throws FileNotFoundException, IOException {
        
        int fileSize = (int) binaryFile.length();
        byte[] binaryData = new byte[fileSize];
        
        // validate the start address parameter
        if(start < 0 || start > END_ADDRESS) {
            throw new IllegalArgumentException(
                String.format("Start address of binary file out of range: 0x%X", start)
            );
        }   
        
        // validate the end address parameter
        if(start > end) {
            throw new IllegalArgumentException(
                String.format("End address of binary file out of range: 0x%X", end)
            );
        }   
        
        // evaluate the length of data to be stored in memory
        int lenght = Math.min(end, END_ADDRESS) - start + 1;
        
        // read binary file data
        try (java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.FileInputStream(binaryFile))) {
            
            // read all bytes from binary file
            dis.readFully(binaryData);
            
            // copy the binary data to memory array
            System.arraycopy(binaryData, 0, this.data, start, lenght);
            
        } catch (IOException ioException) {
           throw ioException;
        }   
    }   
    
    /**
     * Load data from binary file. The start address is the address where the
     * first byte of the binary file will be placed in memory.
     * The memory will be filled with the binary data from file until the end of memory is reached
     * (or the end of file)
     * 
     * @param binaryFile the binary file
     * @param start address where the first byte of the binary file will be placed
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if occurs any error reading the file
     */
    public void loadFromBinaryFile(java.io.File binaryFile, int start) throws FileNotFoundException, IOException {
        int end = Math.min(start + (int) binaryFile.length(), END_ADDRESS);
        this.loadFromBinaryFile(binaryFile, start, end);
    }   
    
    /**
     * Load data from binary file. The binary data will be placed starting at first position in memory.
     * The memory will be filled with the binary data from file until the end of memory is reached
     * (or the end of file)
     * 
     * @param binaryFile the binary file
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if occurs any error reading the file
     */
    public void loadFromBinaryFile(java.io.File binaryFile) throws FileNotFoundException, IOException {
        this.loadFromBinaryFile(binaryFile, 0);
    }   
}   
