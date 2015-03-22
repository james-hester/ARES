package ares.ui;

import java.awt.Color;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;


public class DisplayPanel extends JPanel
{

	ArrayList<PipelineElement> pipelineElementList;
	ArrayList<InstructionLabel> instructionList;
	Timer t1;
	
	private int componentDelta = 0;
	
	public DisplayPanel()
	{
		super();
		setBackground(Color.BLACK);
		
		pipelineElementList = new ArrayList<>(6);
		instructionList = new ArrayList<>(6);
		
		instructionList.add(new InstructionLabel("", 0, 46));
		instructionList.add(new InstructionLabel("", 0, 120));
		instructionList.add(new InstructionLabel("", 12, 194));
		instructionList.add(new InstructionLabel("", 0, 268));
		instructionList.add(new InstructionLabel("", 0, 342));
		instructionList.add(new InstructionLabel("", 0, 416));
		
		pipelineElementList.add(new PipelineElement(200, -148));
		pipelineElementList.add(new PipelineElement(280, -74));
		pipelineElementList.add(new PipelineElement(360, 0));
		pipelineElementList.add(new PipelineElement(440, 74));
		pipelineElementList.add(new PipelineElement(520, 148));
		pipelineElementList.add(new PipelineElement(600, 222));

		//this.setToolTipText("");
		//ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		
	}
	
	public void moveComponents()
	{
		componentDelta++;
		double dY = -((double)PipelineElement.HEIGHT / (double)PipelineElement.WIDTH);
		for(PipelineElement el : pipelineElementList)
		{
			el.move(-1.0, dY);
		}
		for(int i = 0; i < 2; i++)
		{
			instructionList.get(5 - i).move(0, -1.0);
			instructionList.get(i).move(0, -1.0);
		}
		instructionList.get(2).move(-0.15, dY);
		instructionList.get(3).move(0.15, dY);
		repaint();
	}
	
	public PipelineElement getNextElement()
	{
		return pipelineElementList.get(3);
	}
	
	public void propagateStages()
	{
		for(int i = 0; i < 3; i++)
		{
			if (pipelineElementList.get(3).getStage(i))
			{
				pipelineElementList.get(4).setStage(i + 1, true);
				pipelineElementList.get(5).setStage(i + 2, true);
			}
		}
	}
	
	public void addInstruction(String theInstruction)
	{
		instructionList.get(5).setText(theInstruction);
	}
	
	public boolean animationComplete()
	{
		return (componentDelta >= PipelineElement.WIDTH);
	}
	
	public void resetAnimation()
	{
		componentDelta = 0;
		for(int i = 0; i < 5; i++)
		{
			instructionList.set(i, instructionList.get(i + 1));
			pipelineElementList.set(i, pipelineElementList.get(i + 1));
		}
		instructionList.set(5, new InstructionLabel("", 0, 416));
		pipelineElementList.set(5, new PipelineElement(600, 222));
			
	}
	
	@Override
	public String getToolTipText(MouseEvent evt)
	{
		return "";
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		
		for(PipelineElement comp : pipelineElementList)
		{
			comp.render(g2);
		}
		
		g.setColor(Color.GRAY);
		g.fillRect(0, 0, this.getWidth() / 3 - 0, this.getHeight());
		//g.drawLine(0, 74, 600, 74);
		//g.drawLine(0, 148, 600, 148);
		//g.drawLine(0, 222, 600, 222);
		//g.drawLine(0, 296, 600, 296);

		for(InstructionLabel il : instructionList)
		{
			il.render(g2);
		}

	}



}