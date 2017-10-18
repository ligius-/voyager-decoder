package com.ligius.voyagerdecoder.audio.listeners;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import com.ligius.voyagerdecoder.audio.DataPacket;
import com.ligius.voyagerdecoder.util.ProgramOptions;

public class AudioRenderer implements PacketListener {
	SourceDataLine line;

	// https://edwin.baculsoft.com/2010/11/how-to-play-mp3-files-with-java/
	private synchronized SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
		SourceDataLine res = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		res = (SourceDataLine) AudioSystem.getLine(info);
		res.open(audioFormat);

		return res;
	}

	private void showSoundOutputDebug() {
		try {
			System.out.println("Available mixers:");
			Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			for (Mixer.Info mixerInfo : mixers) {
				System.out.println("Mixer: " + mixerInfo);

				Mixer m = AudioSystem.getMixer(mixerInfo);
				Line.Info[] lines = m.getSourceLineInfo();

				for (Line.Info li : lines) {
					System.out.println("\tFound source (render/output) line: " + li);
					try {
						m.open();
						m.close();
					} catch (LineUnavailableException e) {
						System.out.println("\tLine unavailable.");
					}
				}
			}
		} catch (Exception e) {
			// catch everything, this function might be used in other "catch" blocks
			e.printStackTrace();
		}
	}

	public AudioRenderer(AudioFormat baseFormat) {
		final AudioFormat renderFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2,
				baseFormat.getSampleRate(), false);

		try {
			line = getLine(renderFormat);
			line.start();
		} catch (Exception e) {
			showSoundOutputDebug();
			throw new Error("Cannot render audio: ", e);
		}
	}

	@Override
	public void notifyPacket(DataPacket packet) {
		if (ProgramOptions.getInstance().isOutputSoundEnabled()) {
			line.write(packet.getRawData(), 0, packet.getRawData().length);
		} else if (line.isOpen()) {
			// avoid sound artifacts
			line.flush();
		}
	}

	@Override
	public void stop() {
		line.drain();
		line.stop();
	}

	@Override
	public void initialize() {
		// do nothing
	}
}
