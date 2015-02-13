package ares.core;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;

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
	
	/**
	 * Pipeline registers for control bits.
	 */
	BitSet IF_ID_CTRL =  new BitSet(1);
	BitSet ID_EX_CTRL =  new BitSet(1);
	BitSet EX_MEM_CTRL = new BitSet(5);
	BitSet MEM_WB_CTRL = new BitSet(3);
	
	/**
	 * Holds the exception that will be handled on the next clock cycle.
	 * A real R-series MIPS uses an exception vector to ensure that, if two
	 * exceptions occur in the same clock cycle, the one handled is the
	 * one which occurs in an earlier pipeline stage.
	 * The fact that ARES simulates each stage sequentially can be used to
	 * substantially simplify this model: if an exception is "thrown,"
	 * and there is already an exception stored in this variable,
	 * it is ignored.
	 */
	MIPSException currentException = null;
	
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
		boolean InBranchDelayF = false;
		
		/*
		 * ID: Instruction decode, phase 1.
		 * Parse the instruction.
		 */
		
		int InstrD = 0, PCPlus4D = 0, OpD = 0, SignImmD = 0, RsD = 0, RtD = 0;
		int RsNumD = 0, RtNumD = 0;
		boolean BranchD = false, InBranchDelayD;
		if ( ! isEmpty(IF_ID))
		{
			InstrD = IF_ID[0];
			PCPlus4D = IF_ID[1];
			
			InBranchDelayD = IF_ID_CTRL.get(0);
			
			OpD = (InstrD >>> 26);
			RsNumD = (InstrD >> 21) & 0b11111;
			RtNumD = (InstrD >> 16) & 0b11111;
			SignImmD = ((short)(InstrD & 0x0000FFFF)); //Cast to short used to sign-extend the immediate.
			
			BranchD = (OpD == 0x04 || OpD == 0x05);
		}
		
		/*
		 * EX: Execute, phase 1.
		 * Calculate address for load or store.
		 * Perform the requested ALU operation.
		 */
		
		int InstrE = 0, RsE = 0, RtE = 0, RdE = 0, SignImmE, OpE = 0, FunctE;
		int RsNumE = 0, RtNumE = 0, RdNumE = 0, PCPlus4E = 0;
		int AluOutE = 0, WriteDataE = 0, WriteRegE = 0;
		boolean RegWriteE = false, MemWriteE = false, MemToRegE = false, MemByteE = false, MemHalfwordE = false;
		boolean stallForMultiplierE = false;
		boolean InBranchDelayE = false;
		if ( ! isEmpty(ID_EX))
		{
			InstrE = ID_EX[0];
			RsE = ID_EX[1];
			RtE = ID_EX[2];
			SignImmE = ID_EX[3];
			PCPlus4E = ID_EX[4];
			
			InBranchDelayE = ID_EX_CTRL.get(0);

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
				RegWriteE = true;
				WriteRegE = RdNumE;
				
				switch (FunctE)
				{
				case 0x00: //sll
					AluOutE = RtE << (InstrE >> 6 & 0b11111);
					break;
				case 0x08: //jr
					//do nothing; jump has already been performed
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x10: //mfhi
					stallForMultiplierE = MultiplyUnit.hasStepsRemaining();
					AluOutE = MultiplyUnit.moveFromHi();
					//WriteControlE = stallForMultiplierE ? 0 : 1;
					RegWriteE = true;
					break;
				case 0x12: //mflo
					stallForMultiplierE = MultiplyUnit.hasStepsRemaining();
					AluOutE = MultiplyUnit.moveFromLo();
					//WriteControlE = stallForMultiplierE ? 0 : 1;
					RegWriteE = true;
					break;					
				case 0x18: //mult
					MultiplyUnit.multiply(RsE, RtE);
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x1a: //div
					MultiplyUnit.divide(RsE, RtE);
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x20: //add
					AluOutE = RsE + RtE;
					if ( (int)( (long)RsE + (long)RtE ) != AluOutE) //Check for overflow.
						exceptionVector.addLast(new MIPSException(PC - 8, MIPSException.OVERFLOW));
					break;
				case 0x25: //or
					AluOutE = RsE | RtE;
					break;
				case 0x24: //and
					AluOutE = RsE & RtE;
					break;
				case 0x22: //sub
					AluOutE = RsE - RtE;
					if ( (int)( (long)RsE - (long)RtE ) != AluOutE) //Check for overflow.
						exceptionVector.addLast(new MIPSException(PC - 8, MIPSException.OVERFLOW));
					break;
				case 0x21: //addu
					AluOutE = RsE + RtE;
					break;
				case 0x23: //subu
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
					RegWriteE = true;
					break;
				case 0x02: //j
				case 0x04: //beq
				case 0x05: //bne
					//Branches and jumps are processed in the ID stage.
					//So, don't do anything.
					break;
				case 0x0d: //ori
					AluOutE = (RsE | (SignImmE & 0x0000FFFF)); //zero-extended immediate used for ori
					RegWriteE = true;
					break;
				case 0x08: //addi
					AluOutE = (RsE + (SignImmE & 0x0000FFFF)); //zero-extended immediate used for andi
					RegWriteE = true;
					break;
				case 0x0c: //andi
					AluOutE = (RsE & SignImmE);
					RegWriteE = true;
					break;
				case 0x09: //addiu
					AluOutE = (RsE + SignImmE);
					RegWriteE = true;
					break;
				case 0x0f: //lui
					AluOutE = ((SignImmE & 0x0000FFFF) << 16);
					RegWriteE = true;
					break;
				case 0x24: //lbu
					AluOutE = (RsE + SignImmE);
					RegWriteE = MemToRegE = true;
					MemByteE = true;
					break;
				case 0x25: //lhu
					AluOutE = (RsE + SignImmE);
					RegWriteE = MemToRegE = true;
					MemHalfwordE = true;
					break;
				case 0x30: //ll
					//Not implemented
					break;
				case 0x0a: //slti
					if (RsE < SignImmE)
						AluOutE = 1;
					else
						AluOutE = 0;
					RegWriteE = true;
					break;
				case 0x0b: //sltiu
					if (Integer.compareUnsigned(RsE, SignImmE) < 0)
						AluOutE = 1;
					else
						AluOutE = 0;
					RegWriteE = true;
					break;
				case 0x23: //lw
					AluOutE = (RsE + SignImmE);
					RegWriteE = MemToRegE = true;
					break;
				case 0x2b: //sw
					AluOutE = (RsE + SignImmE);
					WriteDataE = RtE;
					MemWriteE = true;
					break;
				}
				WriteRegE = RtNumE;
			}
			
		}
		
		/*
		 * MEM: Memory read/write, phase 1.
		 */
		int AluOutM = 0, WriteDataM = 0, WriteRegM = 0, ReadDataM = 0;
		boolean MemToRegM = false, MemWriteM = false, RegWriteM = false, MemByteM = false, MemHalfwordM = false;
		if ( ! isEmpty(EX_MEM))
		{
			AluOutM = EX_MEM[0];
			WriteRegM = EX_MEM[2];
			WriteDataM = EX_MEM[3];
			
			RegWriteM = EX_MEM_CTRL.get(0);
			MemWriteM = EX_MEM_CTRL.get(1);
			MemToRegM = EX_MEM_CTRL.get(2);
			MemByteM = EX_MEM_CTRL.get(3);
			MemHalfwordM = EX_MEM_CTRL.get(4);
			
			if (MemWriteM) //if MemWriteM is set, write
			{
				if (MemByteM)
					memory.writeByte(AluOutM, WriteDataM);
				else if (MemHalfwordM)
					memory.writeHalfword(AluOutM, WriteDataM);
				else
					memory.write(AluOutM, WriteDataM);
				
				debugPrint("MEM: memory write at address " + AluOutM + " : " + WriteDataM);
			}
			else if (MemToRegM) //if MemToRegM is set, read
			{
				//TODO: implement these for real
				if (MemByteM)
					ReadDataM = memory.read(AluOutM);
				else if (MemHalfwordM)
					ReadDataM = memory.read(AluOutM);
				else
					ReadDataM = memory.read(AluOutM);
				
				debugPrint("MEM: memory read at address " + AluOutM + " : " + ReadDataM);
			}
			
		}
		
		
		/*
		 * WB: Write back, phase 1.
		 * Write to register file.
		 */
		
		int WriteRegW = 0, ReadDataW = 0, AluOutW = 0, ResultW = 0;
		boolean RegWriteW = false, MemWriteW = false, MemToRegW = false;
		if ( ! isEmpty(MEM_WB))
		{
			AluOutW =   MEM_WB[0];
			WriteRegW = MEM_WB[2];
			ReadDataW = MEM_WB[3];
			
			RegWriteW = MEM_WB_CTRL.get(0);
			MemWriteW = MEM_WB_CTRL.get(1);
			MemToRegW = MEM_WB_CTRL.get(2);
			
			if(MemToRegW)
			{
				ResultW = ReadDataW;
			}
			else
			{
				ResultW = AluOutW;
			}
			
			if (RegWriteW)
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
			
			if ((RsNumD != 0) && (RsNumD == WriteRegE) && RegWriteE)
				RsD = AluOutE;
			
			else if ((RsNumD != 0) && (RsNumD == WriteRegM) && RegWriteM)
				RsD = MemToRegM ? ReadDataM : AluOutM;
			
			else
				RsD = memory.readRegister(RsNumD);
			
			//Forward to SrcBE (identical, except Rt is used instead of Rs)
			
			if ((RtNumD != 0) && (RtNumD == WriteRegE) && RegWriteE)
				RtD = AluOutE;
			
			else if ((RtNumD != 0) && (RtNumD == WriteRegM) && RegWriteM)
				RtD = MemToRegM ? ReadDataM : AluOutM;
			
			else
				RtD = memory.readRegister(RtNumD);

			if (BranchD)
			{
				/*
				 * These two dense lines forward the correct values to the ID comparator. 
				 */
				int compRsD = ( (RsNumD != 0) && (RsNumD == WriteRegM) && RegWriteM) ? AluOutM : RsD;
				int compRtD = ( (RtNumD != 0) && (RtNumD == WriteRegM) && RegWriteM) ? AluOutM : RtD;
				
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
			else if (OpD == 0x02 || OpD == 0x03) //jumps
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
		if (BranchD && //equivalent to BranchD
				((( RegWriteE && (WriteRegE == RsNumD || WriteRegE == RtNumD) ) || 
				(( (MemToRegM) && (WriteRegM == RsNumD || WriteRegM == RtNumD) )))
				))
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
		
		IF_ID_CTRL.set(0, InBranchDelayF);
		}

		if ( ! FlushE)
		{
		ID_EX[0] = InstrD;
		ID_EX[1] = RsD;
		ID_EX[2] = RtD;
		ID_EX[3] = SignImmD;
		ID_EX[4] = PCPlus4D;
		
		ID_EX_CTRL.set(0, InBranchDelayD);
		}
		else if ( ! stallForMultiplierE)
		{
		Arrays.fill(ID_EX, 0);
		ID_EX_CTRL.clear();
		}
		
		EX_MEM[0] = AluOutE;
		EX_MEM[2] = WriteRegE;
		EX_MEM[3] = WriteDataE;
		
		EX_MEM_CTRL.set(0, RegWriteE);
		EX_MEM_CTRL.set(1, MemWriteE);
		EX_MEM_CTRL.set(2, MemToRegE);
		EX_MEM_CTRL.set(3, MemByteE);
		EX_MEM_CTRL.set(4, MemHalfwordE);
		EX_MEM_CTRL.set(5, InBranchDelayE);
		
		MEM_WB[0] = AluOutM;
		MEM_WB[2] = WriteRegM;
		MEM_WB[3] = ReadDataM;
		
		MEM_WB_CTRL.set(0, RegWriteM);
		MEM_WB_CTRL.set(1, MemWriteM);
		MEM_WB_CTRL.set(2, MemToRegM);


		StallF = StallD = FlushE = false;
		
		/*
		 * Check to see whether we've dropped off the bottom of the executing program.
		 */
		
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
			for(int i = 0; i < 5; i++)
			{
				String[] pipelineVars = new String[5];
				pipelineVars[0] = (i == 0 ? Integer.toString(PC) : "");
				pipelineVars[1] = (i < IF_ID.length ? Integer.toString(IF_ID[i]) : "");
				pipelineVars[2] = (i < ID_EX.length ? Integer.toString(ID_EX[i]) : "");
				pipelineVars[3] = (i < EX_MEM.length ? Integer.toString(EX_MEM[i]) : ""); 
				pipelineVars[4] = (i < MEM_WB.length ? Integer.toString(MEM_WB[i]) : "");
				String output = "";
				for(int j = 0; j < 5; j++)
					output += pipelineVars[j] + (pipelineVars[j].length() > 7 ? "\t" : "\t\t");
				debugPrint(output);
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
