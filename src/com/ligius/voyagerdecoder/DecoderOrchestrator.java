package com.ligius.voyagerdecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import javax.sound.sampled.UnsupportedAudioFileException;

import com.ligius.voyagerdecoder.audio.AudioFileParser;
import com.ligius.voyagerdecoder.audio.DataPacket;
import com.ligius.voyagerdecoder.audio.listeners.AudioRenderer;
import com.ligius.voyagerdecoder.audio.listeners.PacketListener;
import com.ligius.voyagerdecoder.ui.ChannelDisplayPanel;
import com.ligius.voyagerdecoder.ui.MainFrame;
import com.ligius.voyagerdecoder.util.ProgramOptions;

/**
 * This class ties all the concerns together: UI, sound, processing, while
 * currently the initializations need to be performed in a specific order.
 * 
 * Designed as a thread so it doesn't bog down Swing
 */
public class DecoderOrchestrator implements Runnable {

	private final ProgramOptions options = ProgramOptions.getInstance();
	private final List<PacketListener> sampleListeners = new ArrayList<PacketListener>();

	private MainFrame frame;

	private AudioFileParser parser;

	//
	//

	private void addSoundRenderer() {
		if (options.isOutputSoundEnabled()) {
			sampleListeners.add(new AudioRenderer(parser.getAudioFormat()));
		}
	}

	private void buildLayout() {
		frame = new MainFrame(parser.getChannels());

		for (int ch = 0; ch < parser.getChannels(); ch++) {
			final ChannelDisplayPanel panel = new ChannelDisplayPanel(parser.getAudioFormat(), ch);
			sampleListeners.addAll(panel.getPacketListeners());

			frame.addChannelDisplayPanel(panel);
		}

		frame.setVisible(true);
	}

	private void updateProgress(double seconds) {
		frame.setTitle((double) ((long) (seconds * 1000)) / 1000 + "s");
	}

	//
	//

	@Override
	public void run() {
		try {

			parser = new AudioFileParser(options.getSoundFileName());

			addSoundRenderer();

			buildLayout();

			ForkJoinPool forkJoinPool = new ForkJoinPool();

			// initialize listeners
			sampleListeners.forEach(listener -> listener.initialize());

			while (parser.hasNext()) {

				while (ProgramOptions.getInstance().isPaused()) {
					// yield to other threads if paused
					Thread.sleep(50);
				}

				double seconds = parser.getSecondsElapsed();
				updateProgress(seconds);

				// process sound data
				if (seconds > options.getStartProcessingSeconds() && seconds < options.getEndProcessingSeconds()) {

					final DataPacket packet = parser.next();

					// call listeners multi-threaded
					forkJoinPool.submit(() -> sampleListeners.parallelStream().forEach(listener -> listener.notifyPacket(packet))).get();
				}

				// reached the end timestamp?
				if (seconds > options.getEndProcessingSeconds()) {
					break;
				}

				// slow down
				if (options.getDelayMsPerBuffer() > 0) {
					Thread.sleep(options.getDelayMsPerBuffer());
				}

			}
			System.out.println("Done: " + parser.getTotalFramesRead() + " frames");

			sampleListeners.forEach(listener -> listener.stop());

		} catch (UnsupportedAudioFileException | IOException | InterruptedException | ExecutionException | Error e) {
			// probably all errors and exceptions are non-recoverable, but specify
			// them individually to allow the debugger to break at correct line
			e.printStackTrace();
			System.exit(1);
		}
	}
}
