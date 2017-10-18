package com.ligius.voyagerdecoder.audio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.ligius.voyagerdecoder.util.ProgramOptions;

public class AudioFileParser {

	private final ProgramOptions options = ProgramOptions.getInstance();

	private AudioFormat audioFormat;
	private AudioInputStream audioInputStream;

	byte[] rawBuffer;
	private int numBytesRead = 0;
	private int numFramesRead = 0;
	private int totalFramesRead = 0;

	public AudioFileParser(final String fileName) throws UnsupportedAudioFileException, IOException {
		prepareInputFile(fileName);
	}

	private void prepareInputFile(final String fileName) throws UnsupportedAudioFileException, IOException {
		final File fileIn = new File(fileName);
		System.out.println("File " + fileName + " exists:" + fileIn.exists());

		audioInputStream = AudioSystem.getAudioInputStream(fileIn);
		audioFormat = getOutFormat(audioInputStream.getFormat());
		System.out.println("Processed audio format: " + audioFormat);

		prepareBuffer();
	}

	private void prepareBuffer() {
		int bufferSize = options.getBufferSizeSamples() * getChannels() * getBytesPerFrame();
		rawBuffer = new byte[bufferSize];

		numBytesRead = 0;
		numFramesRead = 0;
	}

	//
	//

	public boolean hasNext() throws IOException {
		final boolean result;

		// this is awkward, some kind of peek should be used instead
		numBytesRead = audioInputStream.read(rawBuffer);

		if (numBytesRead != -1) {
			// last data packet should have correct size
			if (numBytesRead < rawBuffer.length) {
				rawBuffer = Arrays.copyOf(rawBuffer, numBytesRead);
			}

			numFramesRead = numBytesRead / getBytesPerFrame();
			totalFramesRead += numFramesRead;

			result = true;

		} else {
			result = false;
		}

		return result;
	}

	public DataPacket next() {
		return new DataPacket(rawBuffer, audioInputStream.getFormat(), totalFramesRead / audioFormat.getChannels());
	}

	//
	//

	private static AudioFormat getOutFormat(final AudioFormat inFormat) {
		final int ch = inFormat.getChannels();
		final float rate = inFormat.getSampleRate();
		return new AudioFormat(Encoding.PCM_SIGNED, rate, 16, ch, 2, rate, false);
	}

	private static int getBytesPerFrame(final AudioFormat audioFormat) {
		int bytesPerFrame = audioFormat.getFrameSize();
		if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
			// some audio formats may have unspecified frame size
			// in that case we may read any amount of bytes
			bytesPerFrame = 1;
		}
		return bytesPerFrame;
	}

	public double getSecondsElapsed() {
		return (double) totalFramesRead / audioInputStream.getFormat().getSampleRate() / audioFormat.getChannels();
	}
	
	//
	//

	public int getTotalFramesRead() {
		return totalFramesRead;
	}

	public AudioFormat getAudioFormat() {
		return audioFormat;
	}

	public int getChannels() {
		return audioFormat.getChannels();
	}

	public int getBytesPerFrame() {
		return getBytesPerFrame(audioFormat);
	}

}
