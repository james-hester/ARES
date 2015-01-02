package ares.core;

import java.util.Arrays;
//import java.util.BitSet;

public class Simulator
{
	private Memory memory; //The simulator's memory.
	
	boolean DEBUG = true; //When false, debugPrint() does nothing.
	
	boolean branchD = false;
	int branchAmtD;
	boolean hasNext = true;
	
	/*
	 * The pipeline registers and program counter.
	 */
	int PC;
	int[] IF_ID = new int[2];
	int[] ID_EX = new int[5];
	int[] EX_MEM = new int[4];
	int[] MEM_WB = new int[4];
	
	/*
	 * Pipeline registers for control bits.
	 * Currently unimplemented.
	 */
	//BitSet ID_EX_CTRL = new BitSet(9);
	//BitSet EX_MEM_CTRL = new BitSet(3);
	//BitSet MEM_WB_CTRL = new BitSet(2);
	
	public Simulator(Memory m)
	{
		memory = m;
		PC = Memory.TEXT_SEGMENT_START_ADDRESS;
		Arrays.fill(IF_ID, 0);
		Arrays.fill(ID_EX, 0);
		Arrays.fill(EX_MEM, 0);
		Arrays.fill(MEM_WB, 0);
	}
	
	
	/**
	 * Execute one clock cycle, simulating each of the five pipeline stages of the early MIPS processor.
	 * This method is broken into three parts:
	 * 		1. For each of the five pipeline stages, if there is any data in the pipeline register preceding it,
	 * 		   then complete the pipelined task and store the result in temporary variables.
	 * 		2. Anticipate and handle hazards.
	 * 		3. Store the values of the temporary variables into the pipeline registers.
	 * 
	 * Using temporary variables prevents instructions in various cycles from interfering with each other. 
	 * These variables have names which--for the most part--correspond to the labels on the wires 
	 * on figure 7.58 on page 427 of Digital Design and Computer Architecture (Harris et al., 2013). 
	 * Their noncompliance with Java naming conventions is intended to clearly distinguish them from variables
	 * used elsewhere, such as by the UI.
	 * 
	 */
	public void step()
	{
		MultiplyUnit.step();
		
		/*
		 * IF: Instruction fetch, phase 1.
		 * Fetch the instruction pointed to by the PC and increment the PC by four.
		 */
		
		int InstrF = memory.read(PC);
		int PCPlus4F = PC + 4;
		int NewPCF = PCPlus4F;
		
		/*
		 * ID: Instruction decode, phase 1.
		 * Parse the instruction.
		 */
		
		int InstrD = 0, PCPlus4D = 0, OpD = 0, SignImmD = 0, RsD = 0, RtD = 0;
		int RsNumD = 0, RtNumD = 0;
		if ( ! isEmpty(IF_ID))
		{
			InstrD = IF_ID[0];
			PCPlus4D = IF_ID[1];
			
			OpD = (InstrD >>> 26);
			RsNumD = (InstrD >> 21) & 0b11111;
			RtNumD = (InstrD >> 16) & 0b11111;
			SignImmD = ((short)(InstrD & 0x0000FFFF)); //Cast to char used to sign-extend the immediate.
		}
		
		/*
		 * EX: Execute, phase 1.
		 * Calculate address for load or store.
		 * Perform the requested ALU operation.
		 */
		
		int InstrE = 0, RsE = 0, RtE = 0, RdE = 0, SignImmE, OpE = 0, FunctE;
		int RsNumE = 0, RtNumE = 0, RdNumE = 0, PCPlus4E = 0;
		int AluOutE = 0, WriteDataE = 0, WriteRegE = 0;
		//"WriteControlE" contains write control logic.
		//The low bit is equivalent to RegWrite, the second equivalent to MemWrite,
		//and the third MemToReg.
		int WriteControlE = 0;
		boolean stallForMultiplierE = false;
		if ( ! isEmpty(ID_EX))
		{
			InstrE = ID_EX[0];
			RsE = ID_EX[1];
			RtE = ID_EX[2];
			SignImmE = ID_EX[3];
			PCPlus4E = ID_EX[4];

			OpE = (InstrE >>> 26);
			RsNumE = (InstrE >> 21 & 0b11111);
			RtNumE = (InstrE >> 16 & 0b11111);
			RdNumE = (InstrE >> 11 & 0b11111);
			FunctE = (InstrE & 0b111111);
			
			if (OpE == 0)
			{
				/*
				 * In nearly all cases, RegWrite will be asserted and the other two
				 * bits deasserted, and Rd is being written to.
				 */
				WriteControlE = 1;
				WriteRegE = RdNumE;
				
				switch (FunctE)
				{
				case 0x00: //sll
					AluOutE = RtE << (InstrE >> 6 & 0b11111);
					break;
				case 0x08: //jr
					WriteControlE = 0; //do nothing; jump has already been performed
					WriteRegE = 0;
					break;
				case 0x10: //mfhi
					stallForMultiplierE = MultiplyUnit.hasStepsRemaining();
					AluOutE = MultiplyUnit.moveFromHi();
					WriteControlE = stallForMultiplierE ? 0 : 1;
					break;
				case 0x12: //mflo
					stallForMultiplierE = MultiplyUnit.hasStepsRemaining();
					AluOutE = MultiplyUnit.moveFromLo();
					WriteControlE = stallForMultiplierE ? 0 : 1;
					break;					
				case 0x18: //mult
					MultiplyUnit.multiply(RsE, RtE);
					WriteControlE = 0;
					WriteRegE = 0;
					break;
				case 0x1a: //div
					MultiplyUnit.divide(RsE, RtE);
					WriteControlE = 0;
					WriteRegE = 0;
					break;
				case 0x20: //add
					AluOutE = RsE + RtE;
					break;
				case 0x25: //or
					AluOutE = RsE | RtE;
					break;
				case 0x24: //and
					AluOutE = RsE & RtE;
					break;
				case 0x22: //sub
					AluOutE = RsE - RtE;
					break;
				}
			}
			else
			{
				switch (OpE)
				{
				case 0x03: //jal
					AluOutE = PCPlus4E + 4;
					RtNumE = 31;
					WriteControlE = 1; //001
					break;
				case 0x02: //j
				case 0x04: //beq
				case 0x05: //bne
					//Branches and jumps are processed in the ID stage.
					//So, don't do anything.
					WriteControlE = 0;
					break;
				case 0x0d: //ori
					AluOutE = (RsE | (SignImmE & 0x0000FFFF)); //zero-extended immediate used for ori
					WriteControlE = 1; //001
					break;
				case 0x08: //addi
					AluOutE = (RsE + SignImmE);
					WriteControlE = 1;
					break;
				case 0x0f: //lui
					AluOutE = ((SignImmE & 0x0000FFFF) << 16);
					WriteControlE = 1; //001
					break;
				case 0x23: //lw
					AluOutE = (RsE + SignImmE);
					WriteControlE = 5; //101
					break;
				case 0x2b: //sw
					AluOutE = (RsE + SignImmE);
					WriteDataE = RtE;
					WriteControlE = 2; //010
					break;
				}
				WriteRegE = RtNumE;
			}
			
		}
		
		
		/*
		 * MEM: Memory read/write, phase 1.
		 */
		int AluOutM = 0, WriteDataM = 0, WriteRegM = 0, WriteControlM = 0, ReadDataM = 0;
		if ( ! isEmpty(EX_MEM))
		{
			AluOutM = EX_MEM[0];
			WriteControlM = EX_MEM[1];
			WriteRegM = EX_MEM[2];
			WriteDataM = EX_MEM[3];
			
			
			if ((WriteControlM & 2) != 0) //if MemWriteM is set
			{
				memory.write(AluOutM, WriteDataM);
				debugPrint("MEM: memory write at address " + AluOutM + " : " + WriteDataM);
			}
			else
			{
				ReadDataM = memory.read(AluOutM);
				debugPrint("MEM: memory read at address " + AluOutM + " : " + ReadDataM);
			}
			
		}
		
		
		/*
		 * WB: Write back, phase 1.
		 * Write to register file.
		 */
		
		int WriteRegW = 0, ReadDataW = 0, AluOutW = 0, WriteControlW = 0, ResultW = 0;
		if ( ! isEmpty(MEM_WB))
		{
			AluOutW = MEM_WB[0];
			WriteControlW = MEM_WB[1];
			WriteRegW = MEM_WB[2];
			ReadDataW = MEM_WB[3];
			
			if((WriteControlW & 4) != 0) // if (MemtoReg)
			{
				ResultW = ReadDataW;
			}
			else
			{
				ResultW = AluOutW;
			}
			
			if ((WriteControlW & 1) != 0) //if (RegWrite)
			{
				memory.writeRegister(WriteRegW, ResultW);
			}

		
		}		
		
		/*
		 * ID, phase 2.
		 * Read from registers and perform branches/jumps.
		 */
		if ( ! isEmpty(IF_ID))
		{
			/*
			 * Read-after-write hazard detector.
			 * Note: forwarding control signals are not simulated.
			 * Rather, the values that will be written into the pipeline registers
			 * and used during the NEXT cycle are modified directly.
			 * The code is adapted from page 418 of Digital Design and 
			 * Computer Architecture (Harris et al., 2013).
			 */
			
			//Forward to SrcAE
			
			if ((RsNumD != 0) && (RsNumD == WriteRegE) && ((WriteControlE & 1) != 0))
				RsD = AluOutE;
			
			else if ((RsNumD != 0) && (RsNumD == WriteRegM) && ((WriteControlM & 1) != 0))
				RsD = ((WriteControlM & 4) != 0) ? ReadDataM : AluOutM;
			
			else
				RsD = memory.readRegister(RsNumD);
			
			//Forward to SrcBE (identical, except Rt is used instead of Rs)
			
			if ((RtNumD != 0) && (RtNumD == WriteRegE) && ((WriteControlE & 1) != 0))
				RtD = AluOutE;
			
			else if ((RtNumD != 0) && (RtNumD == WriteRegM) && ((WriteControlM & 1) != 0))
				RtD = ((WriteControlM & 4) != 0) ? ReadDataM : AluOutM;
			
			else
				RtD = memory.readRegister(RtNumD);

			if (OpD == 0x04 || OpD == 0x05)
			{
				/*
				 * These two dense lines forward the correct values to the ID comparator. This is
				 * different from how forwarding to the EX stage is implemented (in its own dedicated
				 * "phase", using the pipeline registers), because these forwarded values are not used
				 * by non-branch instructions. TODO: perhaps this could be done more elegantly in the future.
				 */
				int compRsD = ((RsNumD != 0) && (RsNumD == WriteRegM) && ((WriteControlM & 1) != 0)) ? AluOutM : RsD;
				int compRtD = ((RtNumD != 0) && (RtNumD == WriteRegM) && ((WriteControlM & 1) != 0)) ? AluOutM : RtD;
				
				boolean EqualD = (compRsD == compRtD);
				
				switch (OpD)
				{
				case 0x04:
					if (EqualD)
						NewPCF = PCPlus4D + (SignImmD * 4);
					break;
				case 0x05:
					if (!EqualD)
						NewPCF = PCPlus4D + (SignImmD * 4);
					break;
				}
			}
			else if (OpD == 0x02 || OpD == 0x03)
			{
				NewPCF = (PCPlus4D & 0xF0000000) + ((InstrD & 0x07FFFFFF) << 2);
			}
			else if (OpD == 0x00 && (InstrD & 0b111111) == 0x08) //jr
			{
				NewPCF = RsD;
			}
		}
		

		
		/*
		 * Stall inserter for lw data hazard.
		 */
		boolean StallF = false, StallD = false, FlushE = false, stall = false;
		if (OpE == 0x23 && (RtNumE == RsNumD || RtNumE == RtNumD))
		{
			stall = true;
		}
		/*
		 * Stall inserter for branch data hazard.
		 */
		if ((OpD == 0x04 || OpD == 0x05) && //equivalent to BranchD
				((((WriteControlE & 1) != 0) && (WriteRegE == RsNumD || WriteRegE == RtNumD)) || 
				(((WriteControlM & 4) != 0) && (WriteRegM == RsNumD || WriteRegM == RtNumD))))
		{
			stall = true;
		}
		
		if (stallForMultiplierE)
		{
			stall = true;
		}
		StallF = StallD = FlushE = stall;
		
		/*-------------------------------------------------------------------*
		 * Finally, write the temporary variables to the pipeline registers. *
		 *-------------------------------------------------------------------*/
		
		if ( ! StallF)
		{
		PC = NewPCF;
		}
		
		if ( ! StallD)
		{
		IF_ID[0] = InstrF;
		IF_ID[1] = (InstrF > 0) ? PCPlus4F : 0;
		}

		if ( ! FlushE)
		{
		ID_EX[0] = InstrD;
		ID_EX[1] = RsD;
		ID_EX[2] = RtD;
		ID_EX[3] = SignImmD;
		ID_EX[4] = PCPlus4D;
		}
		else if ( ! stallForMultiplierE)
		{
		Arrays.fill(ID_EX, 0);
		}
		
		EX_MEM[0] = AluOutE;
		EX_MEM[1] = WriteControlE;
		EX_MEM[2] = WriteRegE;
		EX_MEM[3] = WriteDataE;
		
		MEM_WB[0] = AluOutM;
		MEM_WB[1] = WriteControlM;
		MEM_WB[2] = WriteRegM;
		MEM_WB[3] = ReadDataM;


		StallF = StallD = FlushE = false;
		
		if (PC > (memory.getMaxInstAddr() + (4 * 3)) && 
		(isEmpty(ID_EX) && isEmpty(EX_MEM) && isEmpty(MEM_WB)))
		{
			hasNext = false;
		}
		
		if (DEBUG)
		{
			debugPrint("---------------------------");
			for(int i = 0; i < 10; i++)
			{
				debugPrint(i + "\t" + memory.readRegister(i));
			}
			debugPrint("---------------------------");
			
			debugPrint("-------------------------------------------");
			debugPrint("PC"+"\t\t"+"IF/ID"+"\t\t"+"ID/EX"+"\t\t"+"EX/MEM"+"\t\t"+"MEM/WB");
			for(int i = 0; i < 4; i++)
			{
				String strA = Integer.toString(IF_ID[i/2]).length() > 7 ? "\t" : "\t\t";
				String strB = Integer.toString(ID_EX[i]).length() > 7 ? "\t" : "\t\t";
				String strC = Integer.toString(EX_MEM[i]).length() > 7 ? "\t" : "\t\t";
				String strD = Integer.toString(PC).length() > 7 ? "\t" : "\t\t";
				debugPrint(PC+strD+IF_ID[i/2]+strA+ID_EX[i]+strB+EX_MEM[i]+strC+MEM_WB[i]);
			}
		}
		
	}
	
