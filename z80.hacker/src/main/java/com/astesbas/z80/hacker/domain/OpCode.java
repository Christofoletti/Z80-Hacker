package com.astesbas.z80.hacker.domain;

/**
 * Op-code validator and translator. To construct a valid OpCode you must provide
 * the opcode matcher string and the resulting mnemonic after the translation.<br/>
 * e.g.:
 * <br/>
 * <code><br/>
 *    OpCode nop = new OpCode("00", "NOP");<br/>
 *    OpCode ldBc = new OpCode("01####", "LD BC,####");<br/>
 *    OpCode ldBCA = new OpCode("02", "LD (BC),A");<br/>
 *    OpCode ldIx = new OpCode("DD36%%##", "LD (IX+%%),##");<br/>
 *    OpCode djnz = new OpCode("10%%", "DJNZ %%");<br/>
 * </code><br/>
 * <br/>
 * <b>Useful documentation:</b><br/>
 *     https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html<br/>
 *     https://regexone.com/<br/>
 *     http://www.z80.info/decoding.htm#upfx
 *     http://z80-heaven.wikidot.com/opcode-reference-chart
 *     
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 05/jun/2017
 */
public class OpCode {
    
    /** The op code (with wildcards ##/%% for operands) in string format */
    private final String opCode;
    
    /** The mnemonic representation of the op-code */
    private final String mnemonicMask;
    
    /** The op-code prefix */
    private final OpCodePrefix prefix;
    
    /** The opcode pattern validator */
    private final java.util.regex.Pattern pattern;
    
    /** The hexadecimal byte representation format */
    private String hexByteFormat = "0%2sH";
    
    /** The hexadecimal word representation format */
    private String hexWordFormat = "0%4sH";
    
    /** Mask for byte parameter */
    private static final String BYTE_PARAM = "##";
    
    /** Mask for byte parameter (relative jump/index displacement - same as byte parameter) */
    private static final String DISPLACEMENT_PARAM = "%%";
    
    /** Mask for word parameter */
    private static final String WORD_PARAM = "####";
    
    /**
     * OpCode class constructor. Must provide op-code and mnemonic strings. 
     * @param opCodeSequence
     * @param mnemonicMask
     */
    public OpCode(String opCode, String mnemonicMask) {
        
        // stores the opcode and the associated mnemonic mask
        this.opCode = opCode.toUpperCase();
        this.mnemonicMask = mnemonicMask.replaceAll("####|##|%%", "%s");
        
        // sets the patter validator using regular expression
        String opCodePattern = this.opCode.replaceAll("##|%%", "[A-F0-9]{2}");
        this.pattern = java.util.regex.Pattern.compile(opCodePattern);
        
        // set the op-code prefix
        this.prefix = OpCodePrefix.of(this.opCode.substring(0, 2));
    }   
    
    /**
     * Return the op-code prefix (if applicable).
     * For non-prefixed instructions, the prefix is NONE.
     * 
     * @return the Z80 op-code prefix
     */
    public OpCodePrefix getPrefix() {
        return this.prefix;
    }   
    
    /**
     * Return the op-code as string.
     * @return the opCode as string.
     */
	public String get() {
        return this.opCode;
	}
    
	/**
     * Get the number of bytes for the op-code.
     * @return the number of bytes of this op-code
     */
    public int getSize() {
        return (this.opCode.length() >> 1);
    }   
    
    /**
     * Sets the hexadecimal format for bytes.
     * @param hexByteFormat the hexByteFormat to set
     */
    public void setHexByteFormat(String hexByteFormat) {
        this.hexByteFormat = hexByteFormat;
    }   
    
    /**
     * Sets the hexadecimal format for words (two bytes).
     * @param hexWordFormat the hexWordFormat to set
     */
    public void setHexWordFormat(String hexWordFormat) {
        this.hexWordFormat = hexWordFormat;
    }   
    
