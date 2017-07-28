package com.astesbas.z80.hacker.domain;

import com.astesbas.z80.hacker.domain.OpCode;
import com.astesbas.z80.hacker.domain.OpCodePrefix;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OpCodeTest extends TestCase {
    
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public OpCodeTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(OpCodeTest.class);
    }   
    
    /**
     * Tests the OpCode matches() methods.
     */
    public void testSimpleMatch() {
        
    	OpCode nop = new OpCode("00", "NOP");
    	assertTrue("Error validating NOP op code!", nop.matches(new byte[]{0x00}));
    	assertTrue("Error validating NOP op code!", nop.matches("00"));
    	assertTrue("Wrong validation of NOP op code!", !nop.matches("0"));
    	assertTrue("Wrong validation of NOP op code!", !nop.matches("000"));
    	assertTrue("Wrong validation of NOP op code!", !nop.matches(new byte[]{0x01}));
    	
    	OpCode ldBc = new OpCode("01####", "LD BC,####");
    	assertTrue("Error validating {0x01, 0x44, 0xFF} op code byte array!", ldBc.matches(new byte[]{0x01, 0x44, (byte) 0xFF}));
    	assertTrue("Error validating 0155AA op code string!", ldBc.matches("0155AA"));
    	assertTrue("Error validating LD BC,#### op code!", ldBc.matches("01aaaa"));
    	assertTrue("Wrong validation of LD BC,#### op code!", !ldBc.matches("0155AABB"));
    	
    	OpCode ldBCA = new OpCode("02", "LD (BC),A");
    	assertTrue("Error validating {0x02} op code byte array!", ldBCA.matches(new byte[]{0x02}));
    	assertTrue("Error validating 02 op code string!", ldBCA.matches("02"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches("2"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches("03"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches("0211"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches(new byte[]{0x01}));
    	
    	OpCode djnz = new OpCode("10%%", "DJNZ %%");
    	assertTrue("Error validating DJNZ op code!", djnz.matches(new byte[]{0x10, 0x00}));
    	assertTrue("Error validating DJNZ op code!", djnz.matches("10A0"));
    	assertTrue("Error validating DJNZ op code!", djnz.matches("10FF"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches("10"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches("100"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches("101122"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches(new byte[]{0x10}));
    	
    	OpCode ldIx = new OpCode("DD36%%##", "LD (IX+%%),##");
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches(new byte[]{(byte) 0xDD, 0x36, 0x7F, 0x22}));
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches("DD360000"));
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches("DD36FFFF"));
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches("DD361122"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD36"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD3600"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD36112233"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches(new byte[]{(byte) 0xDD, 0x36}));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches(new byte[]{(byte) 0xDD, 0x36, 0x55}));
    }	
    
    public void testOpCodePrefixes() {
        
    	OpCode nop = new OpCode("00", "NOP");
    	assertEquals("Error validating NOP op-code size!", 1, nop.getSize());
    	
    	OpCode ldBc = new OpCode("01####", "LD BC,####");
    	assertEquals("Error validating LD BC,#### op-code size!", 3, ldBc.getSize());
    	assertEquals("Error validating LD BC,#### op code prefix!", OpCodePrefix.NONE, ldBc.getPrefix());
    	
    	OpCode ldBCA = new OpCode("02", "LD (BC),A");
    	assertEquals("Error validating LD (BC),A op-code size!", 1, ldBCA.getSize());
    	
    	OpCode djnz = new OpCode("10%%", "DJNZ %%");
    	assertEquals("Error validating DJNZ %% op code prefix!", OpCodePrefix.NONE, djnz.getPrefix());
    	
    	OpCode ldIx = new OpCode("DD36%%##", "LD (IX+%%),##");
    	assertEquals("Error validating LD (IX+%%),## op code prefix!", OpCodePrefix.DD, ldIx.getPrefix());
    	
    }
    
    /**
     * Tests the OpCode translate() methods.
     */
    public void testOpCodeTranslator() {
    	
    	OpCode nop = new OpCode("00", "NOP");
    	String nopMnemonic1 = nop.translate(new byte[]{0x00});
    	assertEquals("Error translating 00H op code (NOP)!", "NOP", nopMnemonic1);
    	String nopMnemonic2 = nop.translate("00");
    	assertEquals("Error translating 00H op code (NOP)!", "NOP", nopMnemonic2);
    	
    	OpCode ldBc = new OpCode("01####", "LD BC,####");
    	String ldBcMnemonic1 = ldBc.translate(new byte[]{0x01, 0x44, (byte) 0xFF});
    	assertEquals("Error translating 0144FF op code (LD BC,0FF44H)!", "LD BC,0FF44H", ldBcMnemonic1);
    	String ldBcMnemonic2 = ldBc.translate("01AABB");
    	assertEquals("Error translating 0144FF op code (LD BC,0BBAAH)!", "LD BC,0BBAAH", ldBcMnemonic2);
    	
    	OpCode ldBCA = new OpCode("02", "LD (BC),A");
    	String ldBCAMnemonic1 = ldBCA.translate(new byte[]{0x02});
    	assertEquals("Error translating 02 op code (LD (BC),A)!", "LD (BC),A", ldBCAMnemonic1);
        String ldBCAMnemonic2 = ldBCA.translate("02");
        assertEquals("Error translating 02 op code (LD (BC),A)!", "LD (BC),A", ldBCAMnemonic2);
        
        OpCode djnz = new OpCode("10%%", "DJNZ %%");
        String djnzMnemonic1 = djnz.translate(new byte[]{0x10, (byte) 0xC0});
        assertEquals("Error translating 10C0 op code (DJNZ 0C0H)!", "DJNZ 0C0H", djnzMnemonic1);
        String djnzMnemonic2 = djnz.translate("10AB");
        assertEquals("Error translating 10AB op code (DJNZ 0ABH)!", "DJNZ 0ABH", djnzMnemonic2);
        
        OpCode ldIx = new OpCode("DD36%%##", "LD (IX+%%),##");
        String ldIxMnemonic1 = ldIx.translate(new byte[]{(byte) 0xDD, (byte) 0x36, 0x5, 0x2});
        assertEquals("Error translating DD360502 op code (LD (IX+05H),02H)!", "LD (IX+005H),002H", ldIxMnemonic1);
        String ldIxMnemonic2 = ldIx.translate("DD3655BB");
        assertEquals("Error translating DD3655BB op code (LD (IX+55H),0BBH)!", "LD (IX+055H),0BBH", ldIxMnemonic2);
    }   
    
    /**
     * Test OpCode.bytesToHex() for valid entries. 
     */
    public void testValidByteToString() {
        
    	// special cases
    	assertEquals("Error validating empty byte array: ", "", OpCode.bytesToHex(new byte[]{}));
    	
    	// regular cases
    	assertEquals("Wrong string format: ", "55", OpCode.bytesToHex(new byte[]{0x55}));
    	assertEquals("Wrong string format: ", "1138", OpCode.bytesToHex(new byte[]{0x11, 0x38}));
        assertEquals("Wrong string format: ", "0102AABB", OpCode.bytesToHex(new byte[]{01, 02, (byte) 0xAA, (byte) 0xBB}));
	}   
    
    /**
     * Test OpCode.bytesToHex() for a null entry.
     */
    public void testNullByteToString() {
    	try {
    		OpCode.bytesToHex(null);
    		throw new AssertionError("Error validating null string test for bytesToHex() method!");
    	} catch(NullPointerException npe) {
            assertNotNull(npe);
        }
	}      
}
