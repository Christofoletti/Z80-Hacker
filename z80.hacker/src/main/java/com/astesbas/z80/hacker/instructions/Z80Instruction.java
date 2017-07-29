package com.astesbas.z80.hacker.instructions;

/**
 * The z80 instruction base class.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @since 02/jun/2017
 */
public abstract class Z80Instruction {
	
	/**
	 * 
	 * @return
	 */
	public abstract String getMnemonic();
	
	/**
	 * 
	 * @return
	 */
	public abstract byte[] getBytes();
	
	/**
	 * 
	 * @return
	 */
	public abstract int getSize();
	
	@Override
	public String toString() {
		return this.getMnemonic();
	}	
}
