package ares.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import ares.core.CompiledProgram;

public class Assembler
{
	private static final HashMap<String, Integer> regNames = new HashMap<>();
	static
	{
		regNames.put("$zero", 0);
		regNames.put("$at", 1);
		regNames.put("$v0", 2);
		regNames.put("$v1", 3);
		regNames.put("$a0", 4);
		regNames.put("$a1", 5);
		regNames.put("$a2", 6);
		regNames.put("$a3", 7);
		regNames.put("$t0", 8);
		regNames.put("$t1", 9);
		regNames.put("$t2", 10);
		regNames.put("$t3", 11);
		regNames.put("$t4", 12);
		regNames.put("$t5", 13);
		regNames.put("$t6", 14);
		regNames.put("$t7", 15);
		regNames.put("$s0", 16);
		regNames.put("$s1", 17);
		regNames.put("$s2", 18);
		regNames.put("$s3", 19);
		regNames.put("$s4", 20);
		regNames.put("$s5", 21);
		regNames.put("$s6", 22);
		regNames.put("$s7", 23);
		regNames.put("$t8", 24);
		regNames.put("$t9", 25);
		regNames.put("$k0", 26);
		regNames.put("$k1", 27);
		regNames.put("$gp", 28);
		regNames.put("$sp", 29);
		regNames.put("$fp", 30);
		regNames.put("$ra", 31);
	}
	
	private static final HashMap<String, Integer[]> instructionSet = new HashMap<>();
	static
	{
		/*
		 * What the assembler does with each instruction can be
		 * completely described by three integers. These are as follows:
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
		instructionSet.put("add",	new Integer[]{0, 0, 32});
		instructionSet.put("addi",	new Integer[]{1, 8    });
		instructionSet.put("addiu",	new Integer[]{1, 9    });
		instructionSet.put("addu",	new Integer[]{0, 0, 33});
		instructionSet.put("and",	new Integer[]{0, 0, 36});
		instructionSet.put("andi",	new Integer[]{1, 12   });
		instructionSet.put("beq",	new Integer[]{1, 4    });
		instructionSet.put("bne",	new Integer[]{1, 5    });
		instructionSet.put("j", 	new Integer[]{2, 2    });
		instructionSet.put("jal",	new Integer[]{2, 3    });
		instructionSet.put("jr",	new Integer[]{0, 0,  8});
		instructionSet.put("lbu",	new Integer[]{1, 36   });
		instructionSet.put("lhu",	new Integer[]{1, 37   });
		instructionSet.put("ll",	new Integer[]{1, 48   });
		instructionSet.put("lui",	new Integer[]{1, 15   });
		instructionSet.put("lw",	new Integer[]{1, 35   });
		instructionSet.put("nor",	new Integer[]{0, 0, 39});
		instructionSet.put("or",	new Integer[]{0, 0, 37});
		instructionSet.put("ori",	new Integer[]{1, 13   });
		instructionSet.put("slt",	new Integer[]{0, 0, 42});
		instructionSet.put("slti",	new Integer[]{1, 10   });
		instructionSet.put("sltiu",	new Integer[]{1, 11   });
		instructionSet.put("sltu",	new Integer[]{0, 0, 43});
		instructionSet.put("sll",	new Integer[]{3, 0,  0});
		instructionSet.put("srl",	new Integer[]{3, 0,  2});
		instructionSet.put("sb",	new Integer[]{1, 40   });
		instructionSet.put("sc",	new Integer[]{1, 56   });
		instructionSet.put("sh",	new Integer[]{1, 41   });
		instructionSet.put("sw",	new Integer[]{1, 43   });
		instructionSet.put("sub",	new Integer[]{0, 0, 34});
		instructionSet.put("subu",	new Integer[]{0, 0, 35});
	}

	public static CompiledProgram assemble(String theProgram) throws AssemblerException
	{
		String[] splitProgram = theProgram.split("\n");
		for(String instruction : splitProgram)
		{
			/*
			 * Before parsing the instruction, get rid of whitespace, capital letters,
			 * and anything else that might get in our way.
			 * 
			 */
			String parsedInstruction = instruction.toLowerCase(); //Clean up around the edges.
			//The first space separates the instruction name from the operands, so replace it with ','.
			parsedInstruction = parsedInstruction.replaceAll("\t", " ").replaceFirst(" ", ",")
					.replaceAll("\\(", ",").replaceAll("\\)", "").replaceAll(" ", ""); 
			
			System.out.println(parsedInstruction);
			int result = 0;
			String[] operands = parsedInstruction.split(",");
			try
			{
				Integer[] directives = instructionSet.get(operands[0]);
				switch (directives[0])
				{
				case 0:
					result = assembleRFormat(directives[1].intValue(),	//opcode = directives[1] 
							getRegNum(operands[1]), 	//rs
							getRegNum(operands[2]), 	//rt
							getRegNum(operands[3]), 	//rd
							0, 							//shamt = 0 for non-shift R-type instructions
							directives[2].intValue());  //funct = directives[2]
					break;
				case 1:
					result = assembleIFormat(directives[1].intValue(),
							getRegNum(operands[1]),
							getRegNum(operands[2]),
							parseImmediate(operands[3])
							);
					break;
				case 2:
					result = assembleJFormat(directives[1].intValue(),
							parseImmediate(operands[1])
							);
					break;
				case 3:
					result = assembleRFormat(directives[1].intValue(),
							0,
							getRegNum(operands[2]),
							getRegNum(operands[1]),
							parseImmediate(operands[3]),
							directives[2].intValue()
							);
					break;
				}
			}
			catch (AssemblerException e)
			{
				throw new AssemblerException("Error assembling statement " + instruction + ": " + e.getMessage());
			}
			System.out.println("0x" + Integer.toHexString(result));
		}
		
