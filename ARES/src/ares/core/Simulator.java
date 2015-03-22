package ares.core;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;

public class Simulator
{
	private Memory memory; //The simulator's memory.
	
	private static final boolean DEBUG = true; //When false, debugPrint() does nothing.
	
	boolean branchD = false;
	int branchAmtD;
	boolean hasNext = true;
	
	/**
	 * The pipeline registers and program counter.
	 */
	private int PC;
	private int[] IF_ID = new int[2];
	private int[] ID_EX = new int[5];
	private int[] EX_MEM = new int[4];
	private int[] MEM_WB = new int[4];
	
	/**
	 * Pipeline registers for control bits.
	 */
	private BitSet IF_ID_CTRL =  new BitSet(1);
	private BitSet ID_EX_CTRL =  new BitSet(1);
	private BitSet EX_MEM_CTRL = new BitSet(7);
	private BitSet MEM_WB_CTRL = new BitSet(3);
	
	/**
	 * After step() is called, each bit will represent whether
	 * anything happened in the corresponding stage. Used only
	 * by functions called by UI code, as a convenience.
	 */
	private BitSet stageOccurred = new BitSet(5);
	
	/**
	 * Holds the exception that will be handled on the next clock cycle.
	 * A real R-series uses an exception vector to ensure that, if two
	 * exceptions occur in the same clock cycle, the one handled is the
	 * one which occurs in an earlier pipeline stage.
	 * The fact that ARES simulates each stage sequentially can be used to
	 * substantially simplify this model: if an exception is "thrown,"
	 * and there is already an exception stored in this variable,
	 * it is ignored.
	 */
	MIPSException currentException = null;
	
	Coprocessor0 cp0 = new Coprocessor0();
	

	
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
	 * Gets the name (the mnemonic) of the instruction which was just fetched in the last IF stage. 
	 * @return a mnemonic, as a String
	 */
	public String getInstructionFetchedName()
	{
		//TODO: write a function which will extract the instruction name from this
		//maybe one of the hash tables from 2pass will help?
		return Integer.toHexString(IF_ID[0]);
	}
	
	public char getExecuteOperationName()
	{
		return '+';
	}
	
