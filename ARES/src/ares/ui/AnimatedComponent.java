package ares.ui;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.Timer;

public abstract class AnimatedComponent
{
	protected double x, y;
	
	public AnimatedComponent(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public abstract void render(Graphics2D g2);
	

}
