package com.ligius.voyagerdecoder.audio.listeners;

import com.ligius.voyagerdecoder.audio.DataPacket;

public interface PacketListener {

	/**
	 * Called whenever there is a new packet ready to be processed
	 */
	void notifyPacket(final DataPacket packet);

	/**
	 * Perform cleanup on stream end
	 */
	void stop();

	/**
	 * Perform initialization before the actual streaming process starts
	 */
	void initialize();

}
