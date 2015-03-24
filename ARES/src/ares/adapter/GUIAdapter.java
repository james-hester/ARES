package ares.adapter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Scanner;

import javax.swing.*;

import ares.core.Memory;
import ares.core.Simulator;
import ares.ui.DisplayPanel;
import ares.ui.PipelineElement;

public class GUIAdapter extends JFrame
{

	public static final int ANIMATION_DEFAULT_SPEED = 5;
	
	public static final double SIMULATION_DEFAULT_SPEED_HZ = 1.0;
	public static final double SIMULATION_MIN_SPEED_HZ = 0.1;
	public static final double SIMULATION_MAX_SPEED_HZ = 10.0;
	public static final double SIMULATION_SPEED_STEP_SIZE = 0.1;
	
	
	private Timer mainAnimationTimer, stallAnimationTimer, runSpeedTimer, repaintTimer;
	private DisplayPanel pipelineDisplay;
	
	private Simulator simulator;
	private Memory memory;
	
	JButton stepButton, runButton;
	JLabel fileLabel = new JLabel("File loaded: <none>");
	JSpinner runSpeed;
	
	public GUIAdapter()
	{
		super();
		
		pipelineDisplay = new DisplayPanel();
		pipelineDisplay.setPreferredSize(new Dimension(600, 370));
		
		stepButton = new JButton("Step");
		stepButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (mainAnimationTimer.isRunning() || runSpeedTimer.isRunning())
					return;
				simulateCycle();
			}
		
		});
		stepButton.setEnabled(false);
		
		runButton = new JButton("Run");
		runButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e)
			{
				runButtonPressed();
			}
		
		});
		runButton.setEnabled(false);
		
		runSpeed = new JSpinner(new SpinnerNumberModel(
				SIMULATION_DEFAULT_SPEED_HZ, 
				SIMULATION_MIN_SPEED_HZ, 
				SIMULATION_MAX_SPEED_HZ, 
				SIMULATION_SPEED_STEP_SIZE));
		
		JMenuBar menuBar = new JMenuBar();
			JMenu file = new JMenu("File");
				JMenuItem openBinFile = new JMenuItem("Load Binary File...");
				openBinFile.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e) {
						loadBinaryFile();
					}
				});
				file.add(openBinFile);
				JMenuItem openAsmFile = new JMenuItem("Assemble...");
				openAsmFile.setEnabled(false);
				file.add(openAsmFile);
			menuBar.add(file);
		setJMenuBar(menuBar);
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		buttonPane.add(fileLabel);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(stepButton);
		buttonPane.add(runButton);
		buttonPane.add(new JLabel("Speed: "));
		buttonPane.add(runSpeed);
		buttonPane.add(new JLabel(" cycles per second"));
		
		this.setResizable(false);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		mainAnimationTimer = new Timer(5, new AnimationActionListener());
		
		runSpeedTimer = new Timer(Integer.MAX_VALUE, new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
					simulateCycle();
			}
			
		});
	
		stallAnimationTimer = new Timer(20, new StallAnimationActionListener());
		
		repaintTimer = new Timer(1000 / 60, new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
					pipelineDisplay.repaint();
			}	
		});
		
		this.getContentPane().add(pipelineDisplay, BorderLayout.CENTER);
		this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
		pack();
		
		setFocusable(true);
		setVisible(true);
	}
	
	public void runButtonPressed()
	{
		if (runSpeedTimer.isRunning())
		{
			runSpeedTimer.stop();
			runButton.setText("Run");
		}
		else
		{
			runButton.setText("Pause");
			runSpeedTimer.setDelay( (int)(( 1000.0 / (Double) runSpeed.getValue()) ) );
			runSpeedTimer.setInitialDelay(runSpeedTimer.getDelay());
			mainAnimationTimer.setDelay(Math.min(5, runSpeedTimer.getDelay() / 80));
			runSpeedTimer.start();
		}
	}
	
	public void loadBinaryFile()
	{
		
		JFileChooser openDlg = new JFileChooser();
		int result = openDlg.showOpenDialog(this);
		
		if (result == JFileChooser.APPROVE_OPTION)
		{
			File theFile = openDlg.getSelectedFile();
			loadBinaryFile(theFile);
			fileLabel.setText("File loaded: " + theFile.getName());
			stepButton.setEnabled(true);
			runButton.setEnabled(true);
		}
	}
	
	private void loadBinaryFile(File theFile)
	{
		memory = new Memory();
		try
		{
			Scanner readFile = new Scanner(theFile);
			int i = 0;
			while (readFile.hasNextLine())
			{ 
				int a = Integer.parseUnsignedInt(readFile.nextLine(), 16);
				memory.storeWord(Memory.TEXT_SEGMENT_START_ADDRESS + i, a);
				i += 4;
			}
			memory.setMaxInstAddr(Memory.TEXT_SEGMENT_START_ADDRESS + i);
			
			readFile.close();
		} 
		catch (Exception e)  
		{
			JOptionPane.showMessageDialog(this, "An error occurred while loading the specified\nfile. Please ensure the file is in MARS'"
					+ " \"Hexadecimal Text\" format\n(with one hexadecimal number per line) and try again.",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		simulator = new Simulator(memory);
	}
	
	public void simulateCycle()
	{
		/*
		 * First, actually do the step.
		 * Then, do the animation.
		 */
		
		if (simulator.hasNextInstruction())
		{
			simulator.step();
	
			pipelineDisplay.addInstruction(simulator.getInstructionFetchedName());
			
			if (simulator.getInstructionFetchedName().equals("nop"))
			{
				pipelineDisplay.getNextElement().setHole(0, true);
				pipelineDisplay.getNextElement().setStage(0, false);
			}
			else for(int i = 0; i < 5; i++)
				pipelineDisplay.getNextElement().setStage(i, simulator.getStagesOccurred().get(i));	
			
			if ( simulator.normalStallOccurred() )
			{
				pipelineDisplay.getCurrentInstruction().setText("<stall>");
				pipelineDisplay.getCurrentElement().setHole(0, true);
				pipelineDisplay.getCurrentElement().setStage(0, false);
				pipelineDisplay.getNextElement().setHole(1, true);
				pipelineDisplay.getNextElement().setStage(1, false);	
			}
			
			pipelineDisplay.propagateStages();
		}
		else
		{
			if (pipelineDisplay.isEmpty())
			{
				fileLabel.setText("File loaded: <none>");
				stepButton.setEnabled(false);
				runButton.setEnabled(false);
				runSpeedTimer.stop();
			}
			pipelineDisplay.addInstruction("");
			pipelineDisplay.getNextElement().setHole(0, true);
			pipelineDisplay.getNextElement().setStage(0, false);
			pipelineDisplay.propagateStages();
		}
		mainAnimationTimer.start();
		repaintTimer.start();
	}
	
	class StallAnimationActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			
		}
		
	}
	
	class AnimationActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			pipelineDisplay.moveComponents();
			if (pipelineDisplay.animationComplete())
			{
				pipelineDisplay.resetAnimation();
				pipelineDisplay.repaint();
				mainAnimationTimer.stop();
				repaintTimer.stop();
			}
		}
	}
}
