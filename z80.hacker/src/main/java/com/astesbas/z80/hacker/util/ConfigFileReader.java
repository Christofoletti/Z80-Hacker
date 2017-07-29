/**
 * 
 */
package com.astesbas.z80.hacker.util;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Config file reader and parser.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 24/jun/2017
 */
public class ConfigFileReader {
    
    /** The predefined keys for the configuration file */
    public static enum ConfigKey {
        BINARY_FILE, BINARY_START, BINARY_END,
        OUTPUT_FILE, OBJECT_FILE, LOG_FILE,
        DB_ALIGN, TAB_SIZE, AUTO_DJNZ_LABELS, AUTO_JR_LABELS, AUTO_JP_LABELS, AUTO_CALL_LABELS,
        LOW_MEM, HIGH_MEM, START_OFF, LABEL, EQU;
    }   
    
    /** Map of keys/values read from file*/
    private Multimap<String, String> multimap = new Multimap<>();
    
    /**
     * Loads the parameters from file.
     * 
     * @param parametersFile the input file
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    public synchronized void load(java.io.File parametersFile) 
            throws java.io.IOException, IllegalArgumentException {
        
        // line of data read from text file
        String line;
        
        // the current line number (used for error messages)
        int lineNumber = 0;
        this.multimap.clear();
        
        SystemOut.vprintln("Reading parameters from project configuration file:");
        
        // FileReader used to read the text file
        FileReader fileReader = new FileReader(parametersFile);
        
        try (java.io.BufferedReader bufferedReader = new java.io.BufferedReader(fileReader)) {
            
            while ((line = bufferedReader.readLine()) != null) {
                
                // update the line number counter
                lineNumber++;
                
                // discard comment lines and empty lines
                line = StringUtil.clean(line);
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }   
                
                // split the line into two strings
                String lineSplit[] = StringUtil.splitInTwo(line, ":");
                if (lineSplit.length > 1) {
                    
                    // get the key:value in line
                    String key = lineSplit[0].trim();
                    String value = lineSplit[1].trim();
                    
                    // put the key and value into the map
                    this.multimap.map(key, value);
                    SystemOut.vprintf("%s: [%s]%n", key, value);
                    
                } else {
                    throw new IllegalArgumentException(
                        String.format("Invalid data at line %n\n\t\"%s\"", lineNumber, line)
                    );
                }   
            }   
        }   
    }
    
    /**
     * Return the number of parameters read from file.
     * @return the number of keys in the multimap
     */
    public int getKeysCount() {
        return this.multimap.size();
    }   
    
    /**
     * Return the number of parameters read from file.
     * @return the number of values for the given key
     */
    public int getCount(Object key) {
        List<String> values = this.multimap.get(key.toString());
        return (values != null) ? values.size():0;
    }   
    
    /**
     * Return the mapped values for the given key.
     * The key parameter is declared as a Object so it is possible to use a Key enum or a regular string as a key.
     * If the key is not mapped, an empty list is returned.
     * 
     * @param key the key
     * @return the list of values associated to the given key
     */
    public List<String> get(Object key) {
        List<String> mappedList = this.multimap.get(key.toString());
        return (mappedList == null) ? new ArrayList<>():mappedList;
    }   
    
//    /**
//     * Return the first mapped value for the given key.
//     * @param key the key
//     * @return the first value associated to the given key
//     */
//    public Optional<String> getFirst(Object key) {
//        return Optional.ofNullable(this.multimap.getFirst(key.toString()));
//    }   
//    
//    /**
//     * Return the last mapped value for the given key.
//     * @param key the key
//     * @return the last value associated to the given key
//     */
//    public Optional<String> getLast(Object key) {
//        return Optional.ofNullable(this.multimap.getLast(key.toString()));
//    }   
    
    /**
     * Return the value as a string for the given key.
     * Note: if there is more than one mapped value for the given key, this method will throw an exception.
     * The key parameter is declared as a Object so it is possible to use a Key enum or a regular string as a key.
     * 
     * @param key the key
     * @return the optional for the string value
     * @throws IllegalAccessException if there is more than one mapped value for the given key
     */
    public Optional<String> getString(Object key) throws IllegalAccessException {
        String value = this.multimap.getSingle(key.toString());
        return Optional.ofNullable(value);
    }   
    
    /**
     * Return the value as a integer for the given key.
     * Note: if there is more than one mapped value for the given key, this method will throw an exception.
     * 
     * @param key the key
     * @return the optional for the integer value
     * @throws IllegalAccessException if there is more than one mapped value for the given key
     */
    public Optional<Integer> getInteger(Object key) throws IllegalAccessException, NumberFormatException {
        String value = this.multimap.getSingle(key.toString());
        if(value instanceof String) {
            return Optional.of(Integer.valueOf(value));
        }   
        return Optional.empty();
    }   
    
    /**
     * Return an hexadecimal value as a integer for the given key.
     * Note: if there is more than one mapped value for the given key, this method will throw an exception.
     * 
     * @param key the key
     * @return the optional for the integer value
     * @throws IllegalAccessException if there is more than one mapped value for the given key
     */
    public Optional<Integer> getAddress(Object key) throws IllegalAccessException, NumberFormatException {
        String value = this.multimap.getSingle(key.toString());
        if(value instanceof String) {
            return Optional.of(Integer.decode(value));
        }   
        return Optional.empty();
    }   
    
    /**
     * Return the value as a boolean for the given key.
     * Note: if there is more than one mapped value for the given key, this method will throw an exception.
     * 
     * @param key the key
     * @return the optional for the boolean value
     * @throws IllegalAccessException if there is more than one mapped value for the given key
     */
    public Optional<Boolean> getBoolean(Object key) throws IllegalAccessException {
        String value = this.multimap.getSingle(key.toString());
        if(value instanceof String) {
            return Optional.of(Boolean.valueOf(value));
        }   
        return Optional.empty();
    }   
}