		return new CompiledProgram();
	}
	
	public static String[] preprocess(String input)
	{
		Scanner strIterator = new Scanner(input);
		ArrayList<String> parsedProgram = new ArrayList<>();
		
		int lineNumber = 0;
		while (strIterator.hasNextLine())
		{
			lineNumber++;
			/*
			 * Before parsing the instruction, get rid of whitespace, capital letters,
			 * and anything else that might get in our way.
			 * 
			 */
			
			String theInstruction = strIterator.nextLine();
			
			theInstruction = theInstruction.toLowerCase(); //Clean up around the edges.
			//The first space separates the instruction name from the operands, so replace it with ','.
			theInstruction = theInstruction.trim().replaceAll("\t", " ").replaceFirst(" ", ",")
					.replaceAll("\\(", ",").replaceAll("\\)", "").replaceAll(" ", "");
			
			int isComment = theInstruction.indexOf('#');
			if (isComment == 0)
				continue; //the entire line was a comment
			else if (isComment > 0)
			{
				theInstruction = theInstruction.substring(0, isComment);
			}
			
			theInstruction = lineNumber + ":" + theInstruction;
			
			parsedProgram.add(theInstruction);
			
		}
		
		strIterator.close();
		return parsedProgram.toArray(new String[1]);
	}
	
	private static int assembleRFormat(int opcode, int rs, int rt, int rd, int shamt, int funct)
	{
		return ((opcode & 0b111111) << 26) + 
				((rs & 0b11111) << 21) + 
				((rt & 0b11111) << 16) + 
				((rd & 0b11111) << 11) + 
				((shamt & 0b11111) << 6) + 
				(funct & 0b111111);
	}
	
	private static int assembleIFormat(int opcode, int rs, int rt, int immediate)
	{
		return ((opcode & 0b111111) << 26) + 
				((rs & 0b11111) << 21) + 
				((rt & 0b11111) << 16) + 
				(immediate & 0xFFFF);
	}
	
	private static int assembleJFormat(int opcode, int address)
	{
		return ((opcode & 0b111111) << 26) + 
				( (address >> 2) & ((1 << 27) - 1)); 
		//(1 << 27) - 1 == 0b(26 ones)
	}
	
	private static int getRegNum(String name) throws AssemblerException
	{
		if (regNames.containsKey(name))
			return regNames.get(name);
		else
		{
			name = name.replaceAll("\\$", "");
			System.out.println(name);
			int litReg = Integer.parseInt(name);
			if (litReg > 0 && litReg < 32)
				return litReg;
			else
				throw new AssemblerException("Invalid register: " + name);
		}
	}
	
	private static int parseImmediate(String imm) throws AssemblerException
	{
		try
		{
			return Integer.parseInt(imm);
		}
		catch (Exception e)
		{
			throw new AssemblerException("Not an integer: " + imm);
		}
	}
	
}

class AssemblerException extends Exception
{
	public AssemblerException(String msg)
	{
		super(msg);
	}
}
