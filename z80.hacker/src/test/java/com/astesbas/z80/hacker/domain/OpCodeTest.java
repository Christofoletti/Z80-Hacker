package com.astesbas.z80.hacker.domain;

import com.astesbas.z80.hacker.domain.OpCode;
import com.astesbas.z80.hacker.domain.OpCodeClass;
import com.astesbas.z80.hacker.util.StringUtil;

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
    	assertTrue("Error validating NOP op code!", nop.matches(new byte[] {0x00}));
    	assertTrue("Error validating NOP op code!", nop.matches("00"));
    	assertTrue("Wrong validation of NOP op code!", !nop.matches("0"));
    	assertTrue("Wrong validation of NOP op code!", !nop.matches("000"));
    	assertTrue("Wrong validation of NOP op code!", !nop.matches(new byte[] {0x01}));
    	
    	OpCode ldA = new OpCode("3E##", "LD A,##");
        assertTrue("Error validating {0x3E, 0xFF} op code byte array!", ldA.matches(new byte[] {0x3E, (byte) 0xFF}));
        assertTrue("Error validating 3EAA op code string!", ldA.matches("3EAA"));
        assertTrue("Error validating LD A,## op code!", ldA.matches("3Ebb"));
        assertTrue("Wrong validation of LD A,## op code!", !ldA.matches("AABB"));
        
    	OpCode ldBc = new OpCode("01####", "LD BC,####");
    	assertTrue("Error validating {0x01, 0x44, 0xFF} op code byte array!", 
    	        ldBc.matches(new byte[] {0x01, 0x44, (byte) 0xFF}));
    	assertTrue("Error validating 0155AA op code string!", ldBc.matches("0155AA"));
    	assertTrue("Error validating LD BC,#### op code!", ldBc.matches("01aaaa"));
    	assertTrue("Wrong validation of LD BC,#### op code!", !ldBc.matches("0155AABB"));
    	
    	OpCode ldBCA = new OpCode("02", "LD (BC),A");
    	assertTrue("Error validating {0x02} op code byte array!", ldBCA.matches(new byte[] {0x02}));
    	assertTrue("Error validating 02 op code string!", ldBCA.matches("02"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches("2"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches("03"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches("0211"));
    	assertTrue("Wrong validation of LD (BC),A op code!", !ldBCA.matches(new byte[] {0x01}));
    	
    	OpCode djnz = new OpCode("10%%", "DJNZ %%");
    	assertTrue("Error validating DJNZ op code!", djnz.matches(new byte[] {0x10, 0x00}));
    	assertTrue("Error validating DJNZ op code!", djnz.matches("10A0"));
    	assertTrue("Error validating DJNZ op code!", djnz.matches("10FF"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches("10"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches("100"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches("101122"));
    	assertTrue("Wrong validation of DJNZ op code!", !djnz.matches(new byte[] {0x10}));
    	
    	OpCode ldIx = new OpCode("DD36%%##", "LD (IX+%%),##");
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches(new byte[] {(byte) 0xDD, 0x36, 0x7F, 0x22}));
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches("DD360000"));
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches("DD36FFFF"));
    	assertTrue("Error validating LD (IX+%%),## op code!", ldIx.matches("DD361122"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD36"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD3600"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches("DD36112233"));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches(new byte[] {(byte) 0xDD, 0x36}));
    	assertTrue("Wrong validation of LD (IX+%%),## op code!", !ldIx.matches(new byte[] {(byte) 0xDD, 0x36, 0x55}));
    }	
    
    /**
     * 
     */
    public void testOpCodePrefixes() {
        
    	OpCode nop = new OpCode("00", "NOP");
    	assertEquals("Error validating NOP op-code size!", 1, nop.getSize());
    	
    	OpCode ldBc = new OpCode("01####", "LD BC,####");
    	assertEquals("Error validating LD BC,#### op-code size!", 3, ldBc.getSize());
    	assertEquals("Error validating LD BC,#### op code prefix!", OpCodeClass.$$, ldBc.getOpCodeClass());
    	
    	OpCode ldBCA = new OpCode("02", "LD (BC),A");
    	assertEquals("Error validating LD (BC),A op-code size!", 1, ldBCA.getSize());
    	
    	OpCode djnz = new OpCode("10%%", "DJNZ %%");
    	assertEquals("Error validating DJNZ %% op code prefix!", OpCodeClass.$$, djnz.getOpCodeClass());
    	
    	OpCode ldIx = new OpCode("DD36%%##", "LD (IX+%%),##");
    	assertEquals("Error validating LD (IX+%%),## op code prefix!", OpCodeClass.DD, ldIx.getOpCodeClass());
    	
    }
    
    /**
     * Tests the OpCode translate() methods.
     */
    public void testOpCodeTranslator() {
    	
    	OpCode nop = new OpCode("00", "NOP");
    	String nopMnemonic = nop.translate(new byte[] {0});
    	assertEquals("Error translating 00H op code (NOP)!", "NOP", nopMnemonic);
    	
    	OpCode ldBc = new OpCode("01####", "LD BC,####");
    	String ldBcMnemonic1 = ldBc.translate(new byte[] {0x01, 0x44, (byte) 0xFF});
    	assertEquals("Error translating 01 44 FF op code (LD BC,0FF44H)!", "LD BC,0FF44H", ldBcMnemonic1);
    	String ldBcMnemonic2 = ldBc.translate(new byte[] {0x01, (byte) 0xAA, (byte) 0xBB});
    	assertEquals("Error translating 01 BB AA op code (LD BC,0BBAAH)!", "LD BC,0BBAAH", ldBcMnemonic2);
    	String ldBcMnemonic3 = ldBc.translate(new byte[] {0x01, (byte) 0x12, (byte) 0x00});
        assertEquals("Error translating 01 12 00 op code (LD BC,012H)!", "LD BC,012H", ldBcMnemonic3);
        String ldBcMnemonic4 = ldBc.translate(new byte[] {0x01, (byte) 0x12, (byte) 0x00}, "COUNTER");
        assertEquals("Error translating LD BC,COUNTER!", "LD BC,COUNTER", ldBcMnemonic4);
        
    	OpCode ldBCA = new OpCode("02", "LD (BC),A");
    	String ldBCAMnemonic1 = ldBCA.translate(new byte[] {0x02});
    	assertEquals("Error translating 02 op code (LD (BC),A)!", "LD (BC),A", ldBCAMnemonic1);
    	String ldBCAMnemonic2 = ldBCA.translate(new byte[] {0x02}, "USELESS LABEL");
    	assertEquals("Error translating LD (BC),A! with useless label", "LD (BC),A", ldBCAMnemonic2);
    	
        OpCode djnz = new OpCode("10%%", "DJNZ %%");
        String djnzMnemonic1 = djnz.translate(new byte[] {0x10, (byte) 0xC0});
        assertEquals("Error translating 10 C0 op code (DJNZ 0C0H)!", "DJNZ 0C0H", djnzMnemonic1);
        String djnzMnemonic2 = djnz.translate(new byte[] {0x10, (byte) 0xAB});
        assertEquals("Error translating 10 AB op code (DJNZ 0ABH)!", "DJNZ 0ABH", djnzMnemonic2);
        String djnzMnemonic3 = djnz.translate(new byte[] {0x10, (byte) 0xAB}, "LOOP");
        assertEquals("Error translating DJNZ LOOP!", "DJNZ LOOP", djnzMnemonic3);
        
        OpCode ldIx = new OpCode("DD36%%##", "LD (IX+%%),##");
        String ldIxMnemonic1 = ldIx.translate(new byte[] {(byte) 0xDD, (byte) 0x36, 0x5, 0x2});
        assertEquals("Error translating DD 36 05 02 op code (LD (IX+05H),02H)!", "LD (IX+05H),02H", ldIxMnemonic1);
        String ldIxMnemonic2 = ldIx.translate(new byte[] {(byte) 0xDD, 0x36, 0x55, (byte) 0xBB});
        assertEquals("Error translating DD 36 55 BB op code (LD (IX+55H),0BBH)!", "LD (IX+055H),0BBH", ldIxMnemonic2);
        String ldIxMnemonic3 = ldIx.translate(new byte[] {(byte) 0xDD, 0x36, 0x55, (byte) 0xBB}, "HTMI");
        assertEquals("Error translating DD 36 55 BB op code (LD (IX+HTMI),0BBH)!", "LD (IX+HTMI),0BBH", ldIxMnemonic3);
    }   
    
    /**
     * Test OpCode.bytesToHex() for valid entries. 
     */
    public void testValidByteToString() {
        
    	// special cases
    	assertEquals("Error validating empty byte array!", "", StringUtil.bytesToHex(new byte[]{}));
    	
    	// regular cases
    	assertEquals("Wrong string format: 55", "55", StringUtil.bytesToHex(new byte[] {0x55}));
    	assertEquals("Wrong string format: 1138", "1138", StringUtil.bytesToHex(new byte[] {0x11, 0x38}));
        assertEquals("Wrong string format: 0102AABB", "0102AABB", 
                StringUtil.bytesToHex(new byte[] {01, 02, (byte) 0xAA, (byte) 0xBB}));
        
        // test equals method
        OpCode op1 = new OpCode("01##", "XYZ ##");
        OpCode op2 = new OpCode("02##", "ABC ##");
        OpCode op3 = new OpCode("02##", "DEF ##");
        assertTrue("OpCode op1 not equals null fail!", !op1.equals(null));
        assertTrue("OpCode op1 equals op1 fail!", op1.equals(op1));
        assertTrue("OpCode op1 not equals op2 fail!", !op1.equals(op2));
        assertTrue("OpCode op2 equals op3 fail!", op2.equals(op3));
	}   
    
    /**
     * Test OpCode.bytesToHex() for a null entry.
     */
    public void testNullByteToString() {
    	try {
    	    StringUtil.bytesToHex(null);
    		throw new AssertionError("Error validating null string test for bytesToHex() method!");
    	} catch(NullPointerException npe) {
            assertNotNull(npe);
        }
	}   
}
