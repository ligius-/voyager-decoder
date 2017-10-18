package com.ligius.voyagerdecoder.audio;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds some signal processing algorithms (experiments) until a better solution
 * is found.
 * 
 * This is currently under active experimentation, dirty code follows.
 */

public class EdgeProcessor {

	int avg, lastAvg, minY, maxY, delta = 0;
	int currentSampleData, oldSampleData = 0;
	boolean edgeDetected, fallingEdge;

	final int SAMPLE_RATE;

	final int SYNC_FRAME_START_SAMPLES;
	final int SYNC_FRAME_VARIANCE_SAMPLES;
	double burstAverageDuration;

	public EdgeProcessor(final float SAMPLE_RATE) {
		this.SAMPLE_RATE = (int) SAMPLE_RATE;

		this.SYNC_FRAME_START_SAMPLES = (int) ((double) 25.5 / 48000 * this.SAMPLE_RATE);
		this.SYNC_FRAME_VARIANCE_SAMPLES = (int) ((double) 2 / 48000 * this.SAMPLE_RATE);

		this.burstAverageDuration = SYNC_FRAME_START_SAMPLES;
	}

	/**
	 * See http://sam-koblenski.blogspot.de/2015/09/everyday-dsp-for-programmers-
	 * edge.html
	 */
	static int expAvg(int sample, int avg, double w) {
		return (int) (w * sample + (1 - w) * avg);
	}

	static double expAvg(double sample, double avg, double w) {
		return w * sample + (1 - w) * avg;
	}

	public void feedSample(int currentSampleData, int currentSampleCount, double seconds) {
		this.lastAvg = avg;
		this.avg = expAvg(currentSampleData, avg, .35);
		this.currentSampleData = currentSampleData;
		this.delta = currentSampleData - oldSampleData;

		this.minY = Math.min(currentSampleData, minY);
		this.maxY = Math.max(currentSampleData, maxY);

		// TODO: should take bits per frame into account
		this.edgeDetected = Math.abs(delta) > 5000;
		this.fallingEdge = delta > 0;

		this.oldSampleData = currentSampleData;
	}

	boolean isFrameBurst(int sampleDistance) {
		return (sampleDistance >= SYNC_FRAME_START_SAMPLES - SYNC_FRAME_VARIANCE_SAMPLES && sampleDistance <= SYNC_FRAME_START_SAMPLES + SYNC_FRAME_VARIANCE_SAMPLES);
	}

	public int getStartOfFrame(boolean[] peaks) {
		final int MINIMUM_BURSTS_NEEDED = 3;

		int lastPeakPos = -1;
		int burstsDetected = 0;
		int lastBurstPos = -1;

		int firstBurstPos = -1;
		int continuousBursts = 0;
		int continuousBurstSamples = 0;

		// the frame SYNC seems to be 20x16 bits with a width of 25 samples

		for (int i = 0; i < peaks.length; i++) {
			if (peaks[i]) {
				if (lastPeakPos == -1) {
					// set first peak
					lastPeakPos = i;

				} else {
					// see if we have a frame start/sync burst
					int peakDistance = i - lastPeakPos;
					lastPeakPos = i;

					if (isFrameBurst(peakDistance)) {
						burstsDetected++;
						lastBurstPos = i;
						continuousBursts++;
						continuousBurstSamples += peakDistance;

						{
							// try to calibrate "sample rate"
							if (firstBurstPos == -1) {
								firstBurstPos = lastPeakPos;
							}

							if (continuousBursts == 10) {
								burstAverageDuration = expAvg((double) (continuousBurstSamples) / 10, burstAverageDuration, 0.05);
								// System.out.println("average burst length " +
								// burstAverageDuration);
							}
						}

					} else {
						// TODO: I don't think this is needed and messes up when both burst
						// and scanline are in the same dataset
						// burstsDetected--;

						continuousBursts = 0;
						continuousBurstSamples = 0;
						firstBurstPos = -1;
					}
				}
			}
		}

		// take the first peak after the burst
		for (int i = lastBurstPos + 1; i < peaks.length; i++) {
			if (peaks[i]) {
				lastPeakPos = i;
				break;
			}
		}

		// TOOD: while the burst is a good indication, probably the first peak
		// is more useful in determining start of frame; no idea
		// return (burstsDetected > MINIMUM_BURSTS_NEEDED) ? lastBurstPos : -1;
		return (burstsDetected >= MINIMUM_BURSTS_NEEDED) ? lastPeakPos : -1;
	}

	double getSampleRateCorrection() {
		return burstAverageDuration / 24.56;
	}

