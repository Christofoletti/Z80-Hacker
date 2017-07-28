package com.astesbas.z80.hacker;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Command line parameters parser tests.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 16/jun/2017
 */
public class CommandLineOptionsParserTest extends TestCase {
    
    /**
     * Create the test case
     * @param testName
     *            name of the test case
     */
    public CommandLineOptionsParserTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(CommandLineOptionsParserTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testCommandLineProcessorTest() {
        assertTrue(true);
    }
}
