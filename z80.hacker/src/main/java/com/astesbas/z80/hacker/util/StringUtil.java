package com.astesbas.z80.hacker.util;

import java.util.Objects;

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
    
    /** The hexadecimal value representation format */
    private static String hexValueFormat = "0%sH";
    
    /** Avoid instantiation of this class */
    private StringUtil() {};
    
    /**
     * Sets the hexadecimal format for byte/word data.
     * @param hexValueFormat the hexadecimal format to set
     */
    public static void setHexValueFormat(String hexValueFormat) {
        StringUtil.hexValueFormat = Objects.requireNonNull(hexValueFormat);
    }   
    
    /**
     * Splits a string into two strings breaking the original string at the point
     * of the first occurrence of the delimiter. If the delimiter string is not present
     * in the parameter string, then an empty string array is returned.
     * 
     * @param string the input string to be splitted in two strings
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
     * 
     * @param text the input text to be "cleaned"
     * @return the "cleaned "text
     */
    public static String clean(String text) {
        return text.replace('\t', ' ').trim();
    }   
    
    /**
     * Generate an string of spaces the given size.
     * 
     * @param count the size of the string to be generated
     * @return the string of spaces
     */
    public static String spaces(int count) {
        return new String(new char[count]).replace('\0', ' ');
    }   
    
    /**
     * Verify if the given string is inside quotes.
     * 
     * @param text the text to be verified to be quoted
     * @return true if the given string is inside quotes
     */
    public static boolean isQuoted(String text) {
        String trimmedText = text.trim();
        return (trimmedText.startsWith("\"") && trimmedText.endsWith("\""));
    }   
    
    /**
     * 
     * @param text
     * @return
     */
    public static String removeQuotes(String text) {
        if(text.contains("\"")) {
            return text.substring(text.indexOf("\""), text.lastIndexOf("\""));
        } else {
            return text;
        }
    }   
    
    /**
     * Convert an array of bytes to a hexadecimal string.
     * Example: 
     *     input:  byte[] {01, AA, BB}
     *     output: String "01AABB"
     * 
     * @param bytes array of bytes
     * @param separator string to be inserted between bytes
     * @return String representation of the byte array in hexadecimal format.
     */
    public static String bytesToHex(byte[] bytes, String separator) {
        StringBuilder sb = new StringBuilder();
        for(byte b:bytes) {
            sb.append(String.format("%02X", b)).append(separator);
        }   
        return sb.toString();
    }   
    
    /**
     * Convert an array of bytes to a hexadecimal string.
     * Example: 
     *     input:  byte[] {01, AA, BB}
     *     output: String "01AABB"
     * 
     * @param bytes array of bytes
     * @return String representation of the byte array in hexadecimal format.
     */
    public static String bytesToHex(byte[] bytes) {
        return StringUtil.bytesToHex(bytes, "");
    }   
    
    /**
     * Converts a byte into a string representation in hexadecimal.
     * 
     * @param value the byte value
     * @return the string representation of the byte value in hexadecimal
     */
    public static String byteToHexString(byte value) {
        return StringUtil.intToHexString(value & 0xFF);
    }   
    
    /**
     * Converts two bytes (LSB and MSB) into a string representation in hexadecimal (word).
     * 
     * @param lsb least significant byte
     * @param msb most significant byte
     * @return the string representation of the word value in hexadecimal
     */
    public static String wordToHexString(byte lsb, byte msb) {
        return StringUtil.intToHexString((lsb & 0xFF) | ((msb << 8) & 0xFFFF));
    }   
    
    /**
     * Converts an integer into a string representation in hexadecimal.
     * 
     * @param value the byte value
     * @return the string representation of the word value in hexadecimal
     */
    public static String intToHexString(int value) {
        String byteString = String.format("%X", value).replaceFirst("^0*", "");
        return String.format(StringUtil.hexValueFormat, byteString);
    }   
}   
