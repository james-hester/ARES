package ares.core;

import java.util.Arrays;
import java.util.HashMap;

public class Memory
{
	public static final int TEXT_SEGMENT_START_ADDRESS = 0x00400000;
	
	private HashMap<Integer, Integer> mainMemory = new HashMap<>();
	private int[] registers = new int[32];
	private int maxInstructionAddress = 0;
	
	public Memory()
	{
		Arrays.fill(registers, 0);
		
	}
	
	public void setMaxInstAddr(int mIA)
	{
		maxInstructionAddress = mIA;
	}
	
	public int getMaxInstAddr()
	{
		return maxInstructionAddress;
	}
	
	public int read(int address)
	{
		Integer result = mainMemory.get(address);
		if (result == null)
			return 0;
		return result.intValue();
	}
	
	public void write(int address, int data)
	{
		mainMemory.put(address, data);
	}
	
	public int readRegister(int which)
	{
		return registers[which];
	}
	
	public void writeRegister(int which, int data)
	{
		if (which <= 0 || which > 31)
			return;
		registers[which] = data;
	}
}
