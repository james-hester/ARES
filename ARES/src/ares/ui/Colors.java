package ares.ui;

import java.awt.Color;

public enum Colors
{
	/**
	 * The color of the outline of every pipeline element.
	 * Also the color of the text drawn within pipeline elements.
	 */
	PIPELINE_ELEMENT (Color.BLACK),
	/**
	 * The background color of the text field (where instructions are displayed.)
	 */
	TEXT_FIELD (Color.BLACK),
	/**
	 * The background color of the main grid where pipeline stages are displayed.
	 */
	BACKGROUND (Color.WHITE),
	/**
	 * The color of the instruction labels.
	 */
	INSTRUCTION_LABEL (Color.WHITE),
	
	PIPELINE_HIGHLIGHT (new Color(0x3FD13F)),
	
	FORWARDING_ARROW (new Color(0xD26405));
	
	Color theColor;
	private Colors(Color c)
	{
		theColor = c;
	}
	
	public Color getColor()
	{
		return theColor;
	}

	
}