	private static class MultiplyUnit
	{
		private static long product;
		private static int rs, rt, stepsRemaining = -1;
		private static enum Operation{MULTIPLY, DIVIDE};
		private static Operation currentOperation;
		
		public static void multiply(int _rs, int _rt)
		{
			rs = _rs;
			rt = _rt;
			stepsRemaining = 12;
			currentOperation = Operation.MULTIPLY;
		}
		
		public static void divide(int _rs, int _rt)
		{
			rs = _rs;
			rt = _rt;
			stepsRemaining = 35;
			currentOperation = Operation.DIVIDE;
		}
		
		public static void step()
		{
			if (stepsRemaining > 0)
				stepsRemaining--;
			if (stepsRemaining == 0)
			{
				switch (currentOperation)
				{
				case DIVIDE:
					product =  0x00000000FFFFFFFFL & (rs / rt);
					product += 0xFFFFFFFF00000000L & ((long)(rs % rt) << 32);
					break;
				case MULTIPLY:
					product = (long) rs * (long) rt;
					break;
				default:
					break;
				}
				stepsRemaining--;
			}
		}
		
		public static boolean hasStepsRemaining()
		{
			return (stepsRemaining > 0);
		}
		
		public static int moveFromHi()
		{
			return (int)((product & 0xFFFFFFFF00000000L) >> 32);
		}
		
		public static int moveFromLo()
		{
			return (int)((product & 0x00000000FFFFFFFFL));
		}
	}
	
	public boolean hasNextInstruction()
	{
		return hasNext;
	}
	
	private boolean isEmpty(int[] register)
	{
		for(int i = 0; i < register.length; i++)
		{
			if (register[i] != 0)
				return false;
		}
		return true;
	}
	
	private void debugPrint(Object msg)
	{
		if (DEBUG)
		{
			System.out.println(msg);
		}
	}
}
