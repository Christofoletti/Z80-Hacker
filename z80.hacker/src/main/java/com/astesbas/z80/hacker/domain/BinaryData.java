package com.astesbas.z80.hacker.domain;

import static com.astesbas.z80.hacker.util.ConfigFileProperties.ConfigKey.BINARY_END;
import static com.astesbas.z80.hacker.util.ConfigFileProperties.ConfigKey.BINARY_FILE;
import static com.astesbas.z80.hacker.util.ConfigFileProperties.ConfigKey.BINARY_START;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import com.astesbas.z80.hacker.util.ConfigFileProperties;
import com.astesbas.z80.hacker.util.SystemOut;

/**
 * 8-bit/64Kb binary data manager. This is a very simplified manager for binary data.
 * It supplies methods for get byte(s), load binary data from file and a pointer to
 * facilitate getting bytes in sequence. 
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 02/jun/2017
 */
public class BinaryData {
    
    /** Maximum addressed binary data size (2^16 for 8-bit processors) */
    public static final int MAX_SIZE = 1 << 16;
    
    /** The first data position */
    public static final int START_ADDRESS = 0x0000;
    
    /** The last data position */
    public static final int END_ADDRESS = MAX_SIZE - 1;
    
    /** The address mask for 16 bit valid addresses */
    public static final int ADDRESS_MASK = 0xFFFF;
    
    /** The default byte value to initialize the binary data */
    public static final byte DEFAULT_BYTE_VALUE = 0x00;
    
    /** Byte array (the binary data) */
    private final byte[] data = new byte[MAX_SIZE];
    
    /** The current position for getting a byte from byte array */
    private int pointer = 0;
    
    /** The binary file name (set when the binary data is read from a file) */
    private Optional<String> binaryFileName = Optional.empty();
    
    /**
     * Creates a binary data object from given byte array.
     * @param data the binary data array
     */
    public BinaryData(byte[] data) {
        Arrays.fill(this.data, DEFAULT_BYTE_VALUE);
        System.arraycopy(data, 0, this.data, 0, MAX_SIZE);
    }   
    
    /**
     * 
     * @param data
     * @param start
     * @param lenght
     */
    public BinaryData(byte[] data, int start, int lenght) {
        Arrays.fill(this.data, DEFAULT_BYTE_VALUE);
        System.arraycopy(data, 0, this.data, start, lenght);
    }   
    
    /**
     * Return the size of the binary data in bytes.
     * @return number of bytes available in the binary data
     */
    public int getSize() {
        return BinaryData.MAX_SIZE;
    }
    
    /**
     * Return byte from given address of binary data.
     * Note: the address parameter is trunked to a 16-bit address before getting the byte from binary data.
     * 
     * @param address the address of memory
     * @return the byte in the given address
     */
    public byte get(int address) {
        return this.data[address & ADDRESS_MASK];
    }   
    
    /**
     * Return byte from current pointer address plus byte displacement in binary data.
     * Note: the resulting address may be trunked at 0xFFFF before getting the byte from binary data.
     * @param displacement
     * @return
     */
    public byte get(byte displacement) {
        return this.data[(this.pointer + displacement) & ADDRESS_MASK];
    }   
    
    /**
     * Get the byte at the current pointer position.
     * @return
     */
    public byte get() {
        return this.data[this.pointer];
    }   
    
