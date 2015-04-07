package ares.core;

public class MultiplyUnit
{
	private long product;
	private int rs, rt, stepsRemaining = -1;
	private static enum Operation{MULTIPLY, DIVIDE, MULTIPLY_UNSIGNED, DIVIDE_UNSIGNED};
	private Operation currentOperation;
	
	public void multiply(int _rs, int _rt)
	{
		doOperation(Operation.MULTIPLY, _rs, _rt, 14);
	}
	
	public void divide(int _rs, int _rt)
	{
		doOperation(Operation.DIVIDE, _rs, _rt, 35);
	}
	
	public void multiplyUnsigned(int _rs, int _rt)
	{
		doOperation(Operation.MULTIPLY, _rs, _rt, 14);
	}
	
	public void divideUnsigned(int _rs, int _rt)
	{
		doOperation(Operation.DIVIDE_UNSIGNED, _rs, _rt, 35);
	}
	
	private void doOperation(Operation which, int _rs, int _rt, int _stepsRemaining)
	{
		rs = _rs;
		rt = _rt;
		stepsRemaining = _stepsRemaining;
		currentOperation = which;
	}
	
	public void step()
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
	
	public boolean hasStepsRemaining()
	{
		return (stepsRemaining > 0);
	}
	
	public int moveFromHi()
	{
		return (int)((product & 0xFFFFFFFF00000000L) >> 32);
	}
	
	public int moveFromLo()
	{
		return (int)((product & 0x00000000FFFFFFFFL));
	}
	
	public void moveToLo(int what)
	{
		product = (0xFFFFFFFF00000000L & product) + ((long)what & 0xFFFFFFFFL);
	}
	
	public void moveToHi(int what)
	{
		product = (0xFFFFFFFFL & product) + (((long)what & 0xFFFFFFFFL) << 32);
	}
}
