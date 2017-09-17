package com.astesbas.z80.hacker.domain;

import com.astesbas.z80.hacker.domain.Instruction;
import com.astesbas.z80.hacker.domain.PrefixClass;
import com.astesbas.z80.hacker.util.StringUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class InstructionsTest extends TestCase {
    
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public InstructionsTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(InstructionsTest.class);
    }   
    
    /**
     * Tests the instruction matches() methods.
     */
    public void testSimpleMatch() {
        
    	Instruction nop = new Instruction("00", "NOP");
    	assertTrue("Error validating NOP instruction!", nop.matches(new byte[] {0x00}));
    	assertTrue("Error validating NOP instruction!", nop.matches("00"));
    	assertTrue("Wrong validation of NOP instruction!", !nop.matches("0"));
    	assertTrue("Wrong validation of NOP instruction!", !nop.matches("000"));
    	assertTrue("Wrong validation of NOP instruction!", !nop.matches(new byte[] {0x01}));
    	
    	Instruction ldA = new Instruction("3E##", "LD A,##");
        assertTrue("Error validating {0x3E, 0xFF} instruction byte array!", ldA.matches(new byte[] {0x3E, (byte) 0xFF}));
        assertTrue("Error validating 3EAA instruction string!", ldA.matches("3EAA"));
        assertTrue("Error validating LD A,## instruction!", ldA.matches("3Ebb"));
        assertTrue("Wrong validation of LD A,## instruction!", !ldA.matches("AABB"));
        
    	Instruction ldBc = new Instruction("01####", "LD BC,####");
    	assertTrue("Error validating {0x01, 0x44, 0xFF} instruction byte array!", 
    	        ldBc.matches(new byte[] {0x01, 0x44, (byte) 0xFF}));
    	assertTrue("Error validating 0155AA instruction string!", ldBc.matches("0155AA"));
    	assertTrue("Error validating LD BC,#### instruction!", ldBc.matches("01aaaa"));
    	assertTrue("Wrong validation of LD BC,#### instruction!", !ldBc.matches("0155AABB"));
    	
    	Instruction ldBCA = new Instruction("02", "LD (BC),A");
    	assertTrue("Error validating {0x02} instruction byte array!", ldBCA.matches(new byte[] {0x02}));
    	assertTrue("Error validating 02 instruction string!", ldBCA.matches("02"));
    	assertTrue("Wrong validation of LD (BC),A instruction!", !ldBCA.matches("2"));
    	assertTrue("Wrong validation of LD (BC),A instruction!", !ldBCA.matches("03"));
    	assertTrue("Wrong validation of LD (BC),A instruction!", !ldBCA.matches("0211"));
    	assertTrue("Wrong validation of LD (BC),A instruction!", !ldBCA.matches(new byte[] {0x01}));
    	
    	Instruction djnz = new Instruction("10%%", "DJNZ %%");
    	assertTrue("Error validating DJNZ instruction!", djnz.matches(new byte[] {0x10, 0x00}));
    	assertTrue("Error validating DJNZ instruction!", djnz.matches("10A0"));
    	assertTrue("Error validating DJNZ instruction!", djnz.matches("10FF"));
    	assertTrue("Wrong validation of DJNZ instruction!", !djnz.matches("10"));
    	assertTrue("Wrong validation of DJNZ instruction!", !djnz.matches("100"));
    	assertTrue("Wrong validation of DJNZ instruction!", !djnz.matches("101122"));
    	assertTrue("Wrong validation of DJNZ instruction!", !djnz.matches(new byte[] {0x10}));
    	
    	Instruction ldIx = new Instruction("DD36%%##", "LD (IX+%%),##");
    	assertTrue("Error validating LD (IX+%%),## instruction!", ldIx.matches(new byte[] {(byte) 0xDD, 0x36, 0x7F, 0x22}));
    	assertTrue("Error validating LD (IX+%%),## instruction!", ldIx.matches("DD360000"));
    	assertTrue("Error validating LD (IX+%%),## instruction!", ldIx.matches("DD36FFFF"));
    	assertTrue("Error validating LD (IX+%%),## instruction!", ldIx.matches("DD361122"));
    	assertTrue("Wrong validation of LD (IX+%%),## instruction!", !ldIx.matches("DD"));
    	assertTrue("Wrong validation of LD (IX+%%),## instruction!", !ldIx.matches("DD36"));
    	assertTrue("Wrong validation of LD (IX+%%),## instruction!", !ldIx.matches("DD3600"));
    	assertTrue("Wrong validation of LD (IX+%%),## instruction!", !ldIx.matches("DD36112233"));
    	assertTrue("Wrong validation of LD (IX+%%),## instruction!", !ldIx.matches(new byte[] {(byte) 0xDD, 0x36}));
    	assertTrue("Wrong validation of LD (IX+%%),## instruction!", !ldIx.matches(new byte[] {(byte) 0xDD, 0x36, 0x55}));
    }	
    
    /**
     * 
     */
    public void testOpCodePrefixes() {
        
    	Instruction nop = new Instruction("00", "NOP");
    	assertEquals("Error validating NOP instruction size!", 1, nop.getSize());
    	
    	Instruction ldBc = new Instruction("01####", "LD BC,####");
    	assertEquals("Error validating LD BC,#### instruction size!", 3, ldBc.getSize());
    	assertEquals("Error validating LD BC,#### instruction prefix class!", PrefixClass.$$, ldBc.getPrefixClass());
    	
    	Instruction ldBCA = new Instruction("02", "LD (BC),A");
    	assertEquals("Error validating LD (BC),A instruction size!", 1, ldBCA.getSize());
    	
    	Instruction djnz = new Instruction("10%%", "DJNZ %%");
    	assertEquals("Error validating DJNZ %% instruction prefix class!", PrefixClass.$$, djnz.getPrefixClass());
    	
    	Instruction ldIx = new Instruction("DD36%%##", "LD (IX+%%),##");
    	assertEquals("Error validating LD (IX+%%),## instruction prefix class!", PrefixClass.DD, ldIx.getPrefixClass());
    	
    }
    
    /**
     * Tests the Instruction translate() methods.
     */
    public void testOpCodeTranslator() {
    	
    	Instruction nop = new Instruction("00", "NOP");
    	String nopMnemonic = nop.translate(new byte[] {0});
    	assertEquals("Error translating 00H instruction (NOP)!", "NOP", nopMnemonic);
    	
    	Instruction ldBc = new Instruction("01####", "LD BC,####");
    	String ldBcMnemonic1 = ldBc.translate(new byte[] {0x01, 0x44, (byte) 0xFF});
    	assertEquals("Error translating 01 44 FF instruction (LD BC,0FF44H)!", "LD BC,0FF44H", ldBcMnemonic1);
    	String ldBcMnemonic2 = ldBc.translate(new byte[] {0x01, (byte) 0xAA, (byte) 0xBB});
    	assertEquals("Error translating 01 BB AA instruction (LD BC,0BBAAH)!", "LD BC,0BBAAH", ldBcMnemonic2);
    	String ldBcMnemonic3 = ldBc.translate(new byte[] {0x01, (byte) 0x12, (byte) 0x00});
        assertEquals("Error translating 01 12 00 instruction (LD BC,012H)!", "LD BC,012H", ldBcMnemonic3);
        String ldBcMnemonic4 = ldBc.translate(new byte[] {0x01, (byte) 0x12, (byte) 0x00}, "COUNTER");
        assertEquals("Error translating LD BC,COUNTER!", "LD BC,COUNTER", ldBcMnemonic4);
        
    	Instruction ldBCA = new Instruction("02", "LD (BC),A");
    	String ldBCAMnemonic1 = ldBCA.translate(new byte[] {0x02});
    	assertEquals("Error translating 02 instruction (LD (BC),A)!", "LD (BC),A", ldBCAMnemonic1);
    	String ldBCAMnemonic2 = ldBCA.translate(new byte[] {0x02}, "USELESS LABEL");
    	assertEquals("Error translating LD (BC),A! with useless label", "LD (BC),A", ldBCAMnemonic2);
    	
        Instruction djnz = new Instruction("10%%", "DJNZ %%");
        String djnzMnemonic1 = djnz.translate(new byte[] {0x10, (byte) 0xC0});
        assertEquals("Error translating 10 C0 instruction (DJNZ 0C0H)!", "DJNZ 0C0H", djnzMnemonic1);
        String djnzMnemonic2 = djnz.translate(new byte[] {0x10, (byte) 0xAB});
        assertEquals("Error translating 10 AB instruction (DJNZ 0ABH)!", "DJNZ 0ABH", djnzMnemonic2);
        String djnzMnemonic3 = djnz.translate(new byte[] {0x10, (byte) 0xAB}, "LOOP");
        assertEquals("Error translating DJNZ LOOP!", "DJNZ LOOP", djnzMnemonic3);
        
        Instruction ldIx = new Instruction("DD36%%##", "LD (IX+%%),##");
        String ldIxMnemonic1 = ldIx.translate(new byte[] {(byte) 0xDD, (byte) 0x36, 0x5, 0x2});
        assertEquals("Error translating DD 36 05 02 instruction (LD (IX+05H),02H)!", "LD (IX+05H),02H", ldIxMnemonic1);
        String ldIxMnemonic2 = ldIx.translate(new byte[] {(byte) 0xDD, 0x36, 0x55, (byte) 0xBB});
        assertEquals("Error translating DD 36 55 BB instruction (LD (IX+55H),0BBH)!", "LD (IX+055H),0BBH", ldIxMnemonic2);
        String ldIxMnemonic3 = ldIx.translate(new byte[] {(byte) 0xDD, 0x36, 0x55, (byte) 0xBB}, "HTMI");
        assertEquals("Error translating DD 36 55 BB instruction (LD (IX+HTMI),0BBH)!", "LD (IX+HTMI),0BBH", ldIxMnemonic3);
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
        Instruction i1 = new Instruction("01##", "XYZ ##");
        Instruction i2 = new Instruction("02##", "ABC ##");
        Instruction i3 = new Instruction("02##", "DEF ##");
        Instruction i4 = new Instruction("02##", "DEF ##");
        assertTrue("Instruction i1 not equals null fail!", !i1.equals(null));
        assertTrue("Instruction i1 equals i1 fail!", i1.equals(i1));
        assertTrue("Instruction i1 not equals i2 fail!", !i1.equals(i2));
        assertTrue("Instruction i2 equals i3 fail!", !i2.equals(i3));
        assertTrue("Instruction i3 equals i3 fail!", i3.equals(i4));
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
