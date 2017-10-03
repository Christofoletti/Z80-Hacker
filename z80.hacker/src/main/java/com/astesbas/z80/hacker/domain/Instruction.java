package com.astesbas.z80.hacker.domain;

import java.util.Objects;

import com.astesbas.z80.hacker.util.StringUtil;

/**
 * Instruction validator and translator. To construct a valid Instruction you must provide
 * the binary data matcher string and the resulting mnemonic after the translation.<br/>
 * e.g.:
 * <br/>
 * <code><br/>
 *    Instruction nop = new Instruction("00", "NOP");<br/>
 *    Instruction ldBc = new Instruction("01####", "LD BC,####");<br/>
 *    Instruction ldBCA = new Instruction("02", "LD (BC),A");<br/>
 *    Instruction ldIx = new Instruction("DD36%%##", "LD (IX+%%),##");<br/>
 *    Instruction djnz = new Instruction("10%%", "DJNZ %%");<br/>
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
public class Instruction implements java.io.Serializable {
    
    /** Serial version UID for serialization */
    private static final long serialVersionUID = 4409454175330043125L;
    
    /** The instruction byte mask (with wild cards ##/%% for operands) in string format */
    private final String byteMask;
    
    /** The mnemonic representation of the instruction */
    private final String mnemonicMask;
    
    /** The instruction prefix class (instruction's prefix byte) */
    private final PrefixClass prefixClass;
    
    /** The index of the displacement parameter (if applicable) */
    private final int displacementIndex;
    
    /** The index of the data parameter (if applicable) */
    private final int dataIndex;
    
    /** The size of the instruction (in bytes) */
    private final int size;
    
    /** Flag that indicates that this is an undocumented z80 instruction */
    private final boolean isUndocumented;
    
    /** The opcode pattern validator */
    private final java.util.regex.Pattern pattern;
    
    /** Mask for byte parameter */
    private static final String BYTE_PARAM = "##";
    
    /** Mask for word parameter */
    private static final String WORD_PARAM = "####";
    
    /** Mask for byte parameter (relative jump/index displacement - same as byte parameter) */
    private static final String DISPLACEMENT_PARAM = "%%";
    
    /** This is a placeholder for data bytes (to be output as db ##) */
    public static final Instruction DB_BYTE = new Instruction("##", "db ##");
    
    /** This is a placeholder for bytes that are part of instructions (opcode or instruction's parameter) */
    public static final Instruction PARAMTER = new Instruction("##", "[##]");
    
    /**
     * OpCode class constructor. Must provide byte and mnemonic mask strings. 
     * @param byteMask
     * @param mnemonicMask
     */
    public Instruction(String byteMask, String mnemonicMask) {
        
        // Set the indexes of displacement and data parameters
        this.displacementIndex = byteMask.indexOf(DISPLACEMENT_PARAM) >> 1;
        this.dataIndex = byteMask.indexOf(BYTE_PARAM) >> 1;
        
        // Stores the opcode and the associated mnemonic mask 
        this.byteMask = byteMask.toUpperCase().trim();
        this.mnemonicMask = mnemonicMask.replaceAll("####|##|%%", "%s").replace("*", "").trim();
        
        // Set the pattern validator using regular expression
        String opCodeRegexMatcher = this.byteMask.replaceAll("##|%%", "[A-F0-9]{2}");
        this.pattern = java.util.regex.Pattern.compile(opCodeRegexMatcher);
        
        // Set the general properties of the instruction
        this.prefixClass = PrefixClass.of(this.byteMask.substring(0, 2));
        this.size = (this.byteMask.length() >> 1);
        this.isUndocumented = mnemonicMask.contains("*");
    }   
    
    /**
     * Return the instruction's prefix class (if applicable).
     * For non-prefixed instructions, the prefix is $$.
     * 
     * @see PrefixClass
     * @return the Z80 op-code prefix
     */
    public PrefixClass getPrefixClass() {
        return this.prefixClass;
    }   
    
    /**
     * Return the byte mask as string.
     * @return the byte mask
     */
    public String getByteMask() {
        return this.byteMask;
    }   
    
    /**
     * Return the mnemonic mask as string.
     * @return the mnemonic mask
     */
    public String getMnemonicMask() {
        return this.mnemonicMask;
    }   
    
	/**
     * Get the number of bytes that defines this instruction.
     * @return the number of bytes of this instruction
     */
    public int getSize() {
        return this.size;
    }   
    
    /**
     * Return true if the instruction has a word parameter.
     * @return true for instructions with word parameter
     */
    public boolean hasWordParameter() {
        return this.byteMask.contains(WORD_PARAM);
    }   
    
    /**
     * Return true if the instruction has a displacement parameter.
     * @return true for instructions with displacement parameter
     */
    public boolean hasDisplacementParameter() {
        return (this.displacementIndex > 0);
    }   
    
    /**
     * Get the status that indicates if this instruction is officially documented.
     * @return true if the instruction is not officially documented
     */
    public boolean isUndocumented() {
        return this.isUndocumented;
    }   
    
    /**
     * Translates a given sequence of bytes (byte code) into a mnemonic representation.
     * If a non-empty string label is provided, then it will replace the word or displacement byte in the
     * resulting mnemonic.
     * 
     * @param bytes the array of bytes (bytes that defines the instruction)
     * @param label the label to be used in the mnemonic, if applicable
     * @return the mnemonic representation for the given byte code.
     * @exception IllegalArgumentException if the given byte array is not a valid sequence for the opcode,
     * or the label parameter is null.
     */
    public String translate(byte[] bytes, String label) {
        
        if(!this.matches(bytes) || Objects.isNull(label)) {
            throw new IllegalArgumentException(
                String.format("Invalid translation of instruction (%s) for mnemonic: %s and label %s",
                        StringUtil.bytesToHex(bytes), this.mnemonicMask, label)
            );  
        }   
        
        String mnemonic = this.mnemonicMask;
        
        // Format the mnemonic using the word parameter index (if available)
        if (this.hasWordParameter()) {
            
            // The word parameter must by translated using a little endian format
            if(label.isEmpty()) {
                String wordValue = StringUtil.wordToHexString(bytes[this.dataIndex], bytes[this.dataIndex+1]);
                mnemonic = String.format(this.mnemonicMask, wordValue);
            } else {
                mnemonic = String.format(this.mnemonicMask, label);
            }   
            
        } else {
            
            // Get the data parameter (if available).
            // NOTE: the dataIndex in general is greater than zero.
            // (exception for db xx, which is not an instruction)
            String data = (this.dataIndex >= 0) ? StringUtil.byteToHexString(bytes[this.dataIndex]):"";
            
            // Set the mnemonic according to the parameters available.
            // Note that the data string may be empty.
            if (this.displacementIndex < 0) {
                
                // For instructions without displacement, the mnemonic may contain only
                // the data parameter.
                mnemonic = String.format(this.mnemonicMask, data);
                
            } else {
                
                if(label.isEmpty()) {
                    
                    byte byteValue = bytes[this.displacementIndex];
                    mnemonic = String.format(this.mnemonicMask, Byte.toString(byteValue), data);
                    
                    // For byte values lower than 0, remove the plus sign from mnemonic
                    if(byteValue < 0) {
                        mnemonic = mnemonic.replace("+", "");
                    }   
                    
                } else {
                    mnemonic = String.format(this.mnemonicMask, label, data);
                }   
                
                // For extended instructions, the resulting output is a sequence of
                // bytes with a code commentary about the instruction that is being executed
                if(this.isUndocumented) {
                    mnemonic = mnemonic + " ; byte sequence: " + StringUtil.bytesToHex(bytes, ", ");
                }
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
     * Verify if the given String representation of a byte code matches this instruction. 
     * 
     * @param bytesString the string representation of a sequence of bytes (without any separators).
     * @return true if the given byte array is a valid instruction for this instruction.
     */
    public boolean matches(String bytesString) {
        return this.pattern.matcher(bytesString.toUpperCase()).matches();
    }   
    
    /**
     * Verify if this instruction is a data byte value.
     * @return true if this instruction is a data byte value.
     */
    public boolean isDbByte() {
        return this.equals(DB_BYTE);
    }   
    
    /**
     * Verify if this instruction is a parameter byte from an instruction.
     * @return true if this instruction is a parameter byte from an instruction
     */
    public boolean isParameter() {
        return this.equals(PARAMTER);
    }   
    
    @Override
    public int hashCode() {
        return this.mnemonicMask.hashCode();
    }   
    
    /**
     * NOTE: This equals implementation allows comparing a byte sequence or a string
     * with an instruction. This is very unusual, but is implemented this way here to
     * facilitate getting a matching instruction when running the disassembling process.
     * The other solution is using a wrapper that implements this method.
    */
    @Override
    public boolean equals(Object object) {
        
        if(object instanceof Instruction) {
            Instruction instruction = (Instruction) object;
            return this.byteMask.equals(instruction.getByteMask()) &&
                   this.mnemonicMask.equals(instruction.getMnemonicMask());
        }   
        
        return false;
    }   
    
    /**
     * Compare two op-codes based on the sequence of bytes.
     * 
     * @param other other OpCode object to compare.
     * @return 0, 1 or -1 (see the String comparator)
     */
    public int compareTo(Instruction other) {
        return this.byteMask.compareTo(other.getByteMask());
    }   
    
    @Override
    public String toString() {
        return this.mnemonicMask;
    }
}
