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
		                
		try
		{
			System.out.println(System.getProperty("user.dir"));
			readFile = new Scanner(new File("exception_test.txt"));
			int i = 0;
			while (readFile.hasNextLine())
			{
				int a = Integer.parseUnsignedInt(readFile.nextLine(), 16);
				m.storeWord(Memory.TEXT_SEGMENT_START_ADDRESS + i, a);
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
			Scanner kboard = new Scanner(System.in);
			kboard.nextLine();
			
		}
		long end = System.currentTimeMillis();
		System.out.println("Clock speed: " + ((double)i / (1000.0 * (double)(end - start)) + " MHz") );
				
	}
}
