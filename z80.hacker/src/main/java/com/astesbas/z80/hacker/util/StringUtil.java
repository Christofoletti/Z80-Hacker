package com.astesbas.z80.hacker.util;

/**
 * General string utilities.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 30/jun/2017
 */
public class StringUtil {
    
    /** Avoid instantiation of this class */
    private StringUtil() {};
    
    /**
     * Splits a string into two strings breaking the original string at the point
     * of the first occurrence of the delimiter. If the delimiter string is not present
     * in the string, then an empty string array is returned.
     * 
     * @param string the input string to be splited in two strings
     * @param delimiter the delimiter string used as separator
     * @return an array containing the two strings resulting from split  
     */
    public static String[] splitInTwo(String string, String delimiter) {
        int splitIndex = string.indexOf(delimiter);
        if(splitIndex > 0) {
            return new String[] {
                string.substring(0, splitIndex), string.substring(splitIndex+1)
            };  
        }   
        return new String[] {};
    }   
    
    /**
     * Cleanup the given string text by replacing tabs by spaces and removing any leading and trailing spaces.
     * @param text the input text to be "cleaned"
     * @return the "cleaned "text
     */
    public static String clean(String text) {
        return text.replace('\t', ' ').trim();
    }   
}   
