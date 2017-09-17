package com.astesbas.z80.hacker.domain;

/**
 * Z80 instruction prefix class.
 * This enum defines the prefix class according to the first byte of an instrution.
 * For the Z80 processor, there are four byte values that defines prefixes for instructions.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * 
 * @version 1.0
 * @since 05/jun/2017
 */
public enum PrefixClass {
	
    $$, CB, DD, ED, FD;
	
	/**
	 * Return the instruction's prefix class of the given string.
	 * For UNPREFIXED instructions, this method returns the "$$" string.  
	 * 
	 * @param string the string representation of a byte in hexadecimal
	 * @return the instruction class
	 */
    public static PrefixClass of(String string) {
        try {
            return valueOf(string);
        } catch(IllegalArgumentException exception) {
            return $$;
        }   
    }   
    
    /**
     * Return the instruction's class (prefix) of the given byte.
     * 
     * @param value the byte value
     * @return the instruction class
     */
    public static PrefixClass of(byte value) {
        return PrefixClass.of(String.format("%02X", value));
    }   
}
