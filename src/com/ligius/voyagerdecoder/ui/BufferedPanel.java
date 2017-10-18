package com.ligius.voyagerdecoder.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import com.ligius.voyagerdecoder.util.ProgramOptions;

/**
 * This class extends JPanel to draw graphics off-screen to an ImageBuffer
 */

@SuppressWarnings("serial")
public class BufferedPanel extends JPanel {
	private BufferedImage bufferedImage;
	private Graphics graphics;

	private volatile boolean updatingSize = false;
	private static AtomicInteger panelInstances = new AtomicInteger();
	private int panelInstance;

	private boolean fixedRatio = false;
	private boolean imageAdjustmentsEnabled = false;

	public BufferedPanel() {
		super(false);
		this.panelInstance = panelInstances.getAndIncrement();
		this.setBackground(Color.WHITE);

		if (ProgramOptions.getInstance().isSaveImages()) {
			new File(ProgramOptions.getInstance().getSaveFolder()).mkdir();
		}
	}

	/**
	 * This needs to be called before any rendering/drawing is done. The
	 * off-screen image buffer size will be the same as the panel's
	 */
	public void initialize() {
		initialize(this.getWidth(), this.getHeight());

		// panels which do not have fixed (raster) graphics should scale the buffer
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				// thread-safe protection
				updatingSize = true;
				initialize(BufferedPanel.this.getWidth(), BufferedPanel.this.getHeight());
				updatingSize = false;
			}
		});

	}

	/**
	 * This needs to be called before any rendering/drawing is done The off-screen
	 * image buffer size is specified in the parameters
	 * 
	 * @param imageWidth
	 *          buffered image width
	 * @param imageHeight
	 *          buffered image height
	 */
	public void initialize(int imageWidth, int imageHeight) {
		bufferedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

		if (bufferedImage != null) {
			bufferedImage.getGraphics().dispose();
		}

		this.graphics = bufferedImage.createGraphics();

		this.graphics.setFont(new Font("TimesRoman", Font.PLAIN, 9));

		clearImage();
	}

	@Override
	protected void paintComponent(Graphics g) {
		// without this, weird artifacts will be shown on redraw
		super.paintComponent(g);

		if (bufferedImage != null) {
			BufferedImage tmpImage;

			// apply user/custom image processing
			if (this.isImageAdjustmentsEnabled()) {
				RescaleOp rescaleOp = new RescaleOp(ProgramOptions.getInstance().getImageContrast(), ProgramOptions.getInstance().getImageBrigthnessOffset(), null);
				tmpImage = rescaleOp.filter(bufferedImage, null);
			} else {
				tmpImage = bufferedImage;
			}

			// draw the buffered image onto the component
			if (isFixedRatio()) {
				// scale image if a fixed ratio is specified
				double ratio = (double) tmpImage.getHeight() / tmpImage.getWidth();
				g.drawImage(tmpImage, 0, 0, (int) (getWidth()), (int) (getWidth() * ratio), this);
			} else {
				g.drawImage(tmpImage, 0, 0, this);
			}

		}
	};

	protected void clearImage() {
		if (bufferedImage != null) {
			this.getBufferedGraphics().setColor(getBackground());
			this.getBufferedGraphics().fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
			repaint();
		}
	}

	protected Graphics getBufferedGraphics() {
		if (updatingSize) {
			// silent fail: return a "dummy" graphics instead of null
			return super.getGraphics();
		} else {
			return this.graphics;
		}

	}

	protected int getBufferedWidth() {
		return bufferedImage.getWidth();
	}

	protected int getBufferedHeight() {
		return bufferedImage.getHeight();
	}

	/**
	 * Saves the content of the off-screen buffer to a png file
	 * 
	 * @param fileNameWithoutExtension
	 */
	protected void saveGraphicsToFile(final String fileNameWithoutExtension) {
		this.saveGraphicsToFile(fileNameWithoutExtension, Integer.MAX_VALUE);
	}

	/**
	 * Saves the content of the off-screen buffer to a png file
	 * 
	 * @param fileNameWithoutExtension
	 * @param maxWidth
	 *          Only the specified width (px) will be saved, up to the maximum
	 *          component (panel) width
	 */
	protected void saveGraphicsToFile(final String fileNameWithoutExtension, final int maxWidth) {
		if (ProgramOptions.getInstance().isSaveImages()) {
			try {
				BufferedImage tmpBuffer = bufferedImage.getSubimage(0, 0, Math.min(this.getBufferedWidth(), maxWidth), this.getBufferedHeight());

				if (ImageIO.write(tmpBuffer, "png", new File(ProgramOptions.getInstance().getSaveFolder() + fileNameWithoutExtension + ".png"))) {
					System.out.println("saved image " + fileNameWithoutExtension + " (panel " + panelInstance + ")");
				}
				clearImage();
			} catch (IOException e) {
				// there's nothing we can do
				e.printStackTrace();
			}
		}
	}

	private boolean isFixedRatio() {
		return fixedRatio;
	}

	protected void setFixedRatio(boolean fixedRatio) {
		this.fixedRatio = fixedRatio;
	}

	protected boolean isImageAdjustmentsEnabled() {
		return imageAdjustmentsEnabled;
	}

	protected void setImageAdjustmentsEnabled(boolean imageAdjustmentsEnabled) {
		this.imageAdjustmentsEnabled = imageAdjustmentsEnabled;
	}

}
