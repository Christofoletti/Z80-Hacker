package com.astesbas.z80.hacker.util;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * StringUtil class test.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @since 30/07/2017
 */
public class StringUtilTest extends TestCase {
    
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public StringUtilTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(StringUtilTest.class);
    }   
    
    /**
     * Tests the clean method.
     * @throws IOException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     */
    public void test() {
        
        // validate text line without comment chars
        String noComment = StringUtil.clean("Text without comment!", '#');
        assertEquals("Text without comment!", noComment);
        
        // validate comment text line starting with a comment char
        String startWithComment = StringUtil.clean("# comment line", '#');
        assertEquals("", startWithComment);
        
        // validate comment text line with a comment char not at the start of the line
        String textWithComment = StringUtil.clean("    # comment line", '#');
        assertEquals("", textWithComment);
        
        // validate text line with a comment char
        String commentedLine = StringUtil.clean("text! # comments", '#');
        assertEquals("text!", commentedLine);
        
        // validate text line with a comment char inside a quotation
        String quote1 = StringUtil.clean("text: \"this # is not a comment!\"", '#');
        assertEquals("text: \"this # is not a comment!\"", quote1);
    }   
}
