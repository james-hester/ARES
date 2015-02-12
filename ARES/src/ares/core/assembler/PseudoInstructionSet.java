package ares.core.assembler;
import java.util.HashMap;

import ares.core.InstructionSet;


public final class PseudoInstructionSet 
{
	private static final HashMap<String, String[]> pseudoInstructionSet = new HashMap<>();
	static
	{
		pseudoInstructionSet.put("[li, reg, imm32]", new String[]{"lui", "%1,", "%2[HI]", "ori", "%1,", "%2[LO]"});
		pseudoInstructionSet.put("[li, reg, imm16]", new String[]{"ori", "%1,", "%2"});
	}
		
	public static boolean contains(String mnemonic)
	{
		for(String pseudoInstructionHeader : pseudoInstructionSet.keySet())
		{
			String m = "[" + mnemonic + ",";
			if (pseudoInstructionHeader.contains(m))
				return true;
		}
		return false;
	}
	
	/**
	 * Gives number of real instructions inside of a pseudoinstruction.
	 * @param mnemonic the pseudoinstruction
	 * @return number of real MIPS instructions
	 */
	public static int getInstructionLength(String header)
	{
		if ( ! pseudoInstructionSet.containsKey(header))
			return -1;
		String[] mac = pseudoInstructionSet.get(header);
		if (mac == null)
			return -1;
		
		int result = 0;
		for(String inst : mac)
		{
			if (InstructionSet.contains(inst))
				result++;
		}

		return result;
	}
}
