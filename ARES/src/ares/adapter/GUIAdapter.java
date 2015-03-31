package ares.adapter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import ares.core.Memory;
import ares.core.Simulator;
import ares.ui.AnimatedPipelineDisplay;

public class GUIAdapter extends JFrame
{
	private static final long serialVersionUID = 1L;
	public static final double SIMULATION_DEFAULT_SPEED_HZ = 1.0;
	public static final double SIMULATION_MIN_SPEED_HZ = 0.1;
	public static final double SIMULATION_MAX_SPEED_HZ = 10.0;
	public static final double SIMULATION_SPEED_STEP_SIZE = 0.1;

	private Timer runSpeedTimer;
	private AnimatedPipelineDisplay pipelineDisplay;
	
	private Simulator simulator;
	private Memory memory;
	
	JButton stepButton, runButton;
	JLabel fileLabel = new JLabel("File loaded: <none>");
	JSpinner runSpeed;
	
	public GUIAdapter()
	{
		super();
		
		pipelineDisplay = new AnimatedPipelineDisplay();
		pipelineDisplay.setPreferredSize(new Dimension(600, 370));
		
		stepButton = new JButton("Step");
		stepButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (pipelineDisplay.isAnimationOngoing() || runSpeedTimer.isRunning())
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

		runSpeedTimer = new Timer(Integer.MAX_VALUE, new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pipelineDisplay.isAnimationOngoing())
					return;
				simulateCycle();
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
			
			pipelineDisplay.getNextElement().setForwardingOccurred(simulator.getForwardingOccurred());
			
			if (simulator.getStagesOccurred().get(0))
			{
				if (simulator.getInstructionFetchedName().equals("nop"))
				{
					pipelineDisplay.getNextElement().setHole(0, true);
					pipelineDisplay.getNextElement().setStage(0, false);
				}
				else
				{
					pipelineDisplay.getNextElement().setStage(0, true);
					pipelineDisplay.getNextElement().setIFData(simulator.getInstructionFetchedName());
				}
				pipelineDisplay.addInstruction(simulator.getInstructionFetched());
			}
			if (simulator.getStagesOccurred().get(1))
			{
				pipelineDisplay.getNextElement().setStage(1, true);
				String[] idData = simulator.getIDData();
				pipelineDisplay.getNextElement().setIDData(idData[0], idData[1]);
			}
			if (simulator.getStagesOccurred().get(2))
			{
				pipelineDisplay.getNextElement().setStage(2, true);
				pipelineDisplay.getNextElement().setEXData(simulator.getEXData());
			}
			if (simulator.getStagesOccurred().get(3))
			{
				pipelineDisplay.getNextElement().setStage(3, true);
				pipelineDisplay.getNextElement().setMEMData(simulator.getMEMOperation(), simulator.getMEMAddress());
			}
			if (simulator.getStagesOccurred().get(4))
			{
				pipelineDisplay.getNextElement().setStage(4, true);
				pipelineDisplay.getNextElement().setWBData(simulator.getWBData());
			}
			
			if ( simulator.branchOccurred() )
			{
				pipelineDisplay.insertBranch();
			}
			if ( simulator.normalStallOccurred() )
			{
				pipelineDisplay.insertStall();
			}

		}
		else
		{
			if (pipelineDisplay.isEmpty())
			{
				fileLabel.setText("File loaded: <none>");
				stepButton.setEnabled(false);
				runButton.setEnabled(false);
				runButton.setText("Run");
				runSpeedTimer.stop();
				
			}
			pipelineDisplay.addInstruction("");
			pipelineDisplay.getNextElement().setHole(0, true);
			pipelineDisplay.getNextElement().setStage(0, false);
		}
		
		pipelineDisplay.startAnimation();
	}
	
}
