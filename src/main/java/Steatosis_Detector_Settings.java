import java.awt.Image;
import java.awt.Scrollbar;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.net.URL;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;

public class Steatosis_Detector_Settings implements PlugIn, AdjustmentListener
{
	private static final int BG_BRIGHT = 127;
	private static final int BG_SAT = 127;
	private static final int BG_HUE = 75;
	private ImagePlus examplePic;
	private boolean useCoreSaturation;
	private byte[] hue;
	private byte[] hueOrig;
	private byte[] sat;
	private byte[] satOrig;
	private byte[] bright;
	private byte[] brightOrig;
	private int currentSatThresh;
	private double currentMin;
	private double currentMax;
	private double minCirc;
	private double maxCircArea;
	private double minRoundness;
	private double maxRoundnessArea;
	private double minSolid;
	private double maxSolidArea;


	@Override
	public void adjustmentValueChanged(final AdjustmentEvent aEvent)
	{
		final Scrollbar slider = (Scrollbar) aEvent.getAdjustable();
		final int saturation = slider.getValue();
		final ColorProcessor processor = (ColorProcessor) this.examplePic.getProcessor();
		if (saturation < this.currentSatThresh)
		{
			for (int i = 0; i < this.sat.length; i++)
			{
				if ((this.satOrig[i] & 0xFF) > saturation && (this.satOrig[i] & 0xFF) <= this.currentSatThresh)
				{
					this.hue[i] = this.hueOrig[i];
					this.sat[i] = this.satOrig[i];
					this.bright[i] = this.brightOrig[i];
				}
			}
			this.currentSatThresh = saturation;
		}
		else
		{
			for (int i = 0; i < this.sat.length; i++)
			{
				if ((this.satOrig[i] & 0xFF) > this.currentSatThresh && (this.satOrig[i] & 0xFF) <= saturation)
				{
					this.hue[i] = BG_HUE;
					this.sat[i] = BG_SAT;
					this.bright[i] = BG_BRIGHT;
				}
			}
			this.currentSatThresh = saturation;
		}
		processor.setHSB(this.hue, this.sat, this.bright);
		this.examplePic.updateAndDraw();
	}


