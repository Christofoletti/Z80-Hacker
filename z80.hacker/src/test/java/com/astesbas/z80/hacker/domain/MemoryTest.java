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
        
        // get binary file resource from classpath
        // other options is MemoryTest.class.getResource("/test.memory.bin");
        java.net.URL filePath = BinaryData.class.getClassLoader().getResource("test.memory.bin");
        java.io.File binaryFile = new java.io.File(filePath.toURI());
        
        // load data from binary file
        BinaryData binaryData = BinaryData.fromFile(binaryFile);
        
        // 
        assertEquals("Wrong byte at memory position 0x0000!", '$', binaryData.get(0));
        assertEquals("Wrong byte at memory position 0x4000!", 'A', binaryData.get(0x4000));
        assertEquals("Wrong byte at memory position 0xFFFF!", '#', binaryData.get(0xFFFF));
        assertTrue("Wrong byte sequence at memory position 0x8000!", Arrays.equals(this.tripleX, binaryData.getBytes(0x8000, 3)));
        
        // test memory access using get and next
        binaryData.setPointer(0xFFFF);
        assertEquals("Wrong byte at memory position 0xFFFF!", '#', binaryData.get());
        assertEquals("Wrong byte at memory position 0x0000!", '$', binaryData.next());
        
        binaryData.setPointer(0xFFFF);
        //assertTrue("Wrong byte sequence at memory position 0xFFFF!", Arrays.equals(this.eom, binaryData.getBytes(0xFFFF, 3)));
        
    }
    
    /**
     * Tests the memory get() and getBytes() methods.
     * @throws URISyntaxException
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public void testMemoryPartialLoadFromFile() throws URISyntaxException, FileNotFoundException, IOException {
        
        // get binary file resource from classpath
        java.net.URL filePath = MemoryTest.class.getResource("/test.memory.bin");
        java.io.File binaryFile = new java.io.File(filePath.toURI());
        
        // load the binary file into memory
        BinaryData binaryData = BinaryData.fromFile(binaryFile, 0x8000);
        
        assertEquals("Wrong byte at memory position 0x0000!", BinaryData.DEFAULT_BYTE_VALUE, binaryData.get(0));
        assertEquals("Wrong byte at memory position 0x8000!", '$', binaryData.get(0x8000));
        assertEquals("Wrong byte at memory position 0x8002!", '$', binaryData.get(0x8002));
        assertEquals("Wrong byte at memory position 0xBFFF!", 'X', binaryData.get(0xBFFF));
        assertEquals("Wrong byte at memory position 0xC000!", 'A', binaryData.get(0xC000));
        assertEquals("Wrong byte at memory position 0xC001!", 'B', binaryData.get(0xC001));
        assertEquals("Wrong byte at memory position 0xFFFF!", 'Y', binaryData.get(0xFFFE));
        assertEquals("Wrong byte at memory position 0xFFFF!", 'Z', binaryData.get(0xFFFF));
        assertTrue("Wrong byte sequence at memory position 0xC000!", Arrays.equals(this.abc, binaryData.getBytes(0xC000, 3)));
        
        // load the binary file partially into memory
        binaryData = BinaryData.fromFile(binaryFile, 0x4000, 0x8000);
        
        assertEquals("Wrong byte at memory position 0x0000!", BinaryData.DEFAULT_BYTE_VALUE, binaryData.get(0));
        assertEquals("Wrong byte at memory position 0x3FFF!", BinaryData.DEFAULT_BYTE_VALUE, binaryData.get(0x3FFF));
        assertEquals("Wrong byte at memory position 0x8001!", BinaryData.DEFAULT_BYTE_VALUE, binaryData.get(0x8001));
        assertEquals("Wrong byte at memory position 0xC000!", BinaryData.DEFAULT_BYTE_VALUE, binaryData.get(0xC000));
        assertEquals("Wrong byte at memory position 0xFFFF!", BinaryData.DEFAULT_BYTE_VALUE, binaryData.get(0xFFFF));
        assertEquals("Wrong byte at memory position 0x4000!", '$', binaryData.get(0x4000));
        assertEquals("Wrong byte at memory position 0x4001!", '$', binaryData.get(0x4001));
        assertEquals("Wrong byte at memory position 0x4002!", '$', binaryData.get(0x4002));
        assertTrue("Wrong byte sequence at memory position 0x7FFD!", Arrays.equals(this.tripleX, binaryData.getBytes(0x7FFD, 3)));
    }
}
