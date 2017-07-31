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
     * Clean the given text line: removes any leading and trailing whitespace and comments.
     * This "cleaning method" implementation is very simple and may not cover every valid case.
     * This method removes all commented text (if applicable).
     *  
     * @param text the text string to be cleaned
     * @param comment the comment char
     * @return the cleaned string
     */
    public static String clean(String text, char comment) {
        
        // verify if the given text contains at least one comment char
        if(text.indexOf(comment) >= 0) {
            
            // flags to detect quotation in the text
            boolean insideQuote = false;
            boolean insideSingleQuote = false;
            
            // search the string for a comment char
            for(int index = 0; index < text.length(); index++) {
                char currentChar = text.charAt(index);
                if(!insideSingleQuote && currentChar == '"') {
                    insideQuote ^= true;
                } else if(!insideQuote && currentChar == '\'') {
                    insideSingleQuote ^= true;
                } else if(currentChar == comment) {
                    // verify if the comment char outside any quotation
                    if(!insideQuote && !insideSingleQuote) {
                        return StringUtil.clean(text.substring(0, index));
                    }   
                }   
            }   
        }   
        
        return StringUtil.clean(text);
        
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
