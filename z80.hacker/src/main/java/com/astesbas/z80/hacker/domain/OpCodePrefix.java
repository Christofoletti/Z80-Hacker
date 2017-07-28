package com.astesbas.z80.hacker.domain;

/**
 * Z80 op-code prefixes.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * 
 * @version 1.0
 * @since 05/jun/2017
 */
public enum OpCodePrefix {
	
	CB, DD, ED, FD, NONE;
	
	/**
	 * Return the op-code prefix of the given string.
	 * 
	 * @param string the string representation of a byte in hexadecimal
	 * @return the op-code prefix
	 */
    public static OpCodePrefix of(String string) {
        try {
            return valueOf(string);
        } catch(IllegalArgumentException exception) {
            return NONE;
        }   
    }   
    
    /**
     * Return the op-code prefix of the given byte.
     * 
     * @param value the byte value
     * @return the op-code prefix
     */
    public static OpCodePrefix of(byte value) {
       return OpCodePrefix.of(String.valueOf(value));
    }   
}
