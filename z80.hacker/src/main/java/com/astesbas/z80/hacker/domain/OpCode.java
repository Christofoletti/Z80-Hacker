package com.astesbas.z80.hacker.domain;

import com.astesbas.z80.hacker.util.StringUtil;

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
 *     s.replaceFirst("^0+(?!$)", "")
 *     
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 05/jun/2017
 */
public class OpCode implements java.io.Serializable {
    
    /** Serial version UID for serialization */
    private static final long serialVersionUID = 4409454175330043125L;
    
    /** The op code mask (with wildcards ##/%% for operands) in string format */
    private final String opCodeMask;
    
    /** The mnemonic representation of the op-code */
    private final String mnemonicMask;
    
    /** The op-code class (instruction's prefix byte) */
    private final OpCodeClass opCodeClass;
    
    /** The index of the displacement parameter (if applicable) */
    private final int displacementIndex;
    
    /** The index of the data parameter (if applicable) */
    private final int dataIndex;
    
    /** The opcode pattern validator */
    private final java.util.regex.Pattern pattern;
    
    /** Mask for byte parameter */
    private static final String BYTE_PARAM = "##";
    
    /** Mask for word parameter */
    private static final String WORD_PARAM = "####";
    
    /** Mask for byte parameter (relative jump/index displacement - same as byte parameter) */
    private static final String DISPLACEMENT_PARAM = "%%";
    
    /**
     * OpCode class constructor. Must provide op-code and mnemonic strings. 
     * @param opCodeMask
     * @param mnemonicMask
     */
    public OpCode(String opCodeMask, String mnemonicMask) {
        
        // set the indexes of displacement and data parameters
        this.displacementIndex = opCodeMask.indexOf(DISPLACEMENT_PARAM) >> 1;
        this.dataIndex = opCodeMask.indexOf(BYTE_PARAM) >> 1;
        
        // stores the opcode and the associated mnemonic mask 
        this.opCodeMask = opCodeMask.toUpperCase();
        this.mnemonicMask = mnemonicMask.replaceAll("####|##|%%", "%s");
        
        // sets the pattern validator using regular expression
        String opCodeRegexMatcher = this.opCodeMask.replaceAll("##|%%", "[A-F0-9]{2}");
        this.pattern = java.util.regex.Pattern.compile(opCodeRegexMatcher);
        
        // set the opcode prefix for this opcode instance
        this.opCodeClass = OpCodeClass.of(this.opCodeMask.substring(0, 2));
    }   
    
    /**
     * Return the op-code class (if applicable).
     * For non-prefixed instructions, the prefix is $$.
     * 
     * @see OpCodeClass
     * @return the Z80 op-code prefix
     */
    public OpCodeClass getOpCodeClass() {
        return this.opCodeClass;
    }   
    
    /**
     * Return the op-code mask as string.
     * @return the op code mask
     */
    public String getOpCodeMask() {
        return this.opCodeMask;
    }   
    
    /**
     * Return the mnemonic mask as string.
     * @return the mnemonic mask
     */
    public String getMnemonicMask() {
        return this.mnemonicMask;
    }   
    
	/**
     * Get the number of bytes for the op-code.
     * @return the number of bytes of this op-code
     */
    public int getSize() {
        return (this.opCodeMask.length() >> 1);
    }   
    
    /**
     * Translates a given sequence of bytes (byte code) into a mnemonic representation.
     * If a non empty string label is provided, then it will replace the word or displacement byte in the
     * resulting mnemonic.
     * 
     * @param bytes the array of bytes (opcode sequence)
     * @param label the label to be used in the mnemonic, if applicable
     * @return the mnemonic representation for the given byte code.
     * @exception IllegalArgumentException if the given byte array is not a valid sequence for the opcode,
     * or the label parameter is null.
     */
    public String translate(byte[] bytes, String label) {
        
        if(!this.matches(bytes) || label == null) {
            throw new IllegalArgumentException(
                String.format("Invalid translation of opcode (%s) for mnemonic: %s and label %s",
                        StringUtil.bytesToHex(bytes), this.mnemonicMask, label)
            );  
        }   
        
        // initializes the mnemonic string
        String mnemonic = this.mnemonicMask;
        
        // format the mnemonic using the word parameter index (if available)
        if (this.opCodeMask.contains(WORD_PARAM)) {
            
            // the word parameter must by translated using a little endian format
            if(label.isEmpty()) {
                String wordValue = StringUtil.wordToHexString(bytes[this.dataIndex], bytes[this.dataIndex+1]);
                mnemonic = String.format(this.mnemonicMask, wordValue);
            } else {
                mnemonic = String.format(this.mnemonicMask, label);
            }   
            
        } else {
            
            // get the data parameter (if available)
            // NOTE: the dataIndex in general is greater than zero. (exception for db xx, which is not an opcode)
            String data = (this.dataIndex >= 0) ? StringUtil.byteToHexString(bytes[this.dataIndex]):"";
            
            // set the mnemonic according to the parameters available
            // note that the data string may be empty
            if (this.displacementIndex < 0) {
                mnemonic = String.format(this.mnemonicMask, data);
            } else {
                String displacement = (this.displacementIndex > 0 && label.isEmpty()) ? 
                        StringUtil.byteToHexString(bytes[this.displacementIndex]):label;
                mnemonic = String.format(this.mnemonicMask, displacement, data);
            }   
        }   
        
        return mnemonic;
    }   
    
    /**
     * Translate the op code without label.
     * 
     * @param bytes op code sequence of bytes
     * @return the formatted mnemonic for the given op code
     */
    public String translate(byte[] bytes) {
        return this.translate(bytes, "");
    }   
    
	/**
	 * Verify if the given byte array matches this op-code.
	 * 
	 * @param bytes
	 * @return true if the given byte array is a valid instruction for this op-code.
	 */
	public boolean matches(byte[] bytes) {
        return this.matches(StringUtil.bytesToHex(bytes));
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
    
    @Override
    public int hashCode() {
        return this.opCodeMask.hashCode();
    }   
    
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof OpCode) && 
               this.opCodeMask.equals(((OpCode) obj).getOpCodeMask()) &&
               this.mnemonicMask.equals(((OpCode) obj).getMnemonicMask());
    }   
    
    /**
     * Compare two op-codes based on the sequence of bytes.
     * 
     * @param other other OpCode object to compare.
     * @return 0, 1 or -1 (see the String comparator)
     */
    public int compareTo(OpCode other) {
        return this.opCodeMask.compareTo(other.getOpCodeMask());
    }   
    
    @Override
    public String toString() {
        return this.mnemonicMask;
    }
}
