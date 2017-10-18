package com.ligius.voyagerdecoder.audio.listeners;

import java.awt.Color;
import java.util.Set;

import javax.sound.sampled.AudioFormat;

import com.ligius.voyagerdecoder.audio.EdgeProcessor;
import com.ligius.voyagerdecoder.audio.DataPacket;
import com.ligius.voyagerdecoder.ui.BufferedPanelListener;

/**
 * Creates a waveform visualization of the incoming packet data.
 */
public class WavePlotter extends BufferedPanelListener {

	final EdgeProcessor edgeProcessor;
	final AudioFormat audioFormat;
	final int channel;

	final static boolean SHOW_PEAKS = false;
	final static boolean SHOW_PROCESSED_EDGES = true;
	final static boolean SHOW_CRUDE_EDGES = true;

	public WavePlotter(final AudioFormat audioFormat, final int channel) {
		this.audioFormat = audioFormat;
		this.channel = channel;
		this.edgeProcessor = new EdgeProcessor(audioFormat.getSampleRate());
	}

	@Override
	public void initialize() {
		super.initialize();
		// double verticalScale = (double) 500 / super.getHeight();
		// TODO: figure out how to scale this properly
		// super.initializeScaled((int) (super.getWidth() * verticalScale), (int)
		// (super.getHeight() * verticalScale));
	}

	int avg = 0;

	@Override
	public void notifyPacket(final DataPacket packet) {
		final int[] sampleDataArray = packet.getSampleDataForChannel(this.channel);
		final int totalFramesRead = packet.getTotalFramesRead();

		final int width = this.getWidth();
		final int height = this.getHeight();

		// TODO: should take sample rate into account
		final int SAMPLES_PER_X_TICK = 90000 / getWidth();

		Color c1 = Color.BLUE;

		// this.graphics.clearRect(0, 0, width, height);
		final int offsetY = this.getHeight() / 2;

		// TODO: should take bits per frame into account
		final float multiplierY = 1f / 64;

		//this.getGraphics().setColor(Color.WHITE);
		this.clearImage();

		int oldSampleData = sampleDataArray[0];

		// int lastAvg = 0;

		final float multiplierX = (float) width / sampleDataArray.length;
		this.getGraphics().drawLine(0, offsetY, width, offsetY);

		final boolean[] peaks;
		if (SHOW_PEAKS) {
			peaks = this.edgeProcessor.processSamplesForEdges(sampleDataArray, totalFramesRead);
		}

		final Set<Integer> edges;
		if (SHOW_PROCESSED_EDGES) {
			edges = this.edgeProcessor.processSamplesForEdges2(sampleDataArray, totalFramesRead);
		}

		for (int i = 1; i < sampleDataArray.length; i++) {
			final int xStart, xEnd;

			xStart = (int) ((i - 1) * multiplierX);
			xEnd = (int) (i * multiplierX);

			int currentSampleData = sampleDataArray[i];

			double seconds = (double) (totalFramesRead + i) / this.audioFormat.getSampleRate();
			edgeProcessor.feedSample(currentSampleData, totalFramesRead + i, seconds);

			this.getGraphics().setColor(c1);
			this.getGraphics().drawLine(xStart, height - (int) (oldSampleData * multiplierY + offsetY), xEnd, height - (int) (currentSampleData * multiplierY + offsetY));

			// detect edges
			{
				if (SHOW_CRUDE_EDGES) {
					if (edgeProcessor.isEdgeDetected() && edgeProcessor.isFallingEdge()) {
						this.getGraphics().setColor(c1);
						this.getGraphics().drawRect(xStart, 15, 4, 4);
					}
				}

				if (SHOW_PEAKS) {
					if (peaks[i]) {
						this.getGraphics().setColor(Color.BLUE);
						this.getGraphics().drawOval(xStart, 60, 4, 4);
					}
				}

				if (SHOW_PROCESSED_EDGES) {
					if (edges.contains(i)) {
						this.getGraphics().setColor(Color.BLACK);
						this.getGraphics().drawRect(xStart, 64, 5, 5);
					}
				}
			}

			oldSampleData = currentSampleData;

			// draw X axis
			if (i % SAMPLES_PER_X_TICK == 0) {
				this.getGraphics().setColor(Color.BLACK);
				this.getGraphics().drawString(((double) (int) (seconds * 1000) / 1000) + "s", xStart, 10);
			}
		}

		// draw Y axis
		for (int j = 0; j < height; j += 50) {
			this.getGraphics().setColor(Color.BLACK);
			this.getGraphics().drawString("" + (offsetY - j) / multiplierY, 10, j);
		}

		super.notifyPacket(packet);
	}

	@Override
	public void stop() {
		// do nothing
	}
}
