package ares.core;

import java.util.Arrays;
import java.util.HashMap;

public class Memory
{
	public static final int TEXT_SEGMENT_START_ADDRESS = 0x00400000;
	
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
	
	public int read(int address)
	{
		int hiAddress = address & (0xFFFFFFFF - BLOCK_SIZE);
		int loAddress = address & BLOCK_SIZE;
		boolean wasEmpty = false;
		byte[] block = mainMemory.get(hiAddress);
		if (block == null)
		{
			block = new byte[BLOCK_SIZE];
			wasEmpty = true;
		}
		
		int result = 0;
		result |= (block[loAddress]) & 0xFF;
		result |= (block[loAddress + 1] << 8) & 0xFF00;
		result |= (block[loAddress + 2] << 16) & 0xFF0000;
		result |= (block[loAddress + 3] << 24) & 0xFF000000;
		
		if (wasEmpty)
			mainMemory.put(hiAddress, block);
		
		return result;
	}
	
	public void write(int address, int data)
	{
		int hiAddress = address & (0xFFFFFFFF - BLOCK_SIZE);
		int loAddress = address & BLOCK_SIZE;
		
		byte[] block = mainMemory.get(hiAddress);
		if (block == null)
			block = new byte[BLOCK_SIZE];
		block[loAddress] = (byte) (data & 0xFF);
		block[loAddress + 1] = (byte) ((data >>> 8) & 0xFF);
		block[loAddress + 2] = (byte) ((data >>> 16) & 0xFF);
		block[loAddress + 3] = (byte) ((data >>> 24) & 0xFF);
		mainMemory.put(hiAddress, block);
	}
	
	public void writeByte(int address, int data)
	{
		//mainMemory.put(address, data);
	}
	
	public void writeHalfword(int address, int data)
	{
		//mainMemory.put(address, data);
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
