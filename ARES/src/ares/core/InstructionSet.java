package ares.core;
import java.util.HashMap;


public final class InstructionSet 
{
	private static final HashMap<String, int[]> instructionSet = new HashMap<>();
	static
	{
		/*
		 * What the assembler does with each instruction can be
		 * completely described by three ints. These are as follows:
		 * 	int[0]: the operand interpretation mode:
		 * 		0: R-format (normal, such as add $t1, $t2, $t3)
		 * 		1: I-format (normal, such as addi $t1, $t2, 100)
		 * 		2: J-format (normal, such as j 4000)
		 * 		3: R-format w. shift (must be treated differently because shift instructions
		 * 			take 2 register and 1 "immediate" argument, the amount)
		 * 		4: I-format w. base, such as lw or sw
		 * 	int[1]: the opcode
		 * 	int[2]: if R-format, the function (funct)
		 */
		instructionSet.put("add",	new int[]{0, 0x00, 0x20});
		instructionSet.put("addi",	new int[]{1, 0x08      });
		instructionSet.put("addiu",	new int[]{1, 0x09      });
		instructionSet.put("addu",	new int[]{0, 0x00, 0x21});
		instructionSet.put("and",	new int[]{0, 0x00, 0x24});
		instructionSet.put("andi",	new int[]{1, 12   });
		instructionSet.put("beq",	new int[]{1, 4    });
		instructionSet.put("bne",	new int[]{1, 5    });
		instructionSet.put("j", 	new int[]{2, 2    });
		instructionSet.put("jal",	new int[]{2, 3    });
		instructionSet.put("jr",	new int[]{0, 0x00,  0x08});
		instructionSet.put("lbu",	new int[]{1, 36   });
		instructionSet.put("lhu",	new int[]{1, 37   });
		instructionSet.put("ll",	new int[]{1, 48   });
		instructionSet.put("lui",	new int[]{1, 15   });
		instructionSet.put("lw",	new int[]{1, 35   });
		instructionSet.put("nor",	new int[]{0, 0x00, 0x27});
		instructionSet.put("or",	new int[]{0, 0x00, 0x25});
		instructionSet.put("ori",	new int[]{1, 0x0d      });
		instructionSet.put("slt",	new int[]{0, 0x00, 0x2a});
		instructionSet.put("slti",	new int[]{1, 0x0a      });
		instructionSet.put("sltiu",	new int[]{1, 0x0b      });
		instructionSet.put("sltu",	new int[]{0, 0x00, 0x2b});
		instructionSet.put("sll",	new int[]{3, 0x00, 0x00});
		instructionSet.put("srl",	new int[]{3, 0x00, 0x02});
		instructionSet.put("sb",	new int[]{1, 0x28      });
		instructionSet.put("sc",	new int[]{1, 0x38      });
		instructionSet.put("sh",	new int[]{1, 0x29      });
		instructionSet.put("sw",	new int[]{1, 0x2b      });
		instructionSet.put("sub",	new int[]{0, 0x00, 0x22});
		instructionSet.put("subu",	new int[]{0, 0x00, 0x23});
		
		instructionSet.put("mfhi",	new int[]{0, 0x00, 0x10      });
		instructionSet.put("mthi",	new int[]{0, 0x00, 0x11      });
		instructionSet.put("mflo",	new int[]{0, 0x00, 0x12      });
		instructionSet.put("mtlo",	new int[]{0, 0x00, 0x13      });
		instructionSet.put("mult",	new int[]{0, 0x00, 0x18      });
		instructionSet.put("multu",	new int[]{0, 0x00, 0x19      });
		instructionSet.put("div",	new int[]{0, 0x00, 0x1a      });
		instructionSet.put("divu",	new int[]{0, 0x00, 0x1b      });
	}
	
	private static final HashMap<Integer, String> rFormatInstructions = new HashMap<>();
	static
	{
		for(String s : instructionSet.keySet())
		{
			int[] i = instructionSet.get(s);
			if(i.length > 2 && i[1] == 0)
				rFormatInstructions.put(i[2], s);
		}
	}
	
	private static final HashMap<Integer, String> iFormatInstructions = new HashMap<>();
	static
	{
		for(String s : instructionSet.keySet())
		{
			int[] i = instructionSet.get(s);
			if(i[1] != 0)
				iFormatInstructions.put(i[1], s);
		}
	}
	
	public static boolean contains(String mnemonic)
	{
		return instructionSet.containsKey(mnemonic);
	}
	
	public static int[] getAssemblerDirectives(String mnemonic)
	{
		return instructionSet.get(mnemonic);
	}
	
	public static int getOpcode(String mnemonic)
	{
		int[] temp = instructionSet.get(mnemonic);
		if (temp == null)
			return -1;
		return temp[1];
	}
	
	public static int getFunct(String mnemonic)
	{
		int[] temp = instructionSet.get(mnemonic);
		if (temp == null || temp.length < 3)
			return -1;
		return temp[2];
	}
	
	/**
	 * "Disassembles" an instruction into a mnemonic. Parses 0 as "nop" and instructions not in the instruction set as "???".
	 * @param instruction
	 * @return
	 */
	public static String getMnemonic(int instruction)
	{
		if (instruction == 0)
			return "nop";
		int opcode = (instruction >>> 26);
		String result;
		
		if (opcode == 0)
			result = rFormatInstructions.get(instruction & 0b111111);
		else
			result = iFormatInstructions.get(opcode);

		return (result == null) ? "???" : result;
	}
}