	@Override
	public void run(final String aArgs)
	{
		final URL url = getClass().getResource("/patomationCrop.jpg");
		final Image image = Toolkit.getDefaultToolkit().getImage(url);
		this.examplePic = new ImagePlus("Example for thresholding", image);
		this.examplePic.show();
		final ColorProcessor processor = (ColorProcessor) this.examplePic.getProcessor();
		this.hue = new byte[processor.getWidth() * processor.getHeight()];
		this.sat = new byte[processor.getWidth() * processor.getHeight()];
		this.bright = new byte[processor.getWidth() * processor.getHeight()];
		processor.getHSB(this.hue, this.sat, this.bright);
		this.hueOrig = this.hue.clone();
		this.satOrig = this.sat.clone();
		this.brightOrig = this.bright.clone();

		this.currentMin = Prefs.get(FatBlob_Detector.MIN_THRESHOLD_AREA, -1.0);
		if (this.currentMin == -1.0)
		{
			this.currentMin = 15.00;
		}
		this.currentMax = Prefs.get(FatBlob_Detector.MAX_THRESHOLD_AREA, -1.0);
		if (this.currentMax == -1.0)
		{
			this.currentMax = 35000.00; // Yes, that huge. See example file S8.mrxs in the top 'bend'.
		}
		this.minCirc = Prefs.get(FatBlob_Detector.MIN_THRESHOLD_CIRC, -1.0);
		if (this.minCirc == -1.0)
		{
			this.minCirc = 0.35; // Best first guess
		}
		this.maxCircArea = Prefs.get(FatBlob_Detector.MAX_THRESHOLD_CIRCULARITY_AREA, -1.0);
		if (this.maxCircArea == -1.0)
		{
			this.maxCircArea = 6000; // Best first guess
		}
		this.minRoundness = Prefs.get(FatBlob_Detector.MIN_THRESHOLD_ROUNDNESS, -1.0);
		if (this.minRoundness == -1.0)
		{
			this.minRoundness = 0.35; // Best first guess
		}
		this.maxRoundnessArea = Prefs.get(FatBlob_Detector.MAX_THRESHOLD_ROUNDNESS_AREA, -1.0);
		if (this.maxRoundnessArea == -1.0)
		{
			this.maxRoundnessArea = 650; // Best first guess
		}
		this.minSolid = Prefs.get(FatBlob_Detector.MIN_THRESHOLD_SOLID, -1.0);
		if (this.minSolid == -1.0)
		{
			this.minSolid = 0.35; // Best first guess
		}
		this.maxSolidArea = Prefs.get(FatBlob_Detector.MAX_THRESHOLD_SOLIDITY_AREA, -1.0);
		if (this.maxSolidArea == -1.0)
		{
			this.maxSolidArea = 6000; // Best first guess
		}
		// this.maxAspectRatio = Prefs.get(FatBlob_Detector.MAX_THRESHOLD_ASPECT_RATIO, -1.0);
		// if (this.maxAspectRatio == -1.0)
		// {
		// this.maxAspectRatio = 2.5; // Best first guess
		// }
		this.currentSatThresh = (int) Prefs.get(FatBlob_Detector.SAT_THRESHOLD, -1);
		if (this.currentSatThresh == -1)
		{
			this.currentSatThresh = 15;
		}
		this.useCoreSaturation = Prefs.get(FatBlob_Detector.USE_CORE_SAT, true);

		for (int i = 0; i < this.sat.length; i++)
		{
			if ((this.satOrig[i] & 0xFF) <= this.currentSatThresh)
			{
				this.hue[i] = 75;
				this.sat[i] = 127;
				this.bright[i] = 127;
			}
		}
		((ColorProcessor) this.examplePic.getProcessor()).setHSB(this.hue, this.sat, this.bright);
		this.examplePic.updateAndDraw();

		final GenericDialog parameterDialog = new GenericDialog("Set Steatosis Detector parameters");
		parameterDialog.addNumericField("Minimum area", this.currentMin, 2, 7, "\u00B5" + "m" + "\u00B2");
		parameterDialog.addNumericField("Maximum area", this.currentMax, 2, 7, "\u00B5" + "m" + "\u00B2");
		parameterDialog.addNumericField("Minimal circularity", this.minCirc, 2, 7, "");
		parameterDialog.addNumericField("Maximum circularity-threshold area", this.maxCircArea, 2, 7, "\u00B5" + "m" + "\u00B2");
		parameterDialog.addNumericField("Minimal roundness", this.minRoundness, 2, 7, "");
		parameterDialog.addNumericField("Maximum roundness-threshold area", this.maxRoundnessArea, 2, 7, "\u00B5" + "m" + "\u00B2");
		parameterDialog.addNumericField("Minimal solidity", this.minSolid, 2, 7, "");
		parameterDialog.addNumericField("Maximum solidity-threshold area", this.maxSolidArea, 2, 7, "\u00B5" + "m" + "\u00B2");
		// parameterDialog.addNumericField("Maximal aspect ratio", this.maxAspectRatio, 2, 7, "\u00B5" + "m" + "\u00B2");
		parameterDialog.addSlider("Saturation threshold", 0, 255, this.currentSatThresh);
		((Scrollbar) parameterDialog.getSliders().get(parameterDialog.getSliders().size() - 1)).addAdjustmentListener(this);
		parameterDialog.addCheckbox("Use two step core saturation", this.useCoreSaturation);
		parameterDialog.showDialog();

		if (parameterDialog.wasOKed())
		{
			Prefs.set(FatBlob_Detector.MIN_THRESHOLD_AREA, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.MAX_THRESHOLD_AREA, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.MIN_THRESHOLD_CIRC, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.MAX_THRESHOLD_CIRCULARITY_AREA, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.MIN_THRESHOLD_ROUNDNESS, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.MAX_THRESHOLD_ROUNDNESS_AREA, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.MIN_THRESHOLD_SOLID, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.MAX_THRESHOLD_SOLIDITY_AREA, parameterDialog.getNextNumber());
			// Prefs.set(FatBlob_Detector.MAX_THRESHOLD_ASPECT_RATIO, parameterDialog.getNextNumber());
			Prefs.set(FatBlob_Detector.SAT_THRESHOLD, this.currentSatThresh);
			Prefs.set(FatBlob_Detector.USE_CORE_SAT, parameterDialog.getNextBoolean());

			Prefs.savePreferences();
		}

		this.examplePic.close();
	}
}
