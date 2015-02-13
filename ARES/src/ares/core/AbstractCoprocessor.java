package ares.core;

public abstract class AbstractCoprocessor
{
	protected int[] registers;
	
	public AbstractCoprocessor()
	{
		this(32);
	}
	
	public AbstractCoprocessor(int numRegisters)
	{
		registers = new int[numRegisters];
	}
	
	public void writeRegister(int which, int data)
	{
		registers[which] = data;
	}
	
	public int readRegister(int which)
	{
		return registers[which];
	}
	
	public abstract void doOperation(int which);
	
	
}