	public Set<Integer> processSamplesForEdges2(int[] sampleDataArray, int totalFramesRead) {
		Set<Integer> edges = new HashSet<Integer>();

		final int MAX_ELEMENT_COUNT = 20;
		final int MIN_PERCENTAGE_FROM_MAX = 80;
		// TODO: should take bits per frame into account
		final int EDGE_THRESHOLD = 3000;
		final int EDGE_START_THRESHOLD = 3000;

		final int MIN_EDGE_SEPARATION_SAMPLES = (int) (.479 / 1000 * SAMPLE_RATE);

		int ODD_PEAK_DURATION = (int) (.375 / 1000 * SAMPLE_RATE);
		int EVEN_PEAK_DURATION = (int) (.09375 / 1000 * SAMPLE_RATE);
		int PEAK_DURATION_VARIANCE = 3;

		int[] sampleCopy = Arrays.copyOf(sampleDataArray, sampleDataArray.length);
		Arrays.sort(sampleCopy);
		int maximumAvg = 0;
		for (int i = sampleCopy.length - MAX_ELEMENT_COUNT; i < sampleCopy.length; i++) {
			maximumAvg += sampleCopy[i];
		}
		maximumAvg /= MAX_ELEMENT_COUNT;

		int maxPeakDuration = Math.max(ODD_PEAK_DURATION, EVEN_PEAK_DURATION) + PEAK_DURATION_VARIANCE;
		int minPeakDuration = Math.min(ODD_PEAK_DURATION, EVEN_PEAK_DURATION) - PEAK_DURATION_VARIANCE;

		for (int i = maxPeakDuration; i < sampleDataArray.length - 1; i++) {
			if (sampleDataArray[i] - sampleDataArray[i + 1] > EDGE_THRESHOLD && sampleDataArray[i] > EDGE_START_THRESHOLD) {
				// select only edges that have a peak before them at a specific time
				// (.375 or .09375ms)
				for (int j = i - maxPeakDuration; j < i - minPeakDuration; j++) {
					if (sampleDataArray[j] > MIN_PERCENTAGE_FROM_MAX * maximumAvg / 100) {
						edges.add(i);
						// remove all preceding edges which are less than .479ms apart
						int startPos = Math.max(0, i - MIN_EDGE_SEPARATION_SAMPLES);
						for (int k = startPos; k < i; k++) {
							edges.remove(k);
						}
						break;
					}
				}
			}
		}

		// remove all preceding edges which are less than .479ms apart
		// {
		// Set<Integer> toRemove = new HashSet<Integer>();
		// for (Integer edgePos : edges) {
		// int startPos = Math.max(0, edgePos - MIN_EDGE_SEPARATION_SAMPLES);
		// int endPos = edgePos - 1;
		// for (int i = startPos; i < endPos; i++) {
		// toRemove.add(i);
		// }
		// }
		// edges.removeAll(toRemove);
		// }

		return edges;
	}

	public boolean[] processSamplesForEdges(int[] sampleDataArray, int totalFramesRead) {
		// TODO: should take bits per frame into account
		final int PEAK_MIN_THRESHOLD = 1500;
		final int DERIVATIVE_MIN_THRESHOLD = 5000;

		// smooth samples
		int[] averagedArray = new int[sampleDataArray.length];
		int avg = 0;
		for (int i = 0; i < sampleDataArray.length; i++) {
			avg = expAvg(sampleDataArray[i], avg, .25);
			averagedArray[i] = avg;
		}

		boolean[] peaks = new boolean[sampleDataArray.length];
		int[] derivativeSums = new int[sampleDataArray.length];
		// an element is a local peak if greater than all the surrounding
		// SUBSET_SIZE elements
		final int SUBSET_SIZE = 40;
		if (true) {
			for (int i = 0; i < averagedArray.length; i++) {
				// boolean isLargest = true;
				int startIndex = Math.max(0, i - SUBSET_SIZE / 2);
				int endIndex = Math.min(i + SUBSET_SIZE / 2, averagedArray.length - 1);

				int derivative1 = 0;
				int derivative2 = 0;
				// use only positive peaks, end position should be negative
				if (sampleDataArray[i] > PEAK_MIN_THRESHOLD) {
					for (int j = startIndex; j < i - 1; j++) {
						derivative1 += averagedArray[j + 1] - averagedArray[j];
					}
					for (int j = i; j < endIndex - 1; j++) {
						derivative2 += averagedArray[j + 1] - averagedArray[j];
					}
				}
				// derivative1 should be positive, derivative2 negative, it indicates a
				// local high; the falling edge should be stronger
				// note that preamble burst sits close to zero
				if (-derivative2 > derivative1 && derivative2 < -2000 && averagedArray[endIndex] < 0) {
					derivativeSums[i] = derivative1 - derivative2;
				}

			}

			for (int i = 0; i < derivativeSums.length; i++) {
				if (derivativeSums[i] < DERIVATIVE_MIN_THRESHOLD)
					continue;
				int startIndex = Math.max(0, i - SUBSET_SIZE / 2);
				int endIndex = Math.min(i + SUBSET_SIZE / 2, derivativeSums.length - 1);
				boolean isLargest = true;
				for (int j = startIndex; j < endIndex && isLargest; j++) {
					// TODO: what about the equal case?
					if (j != i) {
						isLargest = derivativeSums[j] <= derivativeSums[i];
					}
				}

				if (isLargest) {
					// double seconds = (double) (totalFramesRead + i) / 48000;
					// System.out.println(derivativeSums[i] + " peak at " + seconds +
					// "s");
					peaks[i] = true;
				}
			}
		}

		return peaks;

	}

	public boolean isEdgeDetected() {
		return edgeDetected;
	}

	public boolean isFallingEdge() {
		return fallingEdge;
	}

}
