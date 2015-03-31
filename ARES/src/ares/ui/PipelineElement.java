package ares.ui;

import static ares.ui.Colors.PIPELINE_ELEMENT;
import static ares.ui.Colors.PIPELINE_HIGHLIGHT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.BitSet;

public class PipelineElement
{
	public static final double ARROW_SIDE_LENGTH = 11.0;
	public static final float ARROW_WIDTH = 2.0f;
	
	BitSet hasStage = new BitSet(5);
	BitSet hasHole = new BitSet(5);
	BitSet forwardingOccurred = new BitSet(6);
	
	private double dbX = 0, dbY = 0;
	private int x, y;
	private boolean isHighlighted = false;
	
	private String instructionFetchedF = "";
	private String argTopD = "", argBottomD = "";
	private String instructionExecutedE = "";
	
	private int operationM = 0;
	private String addressM = "";
	
	private String registerW = "";
	
	public static final int WIDTH = 80;
	public static final int HEIGHT = 74;
	
	public PipelineElement(double x, double y)
	{
		move(x, y);
		hasStage.clear();
		hasStage.set(0);
		hasHole.clear();
	}
	
	public void setForwardingOccurred(BitSet forwardingOccurred)
	{
		for(int i = 0; i < forwardingOccurred.size(); i++)
			this.forwardingOccurred.set(i, forwardingOccurred.get(i));
	}
	
	public void setStage(int which, boolean newValue)
	{
		hasStage.set(which, newValue);
	}

	public boolean getStage(int which)
	{
		return hasStage.get(which);
	}
	
	public void setHole(int which, boolean newValue)
	{
		hasHole.set(which, newValue);
	}
	
	public boolean getHole(int which)
	{
		return hasHole.get(which);
	}
	
	public void toggleHighlighted()
	{
		isHighlighted = ( ! isHighlighted);
	}
	
	public void setIFData(String instructionFetched)
	{
		this.instructionFetchedF = instructionFetched;
	}
	
	public void setIDData(String argTop, String argBottom)
	{
		this.argBottomD = argBottom;
		this.argTopD = argTop;
	}
	
	public void setEXData(String instructionExecuted)
	{
		this.instructionExecutedE = instructionExecuted;
	}
	
	public void setMEMData(int operation, String address)
	{
		this.operationM = operation;
		this.addressM = address;
	}
	
	public void setWBData(String registerWritten)
	{
		System.out.println("rw: " + registerWritten);
		this.registerW = registerWritten;
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
		int oldY = y;
		y += (4 * HEIGHT);
		g2.setColor(PIPELINE_ELEMENT.getColor());
		
		if (hasHole.get(0))
		{
			y -= HEIGHT;
		}
		if (hasStage.get(0))
		{
			drawIF(g2, isHighlighted);
			y -= HEIGHT;
		}
		
		if (hasHole.get(1))
		{
			y -= HEIGHT;
		}
		if (hasStage.get(1))
		{
			drawID(g2, isHighlighted);
			y -= HEIGHT;
		}

		if (hasHole.get(2))
			y -= HEIGHT;		
		if (hasStage.get(2))
		{
			drawEX(g2, isHighlighted);
			y -= HEIGHT;
		}
		
		if (hasHole.get(3))
			y -= HEIGHT;
		if (hasStage.get(3))
		{
			drawMEM(g2, isHighlighted);
			y -= HEIGHT;
		}

		if (hasHole.get(4))
			y -= HEIGHT;		
		if (hasStage.get(4))
		{
			drawWB(g2, isHighlighted);
		}
		
		y = oldY;
	}
	
	/*
	 * As their names imply, each of these functions draws its associated pipeline
	 * stage into the (width * height) box with (x,y) representing the top left corner.
	 * 
	 */
	