	public BitSet getStagesOccurred()
	{
		return stageOccurred;
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
		stageOccurred.clear();
		
		/*
		 * IF: Instruction fetch, phase 1.
		 * Fetch the instruction pointed to by the PC and increment the PC by four.
		 */
		
		stageOccurred.set(0);
		int InstrF = memory.loadWord(PC);
		int PCPlus4F = PC + 4;
		int NewPCF = PCPlus4F;
		boolean InBranchDelayF = false;
		
		/*
		 * ID: Instruction decode, phase 1.
		 * Parse the instruction.
		 */
		
		int InstrD = 0, PCPlus4D = 0, OpD = 0, SignImmD = 0, RsD = 0, RtD = 0;
		int RsNumD = 0, RtNumD = 0;
		boolean BranchD = false, InBranchDelayD = false;
		if ( ! isEmpty(IF_ID))
		{
			stageOccurred.set(1);
			InstrD = IF_ID[0];
			PCPlus4D = IF_ID[1];
			
			InBranchDelayD = IF_ID_CTRL.get(0);
			
			OpD = (InstrD >>> 26);
			RsNumD = (InstrD >> 21) & 0b11111;
			RtNumD = (InstrD >> 16) & 0b11111;
			SignImmD = ((short)(InstrD & 0x0000FFFF)); //Cast to short used to sign-extend the immediate.
			
			BranchD = (! InBranchDelayD) && (OpD == 0x04 || OpD == 0x05);
		}
		
		/*
		 * EX: Execute, phase 1.
		 * Calculate address for load or store.
		 * Perform the requested ALU operation.
		 */
		
		int InstrE = 0, RsE = 0, RtE = 0, RdE = 0, SignImmE, OpE = 0, FunctE;
		int RsNumE = 0, RtNumE = 0, RdNumE = 0, PCPlus4E = 0;
		int AluOutE = 0, WriteDataE = 0, WriteRegE = 0;
		boolean RegWriteE = false, MemWriteE = false, MemToRegE = false, 
				MemByteE = false, MemHalfwordE = false, stallForMultiplierE = false,
				InBranchDelayE = false, SignExtendE = false;
		if ( ! isEmpty(ID_EX))
		{
			stageOccurred.set(2);
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
				 * Handle an R-type instruction.
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
				case 0x02: //srl
					AluOutE = RtE >>> (InstrE >> 6 & 0b11111);
					break;
				case 0x03: //sra
					AluOutE = RtE >> (InstrE >> 6 & 0b11111);
					break;
				case 0x04: //sllv
					AluOutE = RtE << (RsE & 0b11111);
					break;
				case 0x06: //srlv
					AluOutE = RtE >>> (RsE & 0b11111);
					break;
				case 0x07: //srav
					AluOutE = RtE >> (RsE & 0b11111);
					break;
				case 0x08: //jr
					//do nothing; jump has already been performed
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x09: //jalr
					AluOutE = PCPlus4E + 4;
					RtNumE = 31;
					RegWriteE = true;
					break;
				case 0x0c: //syscall
					setException(MIPSException.SYSCALL, PC - 8, InBranchDelayE);
					break;
				case 0x0d: //break
					setException(MIPSException.BREAK, PC - 8, InBranchDelayE);
					break;
				case 0x10: //mfhi
					stallForMultiplierE = MultiplyUnit.hasStepsRemaining();
					AluOutE = MultiplyUnit.moveFromHi();
					RegWriteE = true;
					break;
				case 0x11: //mthi
					MultiplyUnit.moveToHi(RsE);
					break;
				case 0x12: //mflo
					stallForMultiplierE = MultiplyUnit.hasStepsRemaining();
					AluOutE = MultiplyUnit.moveFromLo();
					RegWriteE = true;
					break;	
				case 0x13: //mtlo
					MultiplyUnit.moveToLo(RsE);
					break;
				case 0x18: //mult
					MultiplyUnit.multiply(RsE, RtE);
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x19: //multu
					MultiplyUnit.multiplyUnsigned(RsE, RtE);
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x1a: //div
					MultiplyUnit.divide(RsE, RtE);
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x1b: //divu
					MultiplyUnit.divideUnsigned(RsE, RtE);
					RegWriteE = false;
					WriteRegE = 0;
					break;
				case 0x20: //add
					try 
					{
						AluOutE = Math.addExact(RsE, RtE);
					}
					catch (ArithmeticException e)
					{
						setException(MIPSException.OVERFLOW, PC - 8, InBranchDelayE);
					}
					break;
				case 0x21: //addu
					AluOutE = RsE + RtE;
					break;
				case 0x22: //sub
					try
					{
						AluOutE = Math.subtractExact(RsE, RtE);
					}
					catch (ArithmeticException e)
					{
						setException(MIPSException.OVERFLOW, PC - 8, InBranchDelayE);
					}
					break;

				case 0x23: //subu
					AluOutE = RsE - RtE;
					break;
				case 0x24: //and
					AluOutE = RsE & RtE;
					break;
				case 0x25: //or
					AluOutE = RsE | RtE;
					break;
				case 0x26: //xor
					AluOutE = RsE ^ RtE;
					break;
				case 0x27: //nor
					AluOutE = ~(RsE | RtE);
				case 0x2a: //slt
					if (RsE < RtE)
						AluOutE = 1;
					else
						AluOutE = 0;
					RegWriteE = true;
					break;
				case 0x2b: //sltu
					if (Integer.compareUnsigned(RsE, RtE) < 0)
						AluOutE = 1;
					else
						AluOutE = 0;
					RegWriteE = true;
					break;
				default:
					setException(MIPSException.ILLEGAL_INSTRUCTION, PC - 8, InBranchDelayE);
					break;
				}
			}
			else if ((OpE & 0b111100) == 16)
			{
				/*
				 * Handle coprocessor instructions.
				 * Only coprocessor 0 is supported for now.
				 * TODO: Technically, moves to/from coprocessors finish in the MEM stage.
				 */
				int whichCoprocessor = (OpE & 0b11);
				
				if (whichCoprocessor != 0)
					setException(MIPSException.COPROCESSOR_UNUSABLE, PC - 8, InBranchDelayE);
				
				int copFunct = RsNumE;
				switch(copFunct)
				{
				case 0x00: //mfcX
					AluOutE = cp0.readRegister(RdNumE);
					RegWriteE = true;
					break;
				case 0x04: //mtcX
					cp0.writeRegister(RdNumE, RtE);
					break;
				case 0x10: //rfe
					int oldStatus = cp0.readRegister(Coprocessor0.STATUS);
					cp0.writeRegister(Coprocessor0.STATUS, (oldStatus & 0xFFFFFFF0) | ((oldStatus >> 2) & 0x0000003F));
					break;
				default:
					/*
					 * According to Kane and Heinrich (1992), no exception is thrown
					 * if the coprocessor instruction is not recognized.
					 */
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
				case 0x20: //lb
					AluOutE = (RsE + SignImmE);
					RegWriteE = MemToRegE = true;
					MemByteE = SignExtendE = true;
					break;
				case 0x21: //lh
					AluOutE = (RsE + SignImmE);
					RegWriteE = MemToRegE = true;
					MemHalfwordE = SignExtendE = true;
					break;
				case 0x23: //lw
					AluOutE = (RsE + SignImmE);
					RegWriteE = MemToRegE = true;
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
				case 0x27: //sh
					AluOutE = (RsE + SignImmE);
					WriteDataE = RtE;
					MemWriteE = MemHalfwordE = true;
					break;
				case 0x2b: //sw
					AluOutE = (RsE + SignImmE);
					WriteDataE = RtE;
					MemWriteE = true;
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


				default:
					setException(MIPSException.ILLEGAL_INSTRUCTION, PC - 8, InBranchDelayE);
					break;
				}
				WriteRegE = RtNumE;
			}
			
		}
		
		/*
		 * MEM: Memory read/write, phase 1.
		 */
		int AluOutM = 0, WriteDataM = 0, WriteRegM = 0, ReadDataM = 0;
		boolean MemToRegM = false, MemWriteM = false, RegWriteM = false, 
				MemByteM = false, MemHalfwordM = false, InBranchDelayM = false,
				SignExtendM = false;
		if ( ! isEmpty(EX_MEM))
		{
			stageOccurred.set(3);
			AluOutM = EX_MEM[0];
			WriteRegM = EX_MEM[2];
			WriteDataM = EX_MEM[3];
			
			RegWriteM = EX_MEM_CTRL.get(0);
			MemWriteM = EX_MEM_CTRL.get(1);
			MemToRegM = EX_MEM_CTRL.get(2);
			MemByteM = EX_MEM_CTRL.get(3);
			MemHalfwordM = EX_MEM_CTRL.get(4);
			InBranchDelayM = EX_MEM_CTRL.get(5);
			
			if (MemWriteM) //if MemWriteM is set, write
			{
				if (AluOutM < 0 && cp0.inUserMode())
					setException(new MIPSException(MIPSException.ADDRESS_ERROR_ON_LOAD, PC - 12, InBranchDelayM)
					 .setBadVAddr(AluOutM));
				else
				{
					if (MemByteM)
							memory.storeByte(AluOutM, WriteDataM);
					else if (MemHalfwordM)
					{
						if (AluOutM % 2 == 0)
							memory.storeHalfword(AluOutM, WriteDataM);
						else
							setException(new MIPSException(MIPSException.ADDRESS_ERROR_ON_STORE, PC - 12, InBranchDelayM)
										 .setBadVAddr(AluOutM));
					}
					else
					{
						if (AluOutM % 4 == 0)
							memory.storeWord(AluOutM, WriteDataM);
						else
							setException(new MIPSException(MIPSException.ADDRESS_ERROR_ON_STORE, PC - 12, InBranchDelayM)
										 .setBadVAddr(AluOutM));
					}
				}
				debugPrint("MEM: memory write at address " + AluOutM + " : " + WriteDataM);
			}
			else if (MemToRegM) //if MemToRegM is set, read
			{
				if (AluOutM < 0 && cp0.inUserMode())
					setException(new MIPSException(MIPSException.ADDRESS_ERROR_ON_LOAD, PC - 12, InBranchDelayM)
					 .setBadVAddr(AluOutM));
				else
				{
					if (MemByteM)
							ReadDataM = memory.loadByte(AluOutM);
					else if (MemHalfwordM)
					{
						if (AluOutM % 2 == 0)
							ReadDataM = memory.loadHalfword(AluOutM);
						else
							setException(new MIPSException(MIPSException.ADDRESS_ERROR_ON_LOAD, PC - 12, InBranchDelayM)
										 .setBadVAddr(AluOutM));
					}
					else
					{
						if (AluOutM % 4 == 0)
							ReadDataM = memory.loadWord(AluOutM);
						else
							setException(new MIPSException(MIPSException.ADDRESS_ERROR_ON_LOAD, PC - 12, InBranchDelayM)
										 .setBadVAddr(AluOutM));
					}
				}
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
			stageOccurred.set(4);
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
				InBranchDelayF = true;
			}
			else if ( ! InBranchDelayD && (OpD == 0x02 || OpD == 0x03)) //j and jal
			{
				NewPCF = (PCPlus4D & 0xF0000000) + ((InstrD & 0x07FFFFFF) << 2);
				InBranchDelayF = true;
			}
			else if ( ! InBranchDelayD && (OpD == 0x00 && ((InstrD & 0b111111) == 0x08 || (InstrD & 0b111111) == 0x09))) //jr and jalr
			{
				NewPCF = RsD;
				InBranchDelayF = true;
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
		EX_MEM_CTRL.set(6, SignExtendE);
		
		MEM_WB[0] = AluOutM;
		MEM_WB[2] = WriteRegM;
		MEM_WB[3] = ReadDataM;
		
		MEM_WB_CTRL.set(0, RegWriteM);
		MEM_WB_CTRL.set(1, MemWriteM);
		MEM_WB_CTRL.set(2, MemToRegM);


		StallF = StallD = FlushE = false;
				
		/*
		 * Exception handler.
		 * Implementation note: any condition which would prevent an interrupt from occurring,
		 * like IE or one of the interrupt masks being clear, should cause currentException
		 * to be set to null before we get here. Most if not all should be checked in 
		 * setException(MIPSException).
		 */
		if (currentException != null)
		{
			cp0.writeRegister(Coprocessor0.BADVADDR, currentException.getBadVAddr());
			cp0.writeRegister(Coprocessor0.CAUSE, currentException.getCause());
			cp0.writeRegister(Coprocessor0.EPC, currentException.getPC());
			
			/*
			 * The Status register contains a three-level "stack" of old values of the Kernel/User mode bit
			 * and the Interrupt Enable bit:
			 * [KU prev.][IE prev.][KU old][IE old][KU][IE]
			 *      5         4        3       2    1   0 
			 * The code below pushes two zeroes onto this stack.
			 */
			int oldStatus = cp0.readRegister(Coprocessor0.STATUS);
			cp0.writeRegister(Coprocessor0.STATUS, (oldStatus & 0xFFFFFFC0) | ((oldStatus << 2) & 0x0000003F));
			
			if (currentException.getPC() <= PC - 16)
			{
				Arrays.fill(MEM_WB, 0);
				MEM_WB_CTRL.clear();
			}
			if (currentException.getPC() <= PC - 12)
			{
				Arrays.fill(EX_MEM, 0);
				EX_MEM_CTRL.clear();
			}
			if (currentException.getPC() <= PC - 8)
			{
				Arrays.fill(ID_EX, 0);
				ID_EX_CTRL.clear();
			}
			if (currentException.getPC() <= PC - 4)
			{
				Arrays.fill(IF_ID, 0);
				IF_ID_CTRL.clear();
			}
			
			PC = cp0.exceptionAddress(currentException);
			
		}
		currentException = null;
		
		/*
		 * Check to see whether we've dropped off the bottom of the executing program.
		 */
		
		if (PC > (memory.getMaxInstAddr() + (4 * 3)) && 
		(isEmpty(ID_EX) && isEmpty(EX_MEM) && isEmpty(MEM_WB)))
		{
			hasNext = false;
		}
		
		/*
		 * Normal simulator code is complete at this point.
		 * Everything that follows is debug code.
		 */
		
		if (DEBUG)
		{
			debugPrint("---------------------------");
			for(int i = 0; i < 10; i++)
			{
				debugPrint(i + "\t" + memory.readRegister(i));
			}
			debugPrint("---------------------------");
			
			debugPrint("VAddr\t($8)\t" + cp0.readRegister(8));
			debugPrint("Status\t($12)\t" + cp0.readRegister(12));
			debugPrint("Cause\t($13)\t" + cp0.readRegister(13));
			debugPrint("EPC\t($14)\t" + cp0.readRegister(14));
			
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
	
	/**
	 * Convenience method which makes a new MIPSException and calls 
	 * setException(MIPSException). Useful when coprocessor unusable/
	 * BadVAddr fields do not need to be set; keeps code legible.
	 * @param cause the cause code of the exception; available cause codes are static fields in the MIPSException class
	 * @param pc the PC when this instruction was loaded into memory. In the ID phase this is given by PC - 4, in the EX phase by PC - 8, etc.
	 * @param inBranchDelay whether this instruction is executing in a branch delay slot.
	 * @see MIPSException
	 * @see #setException(MIPSException e)
	 */
	private void setException(int cause, int pc, boolean inBranchDelay)
	{
		setException(new MIPSException(cause, pc, inBranchDelay));
	}
	
	/**
	 * Loads an exception into the current exception slot, if able.
	 * (Inability to load the exception could be caused by interrupts
	 * being disabled, the interrupt being masked, etc., along with
	 * another interrupt already being present: see currentException.)
	 * @see #currentException
	 * @param e the exception to handle
	 */
	private void setException(MIPSException e)
	{
		if (cp0.interruptsEnabled() && currentException == null)
			currentException = e;
	}
	
	private static class MultiplyUnit
	{
		private static long product;
		private static int rs, rt, stepsRemaining = -1;
		private static enum Operation{MULTIPLY, DIVIDE, MULTIPLY_UNSIGNED, DIVIDE_UNSIGNED};
		private static Operation currentOperation;
		
		public static void multiply(int _rs, int _rt)
		{
			doOperation(Operation.MULTIPLY, _rs, _rt, 14);
		}
		
		public static void divide(int _rs, int _rt)
		{
			doOperation(Operation.DIVIDE, _rs, _rt, 35);
		}
		
		public static void multiplyUnsigned(int _rs, int _rt)
		{
			doOperation(Operation.MULTIPLY, _rs, _rt, 14);
		}
		
		public static void divideUnsigned(int _rs, int _rt)
		{
			doOperation(Operation.DIVIDE_UNSIGNED, _rs, _rt, 35);
		}
		
		private static void doOperation(Operation which, int _rs, int _rt, int _stepsRemaining)
		{
			rs = _rs;
			rt = _rt;
			stepsRemaining = _stepsRemaining;
			currentOperation = which;
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
				case DIVIDE_UNSIGNED:
					product =  0x00000000FFFFFFFFL & (Integer.divideUnsigned(rs, rt));
					product += 0xFFFFFFFF00000000L & ((long)(Integer.remainderUnsigned(rs, rt)) << 32);
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
		
		public static void moveToLo(int what)
		{
			product = (0xFFFFFFFF00000000L & product) + ((long)what & 0xFFFFFFFFL);
		}
		
		public static void moveToHi(int what)
		{
			product = (0xFFFFFFFFL & product) + (((long)what & 0xFFFFFFFFL) << 32);
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
