package ares.ui.temp;


import ares.core.assembler.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;

public class AssemblerTest {

	public static void main(String[] args) throws FileNotFoundException
	{
		BufferedReader theFile = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(
								new File("lexathon.asm")
										   ), Charset.forName("UTF-8")));
		
		try {
			LinkedList<String> result = Tokenizer.tokenize(theFile, "lexathon.asm");
			System.out.println(result);
			Assembler as = new Assembler(result);
			as.doFirstPass();
			
		} catch (AssemblerError e) {
			System.err.println(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		

	}

}
