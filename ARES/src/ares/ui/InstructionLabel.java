package ares.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class InstructionLabel extends AnimatedComponent {

	String text;
	
	public InstructionLabel(String text, int x, int y)
	{
		super(x, y);
		this.text = text;
	}

	
	
	@Override
	public void render(Graphics2D g2)
	{
		int x = (int) this.x;
		int y = (int) this.y;
		
		g2.setColor(Color.WHITE);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
		g2.drawString(text, x, y);
	}

	public void move(double dX, double dY)
	{
		this.x += dX;
		this.y += dY;
	}
	
	public void setText(String text)
	{
		this.text = text;
	}
	
	public String getText()
	{
		return text;
	}
	
}
