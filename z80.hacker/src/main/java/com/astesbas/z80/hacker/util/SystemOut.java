/**
 * 
 */
package com.astesbas.z80.hacker.util;

/**
 * The output stream manager.
 * Note: it is possible to redirect the system output to another output by using System.setOut(PrintStream).
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 24/jun/2017
 */
public class SystemOut {
    
    /** The verbose status */
    private static boolean verbose = false;
    
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
            System.out.print(string);
        };  
    }   
    
    /**
     * Output string to output stream if verbose is on.
     * @param string
     */
    public static <T extends Object> void vprintln(T string) {
        if(verbose) {
            System.out.println(string);
        };  
    }   
    
    /**
     * Output string to output stream if verbose is on.
     * @param string
     */
    public static void vprintf(String string, Object... args) {
        if(verbose) {
            System.out.printf(string, args);
        };  
    }   
}
