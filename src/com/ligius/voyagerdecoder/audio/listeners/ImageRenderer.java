package com.ligius.voyagerdecoder.audio.listeners;

import java.awt.Color;
import java.util.Set;

import javax.sound.sampled.AudioFormat;

import com.ligius.voyagerdecoder.audio.EdgeProcessor;
import com.ligius.voyagerdecoder.audio.DataPacket;
import com.ligius.voyagerdecoder.ui.BufferedPanelListener;

/**
 * Creates a decoded image representation from the incoming packet data.
 * 
 * This is currently under active experimentation, dirty code follows.
 */
public class ImageRenderer extends BufferedPanelListener {

	final EdgeProcessor edgeProcessor;

	final AudioFormat audioFormat;
	final int channel;

	int processedSamples = 0;
	boolean firstFrameStarted = false;
	double firstEdgeSeconds = -1;

	final int SCANLINE_SAMPLES;
	double SCANLINE_DURATION = 0.0083256; // TODO: adjust in realtime

	final int FRAME_SCANLINES = 512; // ???
	final int SCANLINE_PIXELS = 450; // ???

	int yPos = -1;
	int lastXPos = 0;
	int lastFallingEdgeSample = 0;
	int lastSyncEdgeSample = 0;
	int frameCount = 0;

	public ImageRenderer(final AudioFormat audioFormat, final int channel) {
		this.audioFormat = audioFormat;
		this.channel = channel;

		this.edgeProcessor = new EdgeProcessor(audioFormat.getSampleRate());
		this.SCANLINE_SAMPLES = (int) (this.audioFormat.getSampleRate() * .0083);

	}

	@Override
	public void initialize() {
		setFixedRatio(true);

		double verticalScale = (double) SCANLINE_PIXELS / super.getHeight();
		// TODO: figure out how to scale this properly
		super.initializeScaled((int) ((double) super.getWidth() * verticalScale), (int) ((double) super.getHeight() * verticalScale));

		setImageAdjustmentsEnabled(true);
	}

	private void saveCurrentFrame() {
		saveGraphicsToFile("ch" + this.channel + "_frame" + this.frameCount + ".png", this.yPos);
	}

	boolean frameStartingPreviously = false;

