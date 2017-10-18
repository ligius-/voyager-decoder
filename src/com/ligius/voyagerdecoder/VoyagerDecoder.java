package com.ligius.voyagerdecoder;

import com.ligius.voyagerdecoder.util.ProgramOptions;

/**
 * The main class, starting point for everything else
 * 
 */
public class VoyagerDecoder {

	public VoyagerDecoder(final String args[]) {
		final ProgramOptions options = ProgramOptions.getInstance();

		options.parseArguments(args);
	}

	public void start() {
		new Thread(new DecoderOrchestrator()).start();
	}

	public static void main(final String[] args) {
		new VoyagerDecoder(args).start();
	}
}
