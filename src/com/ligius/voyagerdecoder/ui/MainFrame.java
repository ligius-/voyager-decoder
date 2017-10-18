package com.ligius.voyagerdecoder.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.ligius.voyagerdecoder.util.ProgramOptions;

/**
 * The main UI window
 * 
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	int channels = 0;

	JPanel channelContainer;

	public MainFrame(int channels) {

		this.setLayout(new BorderLayout());
		channelContainer = new JPanel();
		this.getContentPane().add(channelContainer, BorderLayout.CENTER);

		this.getContentPane().add(new OptionsPanel(), BorderLayout.SOUTH);

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.setVisible(true);
	}

	/**
	 * Adds a new [composite] panel to the window, for one audio channel. The
	 * display panel will auto-detect the channel number.
	 */
	public void addChannelDisplayPanel(ChannelDisplayPanel panel) {
		channels++;
		final Dimension d = new Dimension(ProgramOptions.getInstance().getWindowWidth(), ProgramOptions.getInstance().getWindowHeightPerChannel() * channels);

		channelContainer.setLayout(new GridLayout(channels, 1));
		channelContainer.setPreferredSize(d);
		channelContainer.add(panel);
		this.pack();
	}

}
