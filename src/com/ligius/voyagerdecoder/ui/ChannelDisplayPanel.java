package com.ligius.voyagerdecoder.ui;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.swing.JPanel;

import com.ligius.voyagerdecoder.audio.listeners.WavePlotter;
import com.ligius.voyagerdecoder.audio.listeners.ImageRenderer;
import com.ligius.voyagerdecoder.audio.listeners.PacketListener;

/**
 * This class extends a JPanel and is used to display/visualize one audio
 * channel.
 * 
 * Currently the layout is split, with a waveform visualization on the left and
 * the image rendering on the right.
 * 
 */
@SuppressWarnings("serial")
public class ChannelDisplayPanel extends JPanel {

	private final List<PacketListener> packetListeners = new ArrayList<PacketListener>();

	public ChannelDisplayPanel(final AudioFormat audioFormat, final int channel) {
		{
			this.setLayout(new GridLayout(1, 2));

			WavePlotter sampleDataPlotter = new WavePlotter(audioFormat, channel);
			this.add(sampleDataPlotter.getComponent());
			getPacketListeners().add(sampleDataPlotter);

			ImageRenderer sampleImagePlotter = new ImageRenderer(audioFormat, channel);
			this.add(sampleImagePlotter.getComponent());
			getPacketListeners().add(sampleImagePlotter);
		}
	}

	/**
	 * Retrieves the [packet] listeners of the internal/wrapped components. These
	 * NEED to be added as listeners in an extra step to avoid parameter
	 * modification (side effects).
	 * 
	 */
	public List<PacketListener> getPacketListeners() {
		return packetListeners;
	}

	/**
	 * Returns the Component so it can be added to a container
	 */
	public Component getComponent() {
		return this;
	}
}
