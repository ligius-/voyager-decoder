package com.ligius.voyagerdecoder.ui;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.ligius.voyagerdecoder.util.ProgramOptions;

/**
 * This class adds a simple options panel, mostly for debugging and fine-tuning purposes.
 */
@SuppressWarnings("serial")
public class OptionsPanel extends JPanel {

	public OptionsPanel() {
		
		// this should be rewritten for JavaFX or something that allows binding
		
		{
			JCheckBox checkbox = new JCheckBox("Enable sound");
			checkbox.setSelected(ProgramOptions.getInstance().isOutputSoundEnabled());
			checkbox.addChangeListener(event -> ProgramOptions.getInstance().setOutputSoundEnabled(checkbox.isSelected()));
			// disable if not available through command line
			if (!ProgramOptions.getInstance().isOutputSoundEnabled()) {
				checkbox.setEnabled(false);
			}
			this.add(checkbox);
		}

		{
			JCheckBox checkbox = new JCheckBox("Paused");
			checkbox.setSelected(ProgramOptions.getInstance().isPaused());
			checkbox.addChangeListener(event -> ProgramOptions.getInstance().setPaused(checkbox.isSelected()));
			this.add(checkbox);
		}

		{
			this.add(new JLabel("Contrast"));
			SpinnerNumberModel model = new SpinnerNumberModel(ProgramOptions.getInstance().getImageContrast(), 0f, 5f, 0.05f);
			JSpinner spinner = new JSpinner(model);
			((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
			spinner.addChangeListener(event -> ProgramOptions.getInstance().setImageContrast(model.getNumber().floatValue()));
			this.add(spinner);
		}

		{
			this.add(new JLabel("Brightness offset"));
			SpinnerNumberModel model = new SpinnerNumberModel(ProgramOptions.getInstance().getImageBrigthnessOffset(), -255, 255, 5);
			JSpinner spinner = new JSpinner(model);
			((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
			spinner.addChangeListener(event -> ProgramOptions.getInstance().setImageBrigthnessOffset(model.getNumber().floatValue()));
			this.add(spinner);
		}

		{
			this.add(new JLabel("Slow down"));
			SpinnerNumberModel model = new SpinnerNumberModel(ProgramOptions.getInstance().getDelayMsPerBuffer(), 0, 5000, 50);
			JSpinner spinner = new JSpinner(model);
			((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
			spinner.addChangeListener(event -> ProgramOptions.getInstance().setDelayMsPerBuffer(model.getNumber().intValue()));
			this.add(spinner);
			this.add(new JLabel("ms"));
		}

	}
}
