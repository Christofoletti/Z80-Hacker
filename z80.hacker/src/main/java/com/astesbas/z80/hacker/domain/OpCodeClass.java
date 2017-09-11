package com.astesbas.z80.hacker.domain;

/**
 * Z80 op-code class prefixes.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * 
 * @version 1.0
 * @since 05/jun/2017
 */
public enum OpCodeClass {
	
    $$, CB, DD, ED, FD;
	
	/**
	 * Return the op code class of the given string.
	 * For UNPREFIXED op codes, this method returns the "$$" string.  
	 * 
	 * @param string the string representation of a byte in hexadecimal
	 * @return the op-code class
	 */
    public static OpCodeClass of(String string) {
        try {
            return valueOf(string);
        } catch(IllegalArgumentException exception) {
            return $$;
        }   
    }   
    
    /**
     * Return the op-code class (prefix) of the given byte.
     * 
     * @param value the byte value
     * @return the op-code class
     */
    public static OpCodeClass of(byte value) {
        return OpCodeClass.of(String.format("%02X", value));
    }   
}
