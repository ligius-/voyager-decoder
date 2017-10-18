package com.ligius.voyagerdecoder.util;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;

/**
 * This singleton holds a global repository of program-wide options. It also
 * parses the command line arguments.
 * 
 * There are cleaner ways to do this but they seem overkill.
 */
public class ProgramOptions {

	@Parameter
	private List<String> parameters = new ArrayList<>();

	@Parameter(names = { "-i", "-input" }, description = "Input sound file (16-bits only!)", arity = 1, required = true)
	private String soundFileName;

	@Parameter(names = { "-o", "-outputSoundEnabled" }, description = "Whether sound output is enabled (disable if format not supported)", arity = 1)
	private boolean outputSoundEnabled = true;

	@Parameter(names = { "-b", "-bufferSizeChannel" }, description = "How many samples per channel should the processing buffer hold", validateWith = PositiveInteger.class, arity = 1)
	private int bufferSizeSamples = 2048;

	@Parameter(names = { "-startSeconds" }, description = "At which timestamp should processing start", arity = 1)
	private double startProcessingSeconds = 0.0f;

	@Parameter(names = { "-endSeconds" }, description = "At which timestamp should processing stop", arity = 1)
	private double endProcessingSeconds = 3600.0f;

	@Parameter(names = { "-paused" }, description = "Whether to start in a paused state (to allow image adjustements)", arity = 1)
	private boolean paused = false;

	@Parameter(names = { "-s", "-saveImages" }, description = "Whether decoded images should be saved")
	private boolean saveImages = false;

	@Parameter(names = { "-f", "-saveFolder" }, description = "Under which folder should the saved images be stored", arity = 1)
	private String saveFolder = "output/";

	@Parameter(names = { "-width" }, description = "Window width in pixels", arity = 1)
	private int windowWidth = 1200;

	@Parameter(names = { "-heightPerChannel" }, description = "Window height per channel in pixels", arity = 1)
	private int windowHeightPerChannel = 400;

	@Parameter(names = { "-contrast" }, description = "Output image contrast (1.0 is 100%)", arity = 1)
	private float imageContrast = 1.0f;

	@Parameter(names = { "-brightness" }, description = "Output image brightness offset, positive is brighter", arity = 1)
	private float imageBrigthnessOffset = 0f;

	@Parameter(names = "--help", description = "Show this help", help = true)
	private boolean help = true;

	private int delayMsPerBuffer = 0;

	//
	// Singleton and init
	//

	private ProgramOptions() {
	}

	private static class SingletonHelper {
		private static final ProgramOptions INSTANCE = new ProgramOptions();
	}

	public static ProgramOptions getInstance() {
		return SingletonHelper.INSTANCE;
	}

	/**
	 * Parses command-line arguments
	 */
	public void parseArguments(String args[]) {
		final JCommander jCommander = JCommander.newBuilder().addObject(this).build();
		try {
			jCommander.parse(args);
		} catch (Exception e) {

			e.printStackTrace();

			if (this.help) {
				jCommander.usage();
			}
			System.exit(1);
		}
	}

	//
	// Getters and setters
	//

	public String getSoundFileName() {
		return soundFileName;
	}

	public int getBufferSizeSamples() {
		return bufferSizeSamples;
	}

	public double getStartProcessingSeconds() {
		return startProcessingSeconds;
	}

	public double getEndProcessingSeconds() {
		return endProcessingSeconds;
	}

	public boolean isSaveImages() {
		return saveImages;
	}

	public String getSaveFolder() {
		return saveFolder;
	}

	public boolean isOutputSoundEnabled() {
		return outputSoundEnabled;
	}

	public void setOutputSoundEnabled(boolean outputSoundEnabled) {
		this.outputSoundEnabled = outputSoundEnabled;
	}

	public int getWindowWidth() {
		return windowWidth;
	}

	public int getWindowHeightPerChannel() {
		return windowHeightPerChannel;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public float getImageContrast() {
		return imageContrast;
	}

	public void setImageContrast(float imageContrast) {
		this.imageContrast = imageContrast;
	}

	public float getImageBrigthnessOffset() {
		return this.imageBrigthnessOffset;
	}

	public void setImageBrigthnessOffset(float imageBrigthnessOffset) {
		this.imageBrigthnessOffset = imageBrigthnessOffset;
	}

	public int getDelayMsPerBuffer() {
		return delayMsPerBuffer;
	}

	public void setDelayMsPerBuffer(int delayMsPerBuffer) {
		this.delayMsPerBuffer = delayMsPerBuffer;
	}
}
