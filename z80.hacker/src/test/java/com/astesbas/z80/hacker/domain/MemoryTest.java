package com.astesbas.z80.hacker.domain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Memory tests.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @since 17/06/2017
 */
public class MemoryTest extends TestCase {
    
    private final byte[] tripleX = new byte[] {'X', 'X', 'X'};
    private final byte[] abc = new byte[] {'A', 'B', 'C'};
    private final byte[] eom = new byte[] {'#', '$', '$'};
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MemoryTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(MemoryTest.class);
    }   
    
    /**
     * Tests the OpCode matches() methods.
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public void testMemoryLoadFromFile() throws URISyntaxException, FileNotFoundException, IOException {
        
        // the memory implementation
        Memory memory = new Memory();
        
        // get binary file resource from classpath
        // other options is MemoryTest.class.getResource("/test.memory.bin");
        java.net.URL filePath = Memory.class.getClassLoader().getResource("test.memory.bin");
        java.io.File binaryFile = new java.io.File(filePath.toURI());
        
        // load the binary file into memory
        memory.loadFromBinaryFile(binaryFile);
        assertEquals("Wrong byte at memory position 0x0000!", '$', memory.get(0));
        assertEquals("Wrong byte at memory position 0x4000!", 'A', memory.get(0x4000));
        assertEquals("Wrong byte at memory position 0xFFFF!", '#', memory.get(0xFFFF));
        assertTrue("Wrong byte sequence at memory position 0x8000!", Arrays.equals(this.tripleX, memory.getBytes(0x8000, 3)));
        
        // test memory access using get and next
        memory.setPointer(0xFFFF);
        assertEquals("Wrong byte at memory position 0xFFFF!", '#', memory.get());
        assertEquals("Wrong byte at memory position 0x0000!", '$', memory.next());
        
        memory.setPointer(0xFFFF);
        assertTrue("Wrong byte sequence at memory position 0xFFFF!", Arrays.equals(this.eom, memory.getBytes(0xFFFF, 3)));
        
    }
    
    /**
     * Tests the memory get() and getBytes() methods.
     * @throws URISyntaxException
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public void testMemoryPartialLoadFromFile() throws URISyntaxException, FileNotFoundException, IOException {
        
        // the memory implementation
        Memory memory = new Memory();
        
        // get binary file resource from classpath
        java.net.URL filePath = MemoryTest.class.getResource("/test.memory.bin");
        java.io.File binaryFile = new java.io.File(filePath.toURI());
        
        // load the binary file into memory
        memory.loadFromBinaryFile(binaryFile, 0x8000);
        
        assertEquals("Wrong byte at memory position 0x0000!", Memory.DEFAULT_BYTE_VALUE, memory.get(0));
        assertEquals("Wrong byte at memory position 0x8000!", '$', memory.get(0x8000));
        assertEquals("Wrong byte at memory position 0x8002!", '$', memory.get(0x8002));
        assertEquals("Wrong byte at memory position 0xBFFF!", 'X', memory.get(0xBFFF));
        assertEquals("Wrong byte at memory position 0xC000!", 'A', memory.get(0xC000));
        assertEquals("Wrong byte at memory position 0xC001!", 'B', memory.get(0xC001));
        assertEquals("Wrong byte at memory position 0xFFFF!", 'Y', memory.get(0xFFFE));
        assertEquals("Wrong byte at memory position 0xFFFF!", 'Z', memory.get(0xFFFF));
        assertTrue("Wrong byte sequence at memory position 0xC000!", Arrays.equals(this.abc, memory.getBytes(0xC000, 3)));
        
        // load the binary file partially into memory
        memory.clear();
        memory.loadFromBinaryFile(binaryFile, 0x4000, 0x8000);
        
        assertEquals("Wrong byte at memory position 0x0000!", Memory.DEFAULT_BYTE_VALUE, memory.get(0));
        assertEquals("Wrong byte at memory position 0x3FFF!", Memory.DEFAULT_BYTE_VALUE, memory.get(0x3FFF));
        assertEquals("Wrong byte at memory position 0x8001!", Memory.DEFAULT_BYTE_VALUE, memory.get(0x8001));
        assertEquals("Wrong byte at memory position 0xC000!", Memory.DEFAULT_BYTE_VALUE, memory.get(0xC000));
        assertEquals("Wrong byte at memory position 0xFFFF!", Memory.DEFAULT_BYTE_VALUE, memory.get(0xFFFF));
        assertEquals("Wrong byte at memory position 0x4000!", '$', memory.get(0x4000));
        assertEquals("Wrong byte at memory position 0x4001!", '$', memory.get(0x4001));
        assertEquals("Wrong byte at memory position 0x4002!", '$', memory.get(0x4002));
        assertTrue("Wrong byte sequence at memory position 0x7FFD!", Arrays.equals(this.tripleX, memory.getBytes(0x7FFD, 3)));
    }
}
