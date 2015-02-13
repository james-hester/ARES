package ares.core;

import java.util.Arrays;
import java.util.HashMap;

public class Memory
{
	public static final int TEXT_SEGMENT_START_ADDRESS = 0x00400000;
	public static final int DATA_SEGMENT_START_ADDRESS = 0x10010000;
	public static final int KTEXT_SEGMENT_START_ADDRESS = 0x80000000;
	public static final int KDATA_SEGMENT_START_ADDRESS = 0x90000000;
	
	private static final int BLOCK_SIZE = 0xFFF;

	private HashMap<Integer, byte[]> mainMemory = new HashMap<>();
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
	
	private int load(int address, int numBytes)
	{
		int hiAddress = address & (0xFFFFFFFF - BLOCK_SIZE);
		int loAddress = address & BLOCK_SIZE;
		boolean wasEmpty = false;
		byte[] block = mainMemory.get(hiAddress);
		if (block == null)
		{
			block = new byte[BLOCK_SIZE + 1];
			wasEmpty = true;
		}
		
		int result = 0;
		
		for(int i = 0; i < numBytes; i++)
			result |= (block[loAddress + i] << (8 * i)) & (0xFF << (8 * i));
		
		if (wasEmpty)
			mainMemory.put(hiAddress, block);
		
		return result;
	}
	
	private void store(int address, int data, int numBytes)
	{
		int hiAddress = address & (0xFFFFFFFF - BLOCK_SIZE);
		int loAddress = address & BLOCK_SIZE;
		
		byte[] block = mainMemory.get(hiAddress);
		if (block == null)
			block = new byte[BLOCK_SIZE + 1];
		for(int i = 0; i < numBytes; i++)
			block[loAddress + i] = (byte) ((data >>> (8 * i)) & 0xFF);

		mainMemory.put(hiAddress, block);
	}
	
	public int loadWord(int address)
	{
		return load(address, 4);
	}
	
	public int loadHalfword(int address)
	{
		return load(address, 2);
	}
	
	public int loadByte(int address)
	{
		return load(address, 1);
	}
	
	public void storeWord(int address, int data)
	{
		store(address, data, 4);
	}
	
	public void storeHalfword(int address, int data)
	{
		store(address, data, 2);
	}
	
	public void storeByte(int address, int data)
	{
		store(address, data, 1);
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
