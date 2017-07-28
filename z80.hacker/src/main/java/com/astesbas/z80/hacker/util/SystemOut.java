/**
 * 
 */
package com.astesbas.z80.hacker.util;

import java.io.PrintStream;

/**
 * The output stream manager.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 24/jun/2017
 */
public class SystemOut {
    
    /** The output stream (default is System.out) */
    private static PrintStream output = System.out;
    
    /** The verbose status */
    private static boolean verbose = false;
    
    /**
     * Get the current output stream;
     * @return the PrintStream
     */
    public static PrintStream get() {
        return SystemOut.output;
    }   
    
    /**
     * Redirects the output stream.
     * @param out the output stream.
     */
    public static void setOut(PrintStream out) {
        System.setOut(out);
    }   
    
    /**
     * Sets the verbose status.
     * @param status
     */
    public static void setVerbose(boolean status) {
        SystemOut.verbose = status;
    }   
    
    /**
     * Output string to output stream if verbose is on.
     * @param string
     */
    public static <T extends Object> void vprint(T string) {
        if(verbose) {
            SystemOut.output.print(string);
        };  
    }   
    
    /**
     * Output string to output stream if verbose is on.
     * @param string
     */
    public static <T extends Object> void vprintln(T string) {
        if(verbose) {
            SystemOut.output.println(string);
        };  
    }   
    
    /**
     * Output string to output stream if verbose is on.
     * @param string
     */
    public static void vprintf(String string, Object... args) {
        if(verbose) {
            SystemOut.output.printf(string, args);
        };  
    }   
}