    /**
     * Translates a given sequence of bytes (byte code) into a mnemonic representation.
     * 
     * @param bytes - array of bytes.
     * @return the mnemonic representation for the given byte code.
     * @exception IllegalArgumentException if the given byte array is not a valid sequence for the opcode.
     */
    public String translate(byte[] bytes) {
        return this.translate(OpCode.bytesToHex(bytes));
    }   
    
    /**
     * Translates a given string (hexadecimal digits) into a mnemonic representation.
     * 
     * @param opCode - hexadecimal string representation of an array of bytes (e.g. "01AABB").
     * @return the mnemonic representation for the given hexadecimal string.
     * @exception IllegalArgumentException if the given string is not a valid opcode.
     */
    public String translate(String opCode) {
        
        if (this.matches(opCode)) {
            
            String mnemonic = this.mnemonicMask;
            
            // verify if opcode has a word parameter
            int wordParamIndex = this.opCode.indexOf(WORD_PARAM);
            if(wordParamIndex > -1) {
                
                // the word parameter must by translated using a little endian format
                // (e.g. the byte sequence AABB must be translated to 0BBAAH string)
                String wValue = this.wordToHexString(opCode.substring(wordParamIndex+2, wordParamIndex+4) +
                                opCode.substring(wordParamIndex, wordParamIndex+2));
                mnemonic = String.format(this.mnemonicMask, wValue);
                
            } else {
                
                // get the relative parameter index (if available)
                int displacementIndex = this.opCode.indexOf(DISPLACEMENT_PARAM);
                String displacement = "";
                if(displacementIndex > -1) {
                    displacement = this.byteToHexString(opCode.substring(displacementIndex, displacementIndex+2));
                }	
                
                // get the byte parameter index (if available)
                int dataIndex = this.opCode.indexOf(BYTE_PARAM);
                String data = "";
                if(dataIndex > -1) {
                	data = this.byteToHexString(opCode.substring(dataIndex, dataIndex+2));
                }   
                
                mnemonic = String.format(this.mnemonicMask, displacement, data);
            }
            
        	return mnemonic;
        }   
        
        throw new IllegalArgumentException(
            String.format("Invalid opcode byte sequence (%s) for mnemonic: %s", opCode, this.mnemonicMask)
        );
    }
    
	/**
	 * Verify if the given byte array matches this op-code.
	 * 
	 * @param bytes
	 * @return true if the given byte array is a valid instruction for this op-code.
	 */
	public boolean matches(byte[] bytes) {
        return this.matches(OpCode.bytesToHex(bytes));
    }   
    
    /**
     * Verify if the given String representation of a bytecode matches this op-code. 
     * 
     * @param bytes String containing a sequence of bytes.
     * @return true if the given byte array is a valid instruction for this op-code.
     */
    public boolean matches(String opcode) {
        return this.pattern.matcher(opcode.toUpperCase()).matches();
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
    static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b:bytes) {
            sb.append(String.format("%02X", b));
        }   
        return sb.toString();
    }   
    
    /**
     * Translate a byte string representation to a valid byte representation for the mnemonic.
     * @param hexByte the byte representation
     * @return byte representation for the mnemonic 
     */
    private String byteToHexString(String hexByte) {
        return String.format(this.hexByteFormat, hexByte);
    }   
    
    /**
     * Translate a word string representation to a valid word representation for the mnemonic.
     * @param hexWord the word representation
     * @return word representation for the mnemonic
     */
    private String wordToHexString(String hexWord) {
        return String.format(this.hexWordFormat, hexWord);
    }	
    
    @Override
    public int hashCode() {
        return this.opCode.hashCode();
    }   
    
    @Override
    public boolean equals(Object obj) {
        return this.opCode.equals(obj);
    }   
    
    /**
     * Compare two op-codes based on the sequence of bytes.
     * @param other other OpCode object to compare.
     * @return 0, 1 or -1 (see the String comparator)
     */
    public int compareTo(OpCode other) {
        return this.opCode.compareTo(other.get());
    }   
}
