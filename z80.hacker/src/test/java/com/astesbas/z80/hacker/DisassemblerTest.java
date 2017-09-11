package com.astesbas.z80.hacker;

import com.astesbas.z80.hacker.engine.Z80Disassembler;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Disassembler tests.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 02/sep/2017
 */
public class DisassemblerTest extends TestCase {
    
    private Z80Disassembler disassembler = new Z80Disassembler();
    
    /**
     * Create the test case
     * @param testName
     *            name of the test case
     */
    public DisassemblerTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(DisassemblerTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testDisassembler() {
        
        //this.disassembler.setLowMem(-1);
        this.disassembler.setLowMem(0);
        this.disassembler.setLowMem(0xFFFF);
        //this.disassembler.setLowMem(0x1FFFF);
        
        //System.out.printf("Setting low mem: 0x%X%n", this.lowMem);
    }   
}
