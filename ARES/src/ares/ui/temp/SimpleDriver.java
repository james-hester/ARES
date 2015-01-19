package ares.ui.temp;

import java.util.*;
import java.io.File;

import ares.core.*;

public class SimpleDriver
{
	
	public static void main(String[] args)
	{
		Scanner readFile;
		Memory m = new Memory();
		
                //System.out.println(System.getProperty("user.dir"));
                
		try
		{
			readFile = new Scanner(new File("comprehensive_test.txt"));
			int i = 0;
			while (readFile.hasNextLine())
			{
				int a = Integer.parseUnsignedInt(readFile.nextLine(), 16);
				m.write(Memory.TEXT_SEGMENT_START_ADDRESS + i, a);
				System.out.println(a + "\t" + m.read(Memory.TEXT_SEGMENT_START_ADDRESS + i));
				i += 4;
			}
			m.setMaxInstAddr(Memory.TEXT_SEGMENT_START_ADDRESS + i);
			readFile.close();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return;
		}
				
		Simulator s = new Simulator(m);
		int i = 0;
		long start = System.currentTimeMillis();
		while (s.hasNextInstruction())
		{
			i++;
			System.out.println("\n(" + i + ")\n");
			s.step();
			
		}
		long end = System.currentTimeMillis();
		System.out.println("Clock speed: " + ((double)i / (1000.0 * (double)(end - start)) + " MHz") );
				
	}
		
	/*
	public static void main(String[] args)
	{
		StringBuilder theProgram = new StringBuilder("");
		if (editProgram(theProgram))
		{
			try
			{
				CompiledProgram cp = Assembler.assemble(theProgram.toString());
			}
			catch (Exception e)
			{
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}

	}
  
	
	public static boolean editProgram(StringBuilder theProgram)
	{
		boolean editing = true;
		Scanner kb = new Scanner(System.in);
		do
		{
			System.out.print("> ");
			String input = kb.nextLine() + '\n';
			if (input.charAt(0) == '/' && input.length() > 1)
			{
				switch(input.charAt(1))
				{
				case 'q':
					return false;
				case 'c':
					return true;
				case 'w':
					System.out.println(theProgram);
				}
			}
			else
			{
				theProgram = theProgram.append(input);
			}
		}
		while (editing);
		return false;
	}
*/
}