	@Override
	public void notifyPacket(DataPacket packet) {
		final int[] sampleDataArray = packet.getSampleDataForChannel(this.channel);
		final int totalFramesRead = packet.getTotalFramesRead();
		final int height = this.getBufferedHeight();

		int globalMin = 0;
		for (int i = 1; i < sampleDataArray.length; i++) {
			globalMin = Math.min(globalMin, sampleDataArray[i]);
		}

		processedSamples++;

		boolean[] peaks = this.edgeProcessor.processSamplesForEdges(sampleDataArray, totalFramesRead);
		int startOfFrame = this.edgeProcessor.getStartOfFrame(peaks);
		Set<Integer> edges = this.edgeProcessor.processSamplesForEdges2(sampleDataArray, totalFramesRead);
		// if (edges.size() > 3 || edges.size() < 2) {
		// System.out.println("on frame " + frameCount + " edges:" + edges.size());
		// }

		{
			if (startOfFrame != -1 && startOfFrame > sampleDataArray.length - SCANLINE_SAMPLES / 2) {
				// the start of frame is at the right edge array, remember that
				frameStartingPreviously = true;
			} else if (startOfFrame != -1) {
				frameStartingPreviously = false;
				double seconds = (double) (totalFramesRead + startOfFrame) / this.audioFormat.getSampleRate();
				System.out.println("clean start of frame at " + seconds + "s channel " + channel);
			}
		}

		for (int i = 0; i < sampleDataArray.length; i++) {
			processedSamples++;
			double seconds = (double) (totalFramesRead + i) / this.audioFormat.getSampleRate();

			int currentSampleData = sampleDataArray[i];

			edgeProcessor.feedSample(currentSampleData, totalFramesRead + i, seconds);

			if (peaks[i]) {

				if ((startOfFrame == i && !frameStartingPreviously) || (startOfFrame == -1 && frameStartingPreviously)) {

					if (yPos >= FRAME_SCANLINES * 6 / 4) {
						System.out.println("! exceeded frame duration at " + seconds + "s " + yPos + " lines channel " + channel);
					}
					if ((yPos <= 2 || yPos >= FRAME_SCANLINES)) {
						frameStartingPreviously = false;
						if (yPos >= FRAME_SCANLINES) {
							frameCount++;
							System.out.println("New frame " + frameCount + " at " + seconds + "s channel " + channel);
							saveCurrentFrame();
							// getGraphics().setColor(Color.WHITE);
							clearImage();
						}

						firstFrameStarted = true;
						firstEdgeSeconds = seconds;
						yPos = 0;

						lastFallingEdgeSample = processedSamples;

						continue;
					} else {
						// avoid triggering mid-frame
						frameStartingPreviously = false;
					}
				}
				lastFallingEdgeSample = processedSamples;
			}

			if (firstFrameStarted) {

				double scanlinesSoFar = Math.abs((seconds - firstEdgeSeconds) / SCANLINE_DURATION);
				double scanlineFraction = scanlinesSoFar - Math.floor(scanlinesSoFar);
				// TODO: a reliable way to detect start of scanline, perhaps distance to
				// peak

				// "very white" means start of border, usually at 98.5% or 95.25%
				final boolean correctForSkew = true;
				// if (correctForSkew && grayScaleValue > 255 && (scanlineFraction > 0.9
				// || yPos < 3)) {
				// // this needs to be satisfied: frameStartTime + (scanLines +
				// // scanLinePercentage) * scanLineTime = currentTime
				// double oddPercentage = (double) (SCANLINE_SAMPLES - 19) /
				// SCANLINE_SAMPLES;
				// double evenPercentage = (double) (SCANLINE_SAMPLES - 6) /
				// SCANLINE_SAMPLES;
				// double predictedLinePercentage = yPos % 2 == 0 ? oddPercentage :
				// evenPercentage;
				// double driftSeconds = firstEdgeSeconds + ((double)
				// predictedLinePercentage + yPos) * (SCANLINE_DURATION) - seconds;
				//
				// if (driftSeconds > SCANLINE_DURATION) {
				// System.out.println("too large " + driftSeconds);
				// }
				//
				// // firstEdgeSeconds += driftSeconds;
				// // firstEdgeSeconds += Math.signum(driftSeconds)*.000001;
				// // SCANLINE_DURATION -= Math.signum(driftSeconds)*.000000001;
				//
				// scanlinesSoFar = Math.abs((seconds - firstEdgeSeconds) /
				// SCANLINE_DURATION);
				// scanlineFraction = scanlinesSoFar - Math.floor(scanlinesSoFar);
				// }

				if (correctForSkew && (scanlineFraction > 0.95)) {
					// TODO: perhaps end can be skipped over, check boundaries
					double expectedScanlines = Math.floor(sampleDataArray.length / SCANLINE_SAMPLES);
					if (edges.size() >= expectedScanlines && edges.size() <= expectedScanlines + 1) {
						int SAMPLES_TO_LOOK_AROUND = SCANLINE_SAMPLES - 1;
						int closestEdgePos = -1;

						// look around for a strong edge
						for (int j = 0; j < SAMPLES_TO_LOOK_AROUND / 2; j++) {
							if (edges.contains(i + j)) {
								closestEdgePos = i + j;
								break;
							} else if (edges.contains(i - j)) {
								closestEdgePos = i - j;
								break;
							}
						}

						if (closestEdgePos != -1) {
							double scanlineStartTime = (closestEdgePos + totalFramesRead) / audioFormat.getSampleRate();
							double driftSeconds = scanlineStartTime - seconds;
							double correctionFactor = (scanlinesSoFar < FRAME_SCANLINES ? Math.pow(0.5, scanlinesSoFar / FRAME_SCANLINES) : 1) / 100;
							firstEdgeSeconds += driftSeconds * correctionFactor;
						}
					}

					scanlinesSoFar = Math.abs((seconds - firstEdgeSeconds) / SCANLINE_DURATION);
					scanlineFraction = scanlinesSoFar - Math.floor(scanlinesSoFar);
				}

				boolean isScanLineStarting = scanlinesSoFar > yPos;
				if (isScanLineStarting) {
					// start of new line
					// yPos++;
					yPos = (int) Math.floor(scanlinesSoFar);
					lastXPos = 0;

				}

				int xPos = (int) ((float) height * scanlineFraction);

				// if (xPos > 60 && xPos < 500 && yPos < 500) {
				// if (negativeGrayScaleValue > 255 || negativeGrayScaleValue < 0)
				// System.out.println(xPos + "," + yPos + " : " +
				// negativeGrayScaleValue);
				// }
				// int negativeGrayScaleValue = (currentSampleData / 80) + 130; //
				// 11.10.2017
				// int negativeGrayScaleValue = (currentSampleData / 45) + 120; //
				// 11.10.2017H
				int negativeGrayScaleValue;
				// if (yPos % 10 > 5){
				// negativeGrayScaleValue = (int) ((currentSampleData * Math.pow(xPos,
				// -.1)/5) + 120);
				// }else{
				negativeGrayScaleValue = (currentSampleData / 45) + 120;
				// }
				negativeGrayScaleValue = 255 - Math.max(0, Math.min(255, negativeGrayScaleValue));

				{
					final Color color = new Color(negativeGrayScaleValue, negativeGrayScaleValue, negativeGrayScaleValue);
					getGraphics().setColor(color);
					getGraphics().drawRect(yPos, xPos, 1, xPos - lastXPos);
				}

				lastXPos = xPos;
			}

		}

		super.notifyPacket(packet);

	}

	@Override
	public void stop() {
		frameCount++;
		saveCurrentFrame();
	}
}
