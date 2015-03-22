package ares.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.BitSet;

import javax.swing.JComponent;

public class PipelineElement
{
	private BitSet hasStage = new BitSet(5);
	
	private double dbX = 0, dbY = 0;
	private int x, y;
	
	public static final int WIDTH = 80;
	public static final int HEIGHT = 74;
	
	public PipelineElement(double x, double y)
	{
		move(x, y);
		hasStage.clear();
	}
	
	public void setStage(int which, boolean newValue)
	{
		hasStage.set(which, newValue);
	}

	public boolean getStage(int which)
	{
		return hasStage.get(which);
	}
	
	public void move(double xDelta, double yDelta)
	{
		dbX += xDelta;
		dbY += yDelta;
		this.x = Math.round( (float) this.dbX);
		this.y = Math.round( (float) this.dbY);
	}
	
	public void render(Graphics2D g2)
	{
		y += (4 * HEIGHT);
		
		g2.setColor(Color.WHITE);
		if (hasStage.get(0))
			drawIF(g2);
		
		y -= HEIGHT;
		if (hasStage.get(1))
			drawID(g2);
		
		y -= HEIGHT;
		if (hasStage.get(2))
			drawEX(g2);
		
		y -= HEIGHT;
		if (hasStage.get(3))
			drawMEM(g2);
		
		y -= HEIGHT;
		if (hasStage.get(4))
			drawWB(g2);
	}
	
	/*
	 * As their names imply, each of these functions draws its associated pipeline
	 * stage into the (width * height) box with (x,y) representing the top left corner.
	 * 
	 */
	
	private void drawIF(Graphics2D g2)
	{
		g2.drawRect(x, y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
		g2.drawLine(x + (WIDTH / 2),  y + (HEIGHT / 2), x + WIDTH - 1, y + (HEIGHT / 2));
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
	}
	
	private void drawID(Graphics2D g2)
	{
		g2.drawLine(x, y + (HEIGHT / 2), x + (2 * WIDTH / 16), y + (HEIGHT / 2));
		g2.drawPolygon(
				new int[]{x + (2 * WIDTH / 16), x + (11 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
				new int[]{y + (HEIGHT / 2), y + (HEIGHT / 4), y + (3 * HEIGHT / 4)}, 
				3);
		g2.drawLine(x + (11 * WIDTH / 16), y + (3 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (3 * HEIGHT / 8));
		g2.drawLine(x + (11 * WIDTH / 16), y + (5 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (5 * HEIGHT / 8));
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
	}
	
	private void drawEX(Graphics2D g2)
	{
		g2.drawLine(x, y + (3 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (3 * HEIGHT / 8));
		g2.drawLine(x, y + (5 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (5 * HEIGHT / 8));
		
		g2.drawPolygon(
				new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (3 * WIDTH / 16), x + (2 * WIDTH / 16),
						  x + (2 * WIDTH / 16), x + (10 * WIDTH / 16), x + (10 * WIDTH / 16)}, 
				new int[]{y + (HEIGHT / 4), y + (7 * HEIGHT / 16), y + (HEIGHT / 2), y + (9 * HEIGHT / 16), 
						  y + (3 * HEIGHT / 4), y + (5 * HEIGHT / 8), y + (3 * HEIGHT / 8)}, 
				7);
		
		g2.drawLine(x + (5 * WIDTH / 8),  y + (HEIGHT / 2), x + (7 * WIDTH / 8), y + (HEIGHT / 2));
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
	}
	
	private void drawMEM(Graphics2D g2)
	{
		g2.drawLine(x, y + (HEIGHT / 2), x + (int)(2.5 * WIDTH / 16), y + (HEIGHT / 2));
		g2.drawLine(x + (WIDTH / 16), y + (HEIGHT / 2), x + (WIDTH / 16), y + (7 * HEIGHT / 8));
		g2.drawLine(x + (WIDTH / 16), y + (7 * HEIGHT / 8), x + (3 * WIDTH / 4), y + (7 * HEIGHT / 8));
		g2.drawLine(x + (3 * WIDTH / 4), y + (7 * HEIGHT / 8), x + (3 * WIDTH / 4), y + (5 * HEIGHT / 8));
		g2.drawLine(x + (3 * WIDTH / 4), y + (5 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (5 * HEIGHT / 8));
		g2.drawLine(x + (int)(10.5 * WIDTH / 16), y + (3 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (3 * HEIGHT / 8));
		g2.drawRect(x + (int)(2.5 * WIDTH / 16), y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
	}
	
	private void drawWB(Graphics2D g2)
	{
		g2.drawLine(x, y + (3 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (3 * HEIGHT / 8));
		g2.drawLine(x, y + (5 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (5 * HEIGHT / 8));
		g2.drawPolygon(
				new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
				new int[]{y + (HEIGHT / 4), y + (3 * HEIGHT / 4), y + (HEIGHT / 2)}, 
				3);
	}

}