    /**
     * Return an array of bytes of size "count", starting from given address.
     * @param address the start address in binary data
     * @param count the number of bytes to be fetched
     * @return the array of bytes
     */
    public byte[] getBytes(int address, int count) {
        return Arrays.copyOfRange(this.data, address, address+count);
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
     * Increment the address pointer and return the byte at the new pointer position.
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
     * Increment the current pointer address by a given amount.
     * @param increment the increment count
     */
    public void incrementPointer(int amount) {
        this.setPointer(this.pointer + amount);
    }   
    
    /**
     * Increment the current pointer address by one position.
     */
    public void incrementPointer() {
        this.setPointer(this.pointer + 1);
    }   
    
    /**
     * @return the binaryFileName
     */
    public Optional<String> getBinaryFileName() {
        return this.binaryFileName;
    }   
    
    /**
     * 
     * @param binaryFileName
     */
    private void setBinaryFileName(String binaryFileName) {
        this.binaryFileName = Optional.of(binaryFileName);
    }   
    
    /**
     * Creates a new binary data object with parameters from given properties.
     * @param properties the configuration properties read from file
     */
    public static BinaryData fromProperties(ConfigFileProperties properties) {
        
        Optional<String> binaryFileName = Optional.empty();
        BinaryData binaryData = null;
        
        try {
            
            // Get the binary file name, start and end addresses
            binaryFileName = properties.getString(BINARY_FILE);
            int startAddress = properties.getAddress(BINARY_START).orElse(BinaryData.START_ADDRESS);
            int endAddress = properties.getAddress(BINARY_END).orElse(BinaryData.END_ADDRESS);
            
            if(binaryFileName.isPresent()) {
                File binaryFile = new File(binaryFileName.get());
                binaryData = BinaryData.fromFile(binaryFile, startAddress, endAddress);
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
            System.err.printf("%nError reading binary file: %n\t%s%n", binaryFileName.get(), ioException.getMessage());
            System.exit(-1);
        }   
        
        return binaryData;
    }   
    
    /**
     * Load data from a binary file. The start address is the address where the
     * first byte of the binary file will be placed. The end address is the last
     * position in the binary data object to be filled with the binary data.<br />
     * If the binary file is greater than (end-start) bytes, then the file is truncated.
     * If the file is smaller than (end-start) bytes, the memory is filled from
     * start to start+file_size.
     * 
     * @param binaryFile the binary file
     * @param start address where the first byte of the binary file will be placed
     * @param end address of the last position in memory to be filled with the binary data
     * 
     * @return the BinaryData object
     * 
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if occurs any error reading the file
     */
    public static BinaryData fromFile(java.io.File binaryFile, int start, int end) 
            throws FileNotFoundException, IOException {
        
        int fileSize = (int) binaryFile.length();
        byte[] data = new byte[fileSize];
        
        // Validate the start address parameter
        if(start < 0 || start > END_ADDRESS) {
            throw new IllegalArgumentException(
                String.format("Start address of binary file out of range: 0x%X", start)
            );  
        }   
        
        // Validate the end address parameter
        if(start > end) {
            throw new IllegalArgumentException(
                String.format("End address lower the start address.%nStart: 0x%X, End: 0x%X", start, end)
            );  
        }   
        
        // Evaluate the length of data to be stored in memory
        int lenght = Math.min(end, END_ADDRESS) - start + 1;
        if(start + fileSize < lenght) {
            lenght = start + fileSize;
        }   
        
        SystemOut.vprintf("Reading binary file: %s...", binaryFile.getName());
        
        try (java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.FileInputStream(binaryFile))) {
            
            dis.readFully(data);
            BinaryData binaryData = new BinaryData(data, start, lenght);
            binaryData.setBinaryFileName(binaryFile.getName());
            
            SystemOut.vprintf("Ok%n");
            
            return binaryData;
            
        } catch (IOException ioException) {
            SystemOut.vprintf("Error!%n");
            throw ioException;
        }   
    }   
    
    /**
     * Load data from binary file. The start address is the address where the
     * first byte of the binary file will be placed in binary data.
     * The binary data object will be filled with the binary data from file until the end of space is reached
     * (or the end of file)
     * 
     * @param binaryFile the binary file
     * @param start address where the first byte of the binary file will be placed
     * 
     * @return the BinaryData object
     * 
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if occurs any error reading the file
     */
    public static BinaryData fromFile(java.io.File binaryFile, int start) throws FileNotFoundException, IOException {
        int end = Math.min(start + (int) binaryFile.length(), END_ADDRESS);
        return BinaryData.fromFile(binaryFile, start, end);
    }   
    
    /**
     * Load data from binary file. The binary data will be placed starting at first position in memory.
     * The binary data object will be filled with the binary data read from file until the end of space is reached
     * (or the end of file)
     * 
     * @param binaryFile the binary file
     * 
     * @return the BinaryData object
     * 
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if occurs any error reading the file
     */
    public static BinaryData fromFile(java.io.File binaryFile) throws FileNotFoundException, IOException {
        return BinaryData.fromFile(binaryFile, 0);
    }   
}   
