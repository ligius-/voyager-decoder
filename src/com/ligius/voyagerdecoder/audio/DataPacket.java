package com.ligius.voyagerdecoder.audio;

import javax.sound.sampled.AudioFormat;

/**
 * Represents an immutable audio data packet
 * 
 */
public class DataPacket {
	final private byte[] rawData;
	final private int[][] sampleDataPerChannel;
	final private int totalFramesRead;

	public DataPacket(final byte[] rawData, final AudioFormat audioFormat, final int totalFramesRead) {
		this.rawData = rawData;
		this.sampleDataPerChannel = convertSoundByteArrayToSampleArray(rawData, audioFormat.getChannels(), audioFormat.getFrameSize(), audioFormat.isBigEndian());
		this.totalFramesRead = totalFramesRead;
	}

	int getValueForChannel(int channel, int channels, int currentSamplePos, byte[] data, int bytesPerFrame, boolean bigEndian) {
		int sampleData = 0;
		// the data seems to be interleaved such as (for little-endian):
		// CH0=[3][2] CH1=[1][0] CH0=[7][6] CH1=[5][4]...
		int i = currentSamplePos + (channel * bytesPerFrame / channels);

		for (int j = 0; j < bytesPerFrame / channels; j++) {
			if (j > 0) {
				sampleData <<= 8;
			}

			int samplePosition;
			if (!bigEndian) {
				samplePosition = i + bytesPerFrame / channels - j - 1;
			} else {
				samplePosition = i + j; // TODO: unchecked
			}
			sampleData += data[samplePosition];
		}

		return sampleData;
	}

	int[][] convertSoundByteArrayToSampleArray(byte data[], int channels, int bytesPerFrame, boolean bigEndian) {
		int[][] result = new int[channels][data.length / bytesPerFrame];

		int pos = 0;

		for (int i = 0; i <= data.length - bytesPerFrame; i += bytesPerFrame) {

			pos = i / bytesPerFrame;

			for (int ch = 0; ch < channels; ch++) {
				result[ch][pos] = getValueForChannel(ch, channels, i, data, bytesPerFrame, bigEndian);
			}

		}

		return result;
	}

	//
	//

	/**
	 * Returns the raw (interleaved) data, used for sound rendering
	 */
	public byte[] getRawData() {
		return rawData;
	}

	/**
	 * Returns the sample data array, used for waveform representation and further
	 * processing
	 * 
	 * @param channel
	 *          0 if only one channel, for stereo: 0 is left, 1 is right channel
	 */
	public int[] getSampleDataForChannel(final int channel) {
		return sampleDataPerChannel[channel];
	}

	/**
	 * Returns the number of audio frames (samples) read until now.
	 */
	public int getTotalFramesRead() {
		return totalFramesRead;
	}
}