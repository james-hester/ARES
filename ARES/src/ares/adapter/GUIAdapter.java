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

public class GUIAdapter extends JFrame implements ActionListener
{

	private Timer animationTimer, runSpeedTimer;
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
				if (animationTimer.isRunning() || runSpeedTimer.isRunning())
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
		
		runSpeed = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 3.0, 0.1));
		
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
		
		animationTimer = new Timer(5, this);
		runSpeedTimer = new Timer(Integer.MAX_VALUE, new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("here");
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
			System.err.println((int)(((Double) runSpeed.getValue()) * 1000) );
			runSpeedTimer.setDelay( (int)(((Double) runSpeed.getValue()) * 1000) );
			runSpeedTimer.setInitialDelay(runSpeedTimer.getDelay());
			runSpeedTimer.start();
		}
	}
	
	public void loadBinaryFile()
	{
		memory = new Memory();
        
		JFileChooser openDlg = new JFileChooser();
		int result = openDlg.showOpenDialog(this);
		
		if (result == JFileChooser.APPROVE_OPTION)
		{
			File theFile = openDlg.getSelectedFile();
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
			
			fileLabel.setText("File loaded: " + theFile.getName());
			stepButton.setEnabled(true);
			runButton.setEnabled(true);
		}
	}
	
	public void simulateCycle()
	{
		/*
		 * First, actually do the step.
		 * Then, do the animation.
		 */
		
		simulator.step();
		pipelineDisplay.addInstruction(simulator.getInstructionFetchedName());
		
		for(int i = 0; i < 5; i++)
			pipelineDisplay.getNextElement().setStage(i, simulator.getStagesOccurred().get(i));
		
		pipelineDisplay.propagateStages();
		
		animationTimer.start();
	}
	
	
	//TODO this really should be its own class, anonymous or otherwise (probably otherwise)
	@Override
	public void actionPerformed(ActionEvent e)
	{
		pipelineDisplay.moveComponents();
		if (pipelineDisplay.animationComplete())
		{
			pipelineDisplay.resetAnimation();
			animationTimer.stop();
		}
	}
}
