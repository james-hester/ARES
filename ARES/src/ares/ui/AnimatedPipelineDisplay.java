package ares.ui;

import static ares.ui.Colors.BACKGROUND;
import static ares.ui.Colors.TEXT_FIELD;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;

import javax.swing.JPanel;
import javax.swing.Timer;


public class AnimatedPipelineDisplay extends JPanel
{	

	private static final long serialVersionUID = 1L;
	static final double FRAMES_PER_SECOND = 60;
	static final double ANIMATION_TOTAL_LENGTH_MS = 400;
	static final double STALL_ANIMATION_TOTAL_LENGTH_MS = 400;
	static final long STALL_ANIMATION_PAUSE_MS = 500;
	
	ArrayList<PipelineElement> pipelineElementList;
	ArrayList<InstructionLabel> instructionList;
		
	private Timer stallAnimationTimer = new Timer((int)(1000.0 / FRAMES_PER_SECOND), new StallAnimationActionListener());
	private Timer repaintTimer = new Timer((int)(1000.0 / FRAMES_PER_SECOND), new AnimationActionListener());
	
	private boolean showStallAnimation = false;
	private double stallAnimDisplacement = 1.0;
	private BufferedImage stallAnimImg;
	
	
	public AnimatedPipelineDisplay()
	{
		super();
		setBackground(BACKGROUND.getColor());
		
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
		
		for(PipelineElement e : pipelineElementList)
			e.setStage(0, false);
			
		pipelineElementList.add(new PipelineElement(440, 74));
		pipelineElementList.add(new PipelineElement(520, 148));
		pipelineElementList.add(new PipelineElement(600, 222));
		
	}
		
	public void startAnimation()
	{
		propagateStages();
		if (showStallAnimation)
		{
			stallAnimationTimer.start();
		}
		repaintTimer.start();
	}
	
	public void propagateStages()
	{
		for(int j = 4; j < 6; j++)
		{
			for(int i = 0; i < 4; i++)
				pipelineElementList.get(j).setStage(i + 1, pipelineElementList.get(j - 1).getStage(i));			
			
			for(int i = 4; i > 0; i--)
				pipelineElementList.get(j).setHole(i, pipelineElementList.get(j - 1).getHole(i - 1));
			
			pipelineElementList.get(j).setHole(0, false);
		}
	}
	
	public boolean isAnimationOngoing()
	{
		return repaintTimer.isRunning() || stallAnimationTimer.isRunning();
	}
	
	public void insertBranch()
	{
		pipelineElementList.get(3).setHole(2, true);
		pipelineElementList.get(3).setStage(2, false);
		repaint();
		
	}
	
	public void insertStall()
	{
		pipelineElementList.get(3).setForwardingOccurred(new BitSet(6));
		pipelineElementList.get(3).setIDData("","");
		
		stallAnimImg = (BufferedImage) this.createImage(this.getWidth(), this.getHeight());
		this.paintComponent(stallAnimImg.getGraphics());
		stallAnimImg = stallAnimImg.getSubimage(0, this.getHeight() - PipelineElement.HEIGHT, this.getWidth(), PipelineElement.HEIGHT);
		
		getCurrentInstruction().setText("<stall>");
		getCurrentElement().setHole(0, true);
		getCurrentElement().setStage(0, false);
		getNextElement().setHole(1, true);
		getNextElement().setStage(1, false);

		showStallAnimation = true;
	}
	
	public PipelineElement getNextElement()
	{
		return pipelineElementList.get(3);
	}
	
	public PipelineElement getCurrentElement()
	{
		return pipelineElementList.get(2);
	}
	
	public InstructionLabel getCurrentInstruction()
	{
		return instructionList.get(4);
	}
	
	public boolean isEmpty()
	{
		for(InstructionLabel i : instructionList)
		{
			if ( ! i.getText().equals(""))
				return false;
		}
		return true;
	}
		
	public void addInstruction(String theInstruction)
	{
		instructionList.get(5).setText(theInstruction);
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
		
		g.setColor(TEXT_FIELD.getColor());
		g.fillRect(0, 0, this.getWidth() / 3, this.getHeight());

		for(InstructionLabel il : instructionList)
		{
			il.render(g2);
		}
		
		if(showStallAnimation)
		{
			g2.drawImage(stallAnimImg, 0, this.getHeight() - (int)(PipelineElement.HEIGHT * (stallAnimDisplacement)), null);
		}

	}

	class StallAnimationActionListener implements ActionListener
	{
		private double stepSize = -1000.0 / (STALL_ANIMATION_TOTAL_LENGTH_MS * (double)FRAMES_PER_SECOND);
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			stallAnimDisplacement += stepSize;			
			repaint();
			
			if (stallAnimDisplacement < -0.01)
			{
				stallAnimationTimer.stop();
				
				repaint();
				stallAnimDisplacement = 1.0;
				showStallAnimation = false;
				try
				{
					Thread.sleep(STALL_ANIMATION_PAUSE_MS);
				} catch (InterruptedException ie)
				{
					return;
				}
				
			}
		}
		
	}
	
	class AnimationActionListener implements ActionListener
	{
		private double componentDelta = 0;
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (showStallAnimation)
			{
				return;
			}
			
			double stepSize = -1.0 * ((double)PipelineElement.WIDTH / (ANIMATION_TOTAL_LENGTH_MS / 1000.0)) / (double)FRAMES_PER_SECOND;
			double amountToMove = Math.max((-1.0 * (double)PipelineElement.WIDTH + (double)componentDelta), stepSize);
			
			componentDelta -= amountToMove;
			double dY = ((double)PipelineElement.HEIGHT * amountToMove / (double)PipelineElement.WIDTH);
			for(PipelineElement el : pipelineElementList)
			{
				el.move(amountToMove, dY);
			}
			for(int i = 0; i < 6; i++)
			{
				instructionList.get(i).move(0, amountToMove);
			}
			
			repaint();
			if (componentDelta >= PipelineElement.WIDTH)
			{
				componentDelta = 0;
				pipelineElementList.get(2).toggleHighlighted();
				pipelineElementList.get(3).toggleHighlighted();
				for(int i = 0; i < 5; i++)
				{
					instructionList.set(i, instructionList.get(i + 1));
					pipelineElementList.set(i, pipelineElementList.get(i + 1));
				}
				instructionList.set(5, new InstructionLabel("", 0, 416));
				pipelineElementList.set(5, new PipelineElement(600, 222));
				repaintTimer.stop();
			}
		}
	}

}