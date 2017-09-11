/**
 * 
 */
package com.astesbas.z80.hacker.domain;

import java.util.Optional;

/**
 * This class represents a decoded instruction generated in the disassembling process. 
 * 
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
 * @since 09/aug/2017
 */
public class Instruction {
    
    private final OpCode opcode;
    
    private Optional<String> label;
    
    /**
     * Defines an instruction. The string parameter defines a label to be used in the translation. 
     * @param opcode
     * @param label
     */
    public Instruction(OpCode opcode, String label) {
        this.opcode = opcode;
        this.label = Optional.ofNullable(label);
    }
    
    /**
     * 
     * @param bytes
     * @return
     */
    public String translate(byte[] bytes) {
        if(this.label.isPresent()) {
            return this.opcode.translate(bytes, this.label.get());
        } else {
            return this.opcode.translate(bytes);
        }
    }
    
    /**
     * 
     * @return
     */
    public int getSize() {
        return this.opcode.getSize();
    }
}
