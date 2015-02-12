package ares.core.assembler;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Static class which reduces source code to a list of tokens.
 * @author James Hester
 * @version 1.0
 *
 */
public class Tokenizer 
{
	private static final char COMMENT_SIGIL = '#';
	private static final char QUOTE_SIGIL = '\"';
	private static final char WINDOWS_STYLE_NEWLINE = '\r';
	private static final char NEWLINE = '\n';
	
	private static int MAX_TOKEN_SIZE = 8192;
	
	/**
	 * Tokenizes a String.
	 * @param fileStream the file to be tokenized
	 * @param filename the name of the file (used in error reporting)
	 * @return a LinkedList of tokens (Strings), with the following guarantees:
	 * <p><ul>
	 * <li> 1. No element will be null.
	 * <li> 2. No element will contain whitespace, EXCEPT
	 * 		<li> 2a. Tokens beginning with a newline (\n) will contain no other character,
	 * 				 and will signify the end of a physical line.
	 * 		<li> 2b. Tokens beginning with a tab (\t) contain the filename of the file being tokenized.
	 * <ul><p>
	 * @throws IOException if an error occurs while reading the file
	 */
	public static LinkedList<String> tokenize(BufferedReader fileStream, String filename) throws IOException
	{
		LinkedList<String> tokenList = new LinkedList<>();
		boolean inComment = false;
		boolean inStrLit = false;
		char[] buf = new char[MAX_TOKEN_SIZE];
		int bufIdx = 0;
		
		tokenList.add("\t" + filename);
		
		for(int ch = fileStream.read(); ch != -1; ch = fileStream.read())
		{
			char n = (char) ch;
			
			if (n == WINDOWS_STYLE_NEWLINE)
				continue;
			
			if (inComment)
			{
				if  (n == NEWLINE)
				{
					inComment = false;
					tokenList.add("\n");
				}
				continue;
			}
			if (n == COMMENT_SIGIL && ! inStrLit)
			{
				inComment = true;
				continue;
			}
			
			if (n == QUOTE_SIGIL)
			{
				inStrLit = ! inStrLit;
				continue;
			}
			
			if ( ! inStrLit)
			{				
				if (Character.isWhitespace(n))
				{
					if (bufIdx > 0)
					{						
						char[] ctok = Arrays.copyOfRange(buf, 0, bufIdx);
						buf = new char[MAX_TOKEN_SIZE];
						bufIdx = 0;
						String completeToken = new String(ctok);
						if (completeToken != null)
							tokenList.add(completeToken);
					}
					if (n == NEWLINE)
						tokenList.add("\n");
					continue;
				}
				else
				{
					buf[bufIdx] = n;
					bufIdx++;
				}
			}
			else
			{
				buf[bufIdx] = n;
				bufIdx++;				
			}
		}
		return tokenList;
	}
	
}
