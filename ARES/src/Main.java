import javax.swing.JOptionPane;

import ares.adapter.*;

public class Main {

	public static void main(String[] args)
	{
		/*
		 * Before we even try to run, we need to be sure the installed JRE can do what we ask it to.
		 * To check the version of Java, the Java 8-specific methods used in my code are called; if the Java version
		 * is too low, a NoSuchMethodException is thrown.
		 * This is a roundabout way of checking the Java version, but the System properties (java.version, etc.)
		 * are String representations of the version names (like "1.8.0".) Suppose Java 10 is released--if its version
		 * number is "1.10.x", but my code takes the version number to be "1.10" and compares it to 1.7, it will incorrectly
		 * conclude that the version level is too low. 
		 */
		try
		{
			Integer.parseUnsignedInt("1");
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Java version 8 or greater is required to run this program.\nPlease download the latest version of Java at: https://java.com/download/",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		new GUIAdapter();
		//new AssemblerTestAdapter();
		//new SimpleAdapter();
	}

}