	private void drawIF(Graphics2D g2, boolean highlight)
	{
		
		
		if (highlight)
		{
			g2.setColor(PIPELINE_HIGHLIGHT.getColor());
			g2.fillRect(x, y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
			g2.setColor(PIPELINE_ELEMENT.getColor());
			g2.setStroke(new BasicStroke((float) 2.0));
			g2.drawRect(x, y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
			g2.setStroke(new BasicStroke((float) 1.0));
		}
		else
		{
			g2.drawRect(x, y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
		}
		drawStringCentered(instructionFetchedF, g2, x, y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
		g2.drawLine(x + (WIDTH / 2),  y + (HEIGHT / 2), x + WIDTH - 1, y + (HEIGHT / 2));
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
	}
	
	private void drawID(Graphics2D g2, boolean highlight)
	{
		g2.drawLine(x, y + (HEIGHT / 2), x + (2 * WIDTH / 16), y + (HEIGHT / 2));
		if (highlight)
		{
			g2.setColor(PIPELINE_HIGHLIGHT.getColor());
			g2.fillPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (11 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 2), y + (HEIGHT / 4), y + (3 * HEIGHT / 4)}, 
					3);
			g2.setColor(PIPELINE_ELEMENT.getColor());
			g2.setStroke(new BasicStroke((float) 2.0));
			g2.drawPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (11 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 2), y + (HEIGHT / 4), y + (3 * HEIGHT / 4)}, 
					3);
			g2.setStroke(new BasicStroke((float) 1.0));
		}
		else
		{
			g2.drawPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (11 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 2), y + (HEIGHT / 4), y + (3 * HEIGHT / 4)}, 
					3);
		}
		
		drawStringRightAligned(argTopD, g2, x, y, (7 * WIDTH / 8), (HEIGHT / 4));
		drawStringRightAligned(argBottomD, g2, x, y + (3 * HEIGHT / 4), (7 * WIDTH / 8), (HEIGHT / 4));
		
		g2.drawLine(x + (11 * WIDTH / 16), y + (3 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (3 * HEIGHT / 8));
		g2.drawLine(x + (11 * WIDTH / 16), y + (5 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (5 * HEIGHT / 8));
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
	}
	
	private void drawEX(Graphics2D g2, boolean highlight)
	{
		g2.drawLine(x, y + (3 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (3 * HEIGHT / 8));
		g2.drawLine(x, y + (5 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (5 * HEIGHT / 8));
		
		/*
		 * The points in the polygon start at the top left corner of the shape and go counterclockwise:
		 * 
		 * p1
		 * 
		 * p2			p7
		 * 		p3
		 * p4			p6
		 * 
		 * p5
		 * 
		 */
		g2.drawPolygon(
				new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (3 * WIDTH / 16), x + (2 * WIDTH / 16),
						  x + (2 * WIDTH / 16), x + (10 * WIDTH / 16), x + (10 * WIDTH / 16)}, 
				new int[]{y + (HEIGHT / 4), y + (7 * HEIGHT / 16), y + (HEIGHT / 2), y + (9 * HEIGHT / 16), 
						  y + (3 * HEIGHT / 4), y + (5 * HEIGHT / 8), y + (3 * HEIGHT / 8)}, 
				7);
		
		if (highlight)
		{
			g2.setColor(PIPELINE_HIGHLIGHT.getColor());
			g2.fillPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (3 * WIDTH / 16), x + (2 * WIDTH / 16),
							  x + (2 * WIDTH / 16), x + (10 * WIDTH / 16), x + (10 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 4), y + (7 * HEIGHT / 16), y + (HEIGHT / 2), y + (9 * HEIGHT / 16), 
							  y + (3 * HEIGHT / 4), y + (5 * HEIGHT / 8), y + (3 * HEIGHT / 8)}, 
					7);
			g2.setColor(PIPELINE_ELEMENT.getColor());
			g2.setStroke(new BasicStroke((float) 2.0));
			g2.drawPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (3 * WIDTH / 16), x + (2 * WIDTH / 16),
							  x + (2 * WIDTH / 16), x + (10 * WIDTH / 16), x + (10 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 4), y + (7 * HEIGHT / 16), y + (HEIGHT / 2), y + (9 * HEIGHT / 16), 
							  y + (3 * HEIGHT / 4), y + (5 * HEIGHT / 8), y + (3 * HEIGHT / 8)}, 
					7);
			g2.setStroke(new BasicStroke((float) 1.0));
		}
		else
		{
			g2.drawPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (3 * WIDTH / 16), x + (2 * WIDTH / 16),
							  x + (2 * WIDTH / 16), x + (10 * WIDTH / 16), x + (10 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 4), y + (7 * HEIGHT / 16), y + (HEIGHT / 2), y + (9 * HEIGHT / 16), 
							  y + (3 * HEIGHT / 4), y + (5 * HEIGHT / 8), y + (3 * HEIGHT / 8)}, 
					7);
		}
		
		drawStringCentered(instructionExecutedE, g2, x + (2 * WIDTH / 16), y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2); 
		
		g2.drawLine(x + (5 * WIDTH / 8),  y + (HEIGHT / 2), x + (7 * WIDTH / 8), y + (HEIGHT / 2));		
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
		
		if (forwardingOccurred.get(0))
			drawArrow(g2, x + (3 * WIDTH / 4), y + (HEIGHT / 2), x + (17 * WIDTH / 16), y + (11 * HEIGHT / 8), Colors.FORWARDING_ARROW.getColor());		
		else if (forwardingOccurred.get(2))
			drawArrow(g2, x + (3 * WIDTH / 4), y + (HEIGHT / 2), x + (17 * WIDTH / 16), y + (13 * HEIGHT / 8), Colors.FORWARDING_ARROW.getColor());
	}
	
	private void drawMEM(Graphics2D g2, boolean highlight)
	{
		if (forwardingOccurred.get(1))
			drawArrow(g2, x + (WIDTH / 16), y + (HEIGHT / 2), x + (17 * WIDTH / 16), y + (19 * HEIGHT / 8), Colors.FORWARDING_ARROW.getColor());		
		else if (forwardingOccurred.get(3))
			drawArrow(g2, x + (WIDTH / 16), y + (HEIGHT / 2), x + (17 * WIDTH / 16), y + (21 * HEIGHT / 8), Colors.FORWARDING_ARROW.getColor());
		
		g2.drawLine(x, y + (HEIGHT / 2), x + (int)(2.5 * WIDTH / 16), y + (HEIGHT / 2));
		g2.drawLine(x + (WIDTH / 16), y + (HEIGHT / 2), x + (WIDTH / 16), y + (7 * HEIGHT / 8));
		g2.drawLine(x + (WIDTH / 16), y + (7 * HEIGHT / 8), x + (3 * WIDTH / 4), y + (7 * HEIGHT / 8));
		g2.drawLine(x + (3 * WIDTH / 4), y + (7 * HEIGHT / 8), x + (3 * WIDTH / 4), y + (5 * HEIGHT / 8));
		g2.drawLine(x + (3 * WIDTH / 4), y + (5 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (5 * HEIGHT / 8));
		g2.drawLine(x + (int)(10.5 * WIDTH / 16), y + (3 * HEIGHT / 8), x + (7 * WIDTH / 8), y + (3 * HEIGHT / 8));
		if (highlight)
		{
			g2.setColor(PIPELINE_HIGHLIGHT.getColor());
			g2.fillRect(x + (int)(2.5 * WIDTH / 16), y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
			g2.setColor(PIPELINE_ELEMENT.getColor());
			g2.setStroke(new BasicStroke((float) 2.0));
			g2.drawRect(x + (int)(2.5 * WIDTH / 16), y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
			g2.setStroke(new BasicStroke((float) 1.0));
		}
		else
		{
			g2.drawRect(x + (int)(2.5 * WIDTH / 16), y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
		}
		
		if(operationM != 0)
		{
			Font oldFont = g2.getFont();
			g2.setFont(new Font("Arial", Font.PLAIN, 10));
			drawStringCentered(addressM, g2, x, y, (7 * WIDTH / 8), (HEIGHT / 4));
			g2.setFont(oldFont);
			g2.drawRect(x + (int)(2.5 * WIDTH / 16) + WIDTH / 6, y + (HEIGHT / 2), WIDTH / 6, HEIGHT / 6);
			g2.drawLine(x + (int)(2.5 * WIDTH / 16) + WIDTH / 4, y + (2 * HEIGHT / 5), x + (int)(2.5 * WIDTH / 16) + WIDTH / 4, y + (2 * HEIGHT / 3));
			switch(operationM)
			{
			case 1:
				g2.fillPolygon(new int[]{x + (int)(2.5 * WIDTH / 16) + WIDTH / 4 - WIDTH / 24, 
						x + (int)(2.5 * WIDTH / 16) + WIDTH / 4 + WIDTH / 24, 
						x + (int)(2.5 * WIDTH / 16) + WIDTH / 4}, 
						new int[]{y + (2 * HEIGHT / 5), y + (2 * HEIGHT / 5), y + (HEIGHT / 3)},
						3);
				break;
			case 2:
				g2.fillPolygon(new int[]{x + (int)(2.5 * WIDTH / 16) + WIDTH / 4 - WIDTH / 24, 
						x + (int)(2.5 * WIDTH / 16) + WIDTH / 4 + WIDTH / 24, 
						x + (int)(2.5 * WIDTH / 16) + WIDTH / 4}, 
						new int[]{y + (7 * HEIGHT / 12), y + (7 * HEIGHT / 12), y + (2 * HEIGHT / 3)},
						3);
				break;
			}
		}
		g2.fillRect(x + ((7 * WIDTH) / 8), y, WIDTH / 8, HEIGHT);
	}
	
	private void drawWB(Graphics2D g2, boolean highlight)
	{
		g2.drawLine(x, y + (3 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (3 * HEIGHT / 8));
		g2.drawLine(x, y + (5 * HEIGHT / 8), x + (2 * WIDTH / 16), y + (5 * HEIGHT / 8));
		if (highlight)
		{
			g2.setColor(PIPELINE_HIGHLIGHT.getColor());
			g2.fillPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 4), y + (3 * HEIGHT / 4), y + (HEIGHT / 2)}, 
					3);
			g2.setColor(PIPELINE_ELEMENT.getColor());
			g2.setStroke(new BasicStroke((float) 2.0));
			g2.drawPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 4), y + (3 * HEIGHT / 4), y + (HEIGHT / 2)}, 
					3);
			g2.setStroke(new BasicStroke((float) 1.0));
		}
		else
		{
			g2.drawPolygon(
					new int[]{x + (2 * WIDTH / 16), x + (2 * WIDTH / 16), x + (11 * WIDTH / 16)}, 
					new int[]{y + (HEIGHT / 4), y + (3 * HEIGHT / 4), y + (HEIGHT / 2)}, 
					3);
		}
		
		drawStringCentered(registerW, g2, x + (WIDTH / 12), y + (HEIGHT / 4), WIDTH / 2, HEIGHT / 2);
	}
	
	
	/**
	 * Draws an arrow from (x1, y1) to (x2, y2), pointing towards the second point.
	 * The length of the sides of the arrowhead is given by ARROW_SIDE_LENGTH (in the Constants class.)
	 */
	private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, Color c)
	{
		final double SQRT_2_OVER_2 = Math.sqrt(2) / 2.0; //Used several times; would be wasteful to recompute.
		Color oldColor = g2.getColor();
		g2.setColor(c);
		g2.setStroke(new BasicStroke(ARROW_WIDTH));
		/*
		 * First, draw the line.
		 */
		g2.drawLine(x1, y1, x2, y2);
		/*
		 * Now, draw the arrowhead: the triangle at the end of the arrow.
		 * The completed arrow will look like this:
		 *       Z
		 * 		 |
		 * 		...
		 *       |
		 * D*****B*****C
		 *  **   |   **
		 *   **  |  ** 
		 *    ** | **
		 *      *A*
		 *
		 * We know the following:
		 *   The point A is (x2, y2).
		 *   The line CD is perpendicular to AZ.
		 *   The lines AC and AD are of known length (ARROW_SIDE_LENGTH).
		 *   The lines BC and BD are equal in length.
		 * Hence, the triangles ABD and ABC are congruent, and 
		 * simple geometry gives the length of AB, BC, and BD:
		 * (sqrt(2) / 2) * ARROW_SIDE_LENGTH.
		 */
		/*
		 * Determine the length of the line from (x1, y1) to (x2, y2).
		 * Intermediate results are stored as they will be used later.
		 */
		double deltaY = (y2 - y1);
		double deltaX = (x2 - x1);
		double dY2 = deltaY * deltaY;
		double dX2 = deltaX * deltaX;
		double lineLength = Math.sqrt((double)(dX2) + (double)(dY2));
		/*
		 * The coordinates of B are found relative to A by subtracting the product
		 * of the component deltas and the ratio of AB to AZ.
		 */
		double y3 = y2 - ((deltaY) * ((SQRT_2_OVER_2 * ARROW_SIDE_LENGTH)/ lineLength));
		double x3 = x2 - ((deltaX) * ((SQRT_2_OVER_2 * ARROW_SIDE_LENGTH)/ lineLength));
		/*
		 * The coordinates of C are found relative to B as follows: 
		 *    C
		 *   * *
		 *  *  *
		 * B***X
		 * The angle B is atan( - deltaX / deltaY), and the length BC is known.
		 * The vertical distance is given by BC*sin(atan(angle(B)))
		 * 
		 */
		double ang = Math.sqrt((deltaX * deltaX) / (deltaY * deltaY) + 1);
		double y4 = y3 + ((SQRT_2_OVER_2 * ARROW_SIDE_LENGTH) * -deltaX / (deltaY * ang));
		double x4 = x3 + ((SQRT_2_OVER_2 * ARROW_SIDE_LENGTH) * 1.0 / ang);
		
		double y5 = y3 - ((SQRT_2_OVER_2 * ARROW_SIDE_LENGTH) * -deltaX / (deltaY * ang));
		double x5 = x3 - ((SQRT_2_OVER_2 * ARROW_SIDE_LENGTH) * 1.0 / ang);
		
		g2.fillPolygon(new int[]{x2, (int) x4, (int) x5},
				new int[]{y2, (int) y4, (int) y5},
				3);
		g2.drawLine((int)x4, (int)y4, (int)x5, (int)y5);
		g2.setStroke(new BasicStroke((float) 1.0));
		
		g2.setColor(oldColor);
		
	}
	
	private void drawStringCentered(String str, Graphics2D g2, int x, int y, int width, int height)
	{
		int strWidth = g2.getFontMetrics().stringWidth(str);
		int strHeight = g2.getFontMetrics().getHeight() - g2.getFontMetrics().getDescent();
		
		int xToDraw = x + ((width - strWidth) / 2);
		int yToDraw = y + ((height + strHeight) / 2);
		
		g2.drawString(str, xToDraw, yToDraw);
	}
	
	private void drawStringRightAligned(String str, Graphics2D g2, int x, int y, int width, int height)
	{
		int strWidth = g2.getFontMetrics().stringWidth(str);
		int strHeight = g2.getFontMetrics().getHeight();
		
		int xToDraw = x + ((width - strWidth));
		int yToDraw = y + ((height + strHeight) / 2);
		
		g2.drawString(str, xToDraw, yToDraw);
	}

}
