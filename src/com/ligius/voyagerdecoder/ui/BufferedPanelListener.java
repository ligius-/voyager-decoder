package com.ligius.voyagerdecoder.ui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Timer;

import com.ligius.voyagerdecoder.audio.DataPacket;
import com.ligius.voyagerdecoder.audio.listeners.PacketListener;

/**
 * This class creates a BufferedPanel that also forwards packet listener events.
 * A lot of the BufferedPanel/AWT stuff is wrapped away.
 * 
 */
public abstract class BufferedPanelListener implements PacketListener {
	
	private final BufferedPanel bufferedPanel;
	
	private Timer swingTimer;
	private static final boolean USE_PERIODIC_TIMER = false;
	private static final int PERIODIC_TIMER_INTERVAL_MS = 15;

	protected BufferedPanelListener() {
		this.bufferedPanel = new BufferedPanel();

		if (USE_PERIODIC_TIMER) {
			this.swingTimer = new Timer(PERIODIC_TIMER_INTERVAL_MS, e -> {
				this.bufferedPanel.repaint();
			});
		}
	}

	public void initializeScaled(int width, int height) {
		bufferedPanel.initialize(width, height);

		if (USE_PERIODIC_TIMER) {
			swingTimer.start();
		}
	}

	@Override
	public void initialize() {
		bufferedPanel.initialize();

		if (USE_PERIODIC_TIMER) {
			swingTimer.start();
		}
	}

	/**
	 * You need to override this to process samples
	 */
	@Override
	public void notifyPacket(DataPacket packet) {
		repaint();
	}

	/**
	 * Returns the Component so it can be added to a container
	 */
	public Component getComponent() {
		return bufferedPanel;
	}

	//
	// WRAPPER
	//

	protected int getWidth() {
		return this.bufferedPanel.getWidth();
	}

	protected int getHeight() {
		return this.bufferedPanel.getHeight();
	}

	protected Graphics getGraphics() {
		return this.bufferedPanel.getBufferedGraphics();
	}

	protected void repaint() {
		if (USE_PERIODIC_TIMER) {
			// do nothing, handled by the swing timer
		} else {
			this.bufferedPanel.repaint();
		}
	}

	protected void clearImage() {
		this.bufferedPanel.clearImage();
	}

	protected void saveGraphicsToFile(final String fileName) {
		this.bufferedPanel.saveGraphicsToFile(fileName);
	}

	protected void saveGraphicsToFile(final String fileName, final int widthCropped) {
		this.bufferedPanel.saveGraphicsToFile(fileName, widthCropped);
	}

	protected int getBufferedHeight() {
		return this.bufferedPanel.getBufferedHeight();
	}

	protected void setFixedRatio(boolean fixedRatio) {
		this.bufferedPanel.setFixedRatio(fixedRatio);
	}

	protected void setImageAdjustmentsEnabled(boolean imageAdjustmentsEnabled) {
		this.bufferedPanel.setImageAdjustmentsEnabled(imageAdjustmentsEnabled);
	}
}
