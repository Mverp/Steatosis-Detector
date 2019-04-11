import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;

public class FatBlob_Detector
{
	public static final String MIN_THRESHOLD_AREA = "LiverSteatosis.minThresh";
	public static final String MAX_THRESHOLD_AREA = "LiverSteatosis.maxThresh";
	public static final String MIN_THRESHOLD_CIRC = "LiverSteatosis.minThreshCirc";
	public static final String MAX_THRESHOLD_CIRCULARITY_AREA = "LiverSteatosis.minThreshCircArea";
	public static final String MIN_THRESHOLD_ROUNDNESS = "LiverSteatosis.minThreshRoundness";
	public static final String MAX_THRESHOLD_ROUNDNESS_AREA = "LiverSteatosis.minThreshRoundnessArea";
	public static final String MIN_THRESHOLD_SOLID = "LiverSteatosis.minThreshSolid";
	public static final String MAX_THRESHOLD_SOLIDITY_AREA = "LiverSteatosis.minThreshSolidArea";
	// public static final String MAX_THRESHOLD_ASPECT_RATIO = "LiverSteatosis.maxThreshAspectRatio";
	public static final String SAT_THRESHOLD = "LiverSteatosis.saturationThresh";
	public static final String USE_CORE_SAT = "LiverSteatosis.useCoreSaturation";
	private static final String DIALOG_X = "LiverSteatosis.dialogX";
	private static final String DIALOG_Y = "LiverSteatosis.dialogY";
	public static final String[] CIRCULAR_MEASURES = { "Circularity", "Aspect Ratio", "Roundness", "Solidity" };
	private static final Color FAT_COLOUR = Color.GREEN;
	private static final Color TISSUE_COLOUR = Color.CYAN;
	private int saturation;
	private boolean fillRoi = true;
	private List<FatBlob> selectedRois;
	private List<FatBlob> totalRois;
	private List<FatBlob> deletedRois;
	private Overlay currentOverlay;
	private ImagePlus currentImage;


	public void addRoi(final int x, final int y)
	{
		for (final FatBlob blob : this.deletedRois)
		{
			final Roi roi = blob.getRoi();
			if (roi.contains(x, y))
			{
				this.selectedRois.add(blob);
				this.deletedRois.remove(blob);

				this.currentOverlay.add(roi);
				this.currentImage.killRoi();
				this.currentImage.updateAndDraw();

				break;
			}
		}
	}


	private ImagePlus colourThreshold(final ImagePlus aImage)
	{
		if (!aImage.isVisible())
		{
			aImage.show();
		}
		final int[] min = { 0, 0, 0 };
		final int[] max = { 255, this.saturation, 255 };
		final String[] filter = { "pass", "pass", "pass" };
		final String[] names = { "Hue", "Saturation", "Brightness" };
		final ImagePlus[] images = new ImagePlus[3];
		IJ.selectWindow(aImage.getID());

		IJ.run("HSB Stack");
		IJ.selectWindow(aImage.getID());
		int waitForIt = 0;
		while (aImage.getStack().size() <= 1 && waitForIt < 8)
		{
			IJ.wait(500);
			waitForIt++;
		}
		IJ.wait(500);
		IJ.run("Convert Stack to Images");

		for (int i = 0; i < 3; i++)
		{
			IJ.selectWindow(names[i]);
			images[i] = IJ.getImage();
			IJ.setThreshold(min[i], max[i]);
			IJ.run("Convert to Mask");
			if (filter[i] == "stop")
				IJ.run("Invert");
		}

		final ImageCalculator calc = new ImageCalculator();
		final ImagePlus result1 = calc.run("AND create", images[0], images[1]);
		final ImagePlus result2 = calc.run("AND create", result1, images[2]);

		for (int i = 0; i < 3; i++)
		{
			images[i].close();
		}
		result1.close();

		// IJ.selectWindow(result2.getID());
		return result2;
	}


	private Roi[] getBlobs(final ImagePlus aSource)
	{
		if (!aSource.isVisible())
		{
			aSource.show();
		}
		IJ.run("Set Measurements...", "area shape");

		IJ.selectWindow(aSource.getID());
		IJ.wait(1000);
		IJ.run("Analyze Particles...", "display clear add");
		final RoiManager manager = RoiManager.getRoiManager();
		Window roiWindow = WindowManager.getWindow(manager.getTitle());
		while (roiWindow == null)
		{
			IJ.wait(500);
			roiWindow = WindowManager.getWindow(manager.getTitle());
		}

		IJ.wait(500);
		final Roi[] blobs = manager.getRoisAsArray();
		final Roi noRoi = null;
		aSource.setRoi(noRoi);
		manager.reset();
		manager.close();

		IJ.selectWindow(aSource.getID());
		return blobs;
	}


	private void preFilterRois(final List<FatBlob> aBlobs, final double aMaxThreshold, final double aRoundnessThreshold)
	{
		// final double minCirc = Prefs.get(MIN_THRESHOLD_CIRC, 0.35);
		// final double maxCircArea = Prefs.get(MAX_THRESHOLD_CIRCULARITY_AREA, 6000);
		// final double minRoundness = Prefs.get(MIN_THRESHOLD_ROUNDNESS, 0.35);
		// final double maxRoundnessArea = Prefs.get(MAX_THRESHOLD_ROUNDNESS_AREA, 650);
		// final double minSolid = Prefs.get(MIN_THRESHOLD_SOLID, 0.35);
		// final double maxSolidArea = Prefs.get(MAX_THRESHOLD_SOLIDITY_AREA, 6000);

		final double[] factors = { 0.002805, 6.282, 7.167, 10.00 }; // Size, circularity, roundness, solidity
		final LogisticRegression logR = new LogisticRegression(-16.16, factors);
		final List<FatBlob> blobs = new ArrayList<>();
		blobs.addAll(aBlobs);
		for (final FatBlob blob : blobs)
		{
			if (blob.getArea() > aMaxThreshold)
			{
				removeRoi(blob);
			}

			// Logistic regression
			final double[] values = { blob.getArea(), blob.getCircMeasure(CIRCULAR_MEASURES[0]), blob.getCircMeasure(CIRCULAR_MEASURES[2]), blob.getCircMeasure(CIRCULAR_MEASURES[3]) };
			final double logValue = logR.calculate(values);
			if (logValue <= 0.5)
			{
				setIsFat(blob, false);
			}
			// else if ((blob.getCircMeasure(CIRCULAR_MEASURES[2]) < minRoundness && blob.getArea() < maxRoundnessArea)
			// || (blob.getCircMeasure(CIRCULAR_MEASURES[0]) < minCirc && blob.getArea() < maxCircArea) || (blob.getCircMeasure(CIRCULAR_MEASURES[3]) < minSolid && blob.getArea() < maxSolidArea))
			// {
			// setIsFat(blob, false);
			// }
		}

		this.currentImage.killRoi();
		this.currentImage.updateAndDraw();
	}


	private void removeRoi(final FatBlob aBlob)
	{
		this.selectedRois.remove(aBlob);
		this.deletedRois.add(aBlob);

		this.currentOverlay.remove(aBlob.getRoi());
	}


	public void removeRoi(final int x, final int y)
	{
		for (final FatBlob blob : this.selectedRois)
		{
			final Roi roi = blob.getRoi();
			if (roi.contains(x, y))
			{
				removeRoi(blob);
				this.currentImage.killRoi();
				this.currentImage.updateAndDraw();

				break;
			}
		}
	}


	public double[] run(final List<List<Double[]>> aFatAreaSizes, final Double aRoiNumber, final boolean aAutomatic)
	{
		this.currentImage = IJ.getImage();
		this.saturation = (int) Prefs.get(SAT_THRESHOLD, 15);
		final ImagePlus mask = colourThreshold(this.currentImage.duplicate());
		final Roi[] rois = getBlobs(mask);
		mask.close();

		double totalAreaBlob = 0;
		double totalAreaNonBlob = 0;
		double totalArea = 0;
		final double minThreshold = Prefs.get(MIN_THRESHOLD_AREA, 15);
		final double maxThreshold = Prefs.get(MAX_THRESHOLD_AREA, 35000);
		final boolean useMinPresent = Prefs.get(USE_CORE_SAT, true);
		if (rois.length > 1 || this.currentImage.getProcessor().getPixelValue(0, 0) < 255) // Only act if there is some tissue to be found
		{
			this.totalRois = new ArrayList<>();
			this.selectedRois = new ArrayList<>();
			this.deletedRois = new ArrayList<>();

			final ResultsTable results = ResultsTable.getResultsTable();
			this.currentOverlay = new Overlay();
			double totalAreaRoi = 0;

			final double pixelArea = this.currentImage.getCalibration().pixelHeight * this.currentImage.getCalibration().pixelWidth;
			final double saturationThreshold = 15.0 / 255.0; // Threshold between 0 and 1 and 255 is the max in the 8-bit scale.
			double roisIgnored = 0;
			for (final Roi roi : rois)
			{
				final String[] name = roi.getName().split("-");
				final String label = name[0];
				final double area = results.getValueAsDouble(results.getColumnIndex("Area"), Integer.parseInt(label) - 1);

				// Roundness measures

				final double circ = results.getValueAsDouble(results.getColumnIndex("Circ."), Integer.parseInt(label) - 1);
				final double ar = results.getValueAsDouble(results.getColumnIndex("AR"), Integer.parseInt(label) - 1);
				final double round = results.getValueAsDouble(results.getColumnIndex("Round"), Integer.parseInt(label) - 1);
				final double solidity = results.getValueAsDouble(results.getColumnIndex("Solidity"), Integer.parseInt(label) - 1);

				double roiOkArea = 0;
				if (area >= minThreshold)
				{
					boolean minPresent = false;
					if (useMinPresent)
					{
						final Point[] roiPoints = roi.getContainedPoints();
						for (final Point point : roiPoints)
						{
							final int[] value = this.currentImage.getPixel(point.x, point.y);
							final float saturation = Color.RGBtoHSB(value[0], value[1], value[2], null)[1];
							if (saturation <= saturationThreshold)
							{
								roiOkArea += pixelArea;
							}

							if (roiOkArea >= 12)
							{
								minPresent = true;
								break;
							}
						}
					}

					if (!useMinPresent || minPresent)
					{
						final FatBlob blob = new FatBlob(roi, area);
						blob.addCircMeasure(CIRCULAR_MEASURES[0], circ);
						blob.addCircMeasure(CIRCULAR_MEASURES[1], ar);
						blob.addCircMeasure(CIRCULAR_MEASURES[2], round);
						blob.addCircMeasure(CIRCULAR_MEASURES[3], solidity);
						this.totalRois.add(blob);
						if (area > maxThreshold)
						{
							totalAreaRoi += area; // Max area is not tissue!
							this.deletedRois.add(new FatBlob(roi, area));
							roi.setFillColor(FAT_COLOUR); // Set colour just in case it is added again
							roi.setStrokeColor(FAT_COLOUR);
						}
						else
						{
							totalAreaRoi += area;
							this.currentOverlay.add(roi);
							this.selectedRois.add(blob);
						}
					}
					else
					{
						roisIgnored++;
					}
				}
			}
			this.currentOverlay.setFillColor(FAT_COLOUR);
			this.currentOverlay.setStrokeColor(FAT_COLOUR);

			final double roundnessThreshold = 0.35;
			preFilterRois(this.selectedRois, maxThreshold, roundnessThreshold);

			if (!this.totalRois.isEmpty())
			{
				IJ.log("Number of rois Ignored: " + roisIgnored);

				if (!aAutomatic)
				{
					this.currentImage.setOverlay(this.currentOverlay);

					// final MouseListener listener = new RoiRemovalListener(this, this.currentImage.getCanvas());
					// this.currentImage.getCanvas().addMouseListener(listener);

					final int dialogX = (int) Prefs.get(DIALOG_X, 15);
					final int dialogY = (int) Prefs.get(DIALOG_Y, 15);
					final NonBlockingGenericDialog nonBlockDialog = new NonBlockingGenericDialog("ROI removal");
					nonBlockDialog.setLocation(dialogX, dialogY);
					final StringBuilder message = new StringBuilder("Please remove the selections that should be ignored and label non-fat white tissue separately.\n");
					message.append(" -------------------------------------------------------- \n");
					message.append(" - Alt + click: Remove a selection as background.\n");
					message.append(" - Shift + click: Undo the removal of a selection. Finds the closest removed selection and recolours it.\n");
					message.append(" -------------------------------------------------------- \n");
					message.append(" - Mouse click: Label a selection as non-fat white tissue (e.g. blood vessels). The selection will change colour.\n");
					message.append(" - Ctrl + click: Label a selection as fat. This should only been done on a previously mislabeled selection and should revert the colour change.\n\n");
					nonBlockDialog.enableYesNoCancel("Done", "Stop and Save");
					nonBlockDialog.addMessage(message.toString());
					nonBlockDialog.addCheckbox("Show selections", true);
					nonBlockDialog.addCheckbox("Filled ROI", true);
					nonBlockDialog.addCheckbox("All fat", false);
					nonBlockDialog.addCheckbox("All non-fat tissue", false);

					// Add listener to hide/show all ROIs
					((Checkbox) nonBlockDialog.getCheckboxes().get(0)).addItemListener(new ItemListener()
					{
						@Override
						public void itemStateChanged(final ItemEvent e)
						{
							FatBlob_Detector.this.currentImage.setHideOverlay(e.getStateChange() == ItemEvent.DESELECTED);
							FatBlob_Detector.this.currentImage.updateAndDraw();
						}
					});

					((Checkbox) nonBlockDialog.getCheckboxes().get(1)).addItemListener(new ItemListener()
					{
						@Override
						public void itemStateChanged(final ItemEvent e)
						{
							FatBlob_Detector.this.fillRoi = e.getStateChange() == ItemEvent.SELECTED;
							for (final FatBlob blob : FatBlob_Detector.this.selectedRois)
							{
								setIsFat(blob, blob.isFat());
							}
							FatBlob_Detector.this.currentImage.updateAndDraw();
						}
					});

					// Add listener to set every 'selected' blob to fat
					((Checkbox) nonBlockDialog.getCheckboxes().get(2)).addItemListener(new ItemListener()
					{
						@Override
						public void itemStateChanged(final ItemEvent e)
						{
							if (e.getStateChange() == 1)
							{
								for (final FatBlob blob : FatBlob_Detector.this.selectedRois)
								{
									if (!blob.isFat())
									{
										setIsFat(blob, true);
									}
								}
							}
							FatBlob_Detector.this.currentImage.updateAndDraw();
						}
					});

					// Add a listener to set every 'selected' blob to non-fat tissue
					((Checkbox) nonBlockDialog.getCheckboxes().get(3)).addItemListener(new ItemListener()
					{
						@Override
						public void itemStateChanged(final ItemEvent e)
						{
							if (e.getStateChange() == 1)
							{
								for (final FatBlob blob : FatBlob_Detector.this.selectedRois)
								{
									if (blob.isFat())
									{
										setIsFat(blob, false);
									}
								}
							}
							FatBlob_Detector.this.currentImage.updateAndDraw();
						}
					});
					nonBlockDialog.showDialog();

					if (nonBlockDialog.wasCanceled())
					{
						return null;
					}

					if (!nonBlockDialog.wasOKed())
					{
						// Must be "no" then
						return new double[0];
					}

					Prefs.set(DIALOG_X, nonBlockDialog.getX());
					Prefs.set(DIALOG_Y, nonBlockDialog.getY());

					nonBlockDialog.dispose();
				}

				final List<Double[]> fatList = aFatAreaSizes.get(0);
				final List<Double[]> tissueList = aFatAreaSizes.get(1);
				for (final FatBlob blob : this.selectedRois)
				{
					final double blobArea = blob.getArea();
					final Double[] resultSet = { blobArea, aRoiNumber, blob.getCircMeasure(CIRCULAR_MEASURES[0]), blob.getCircMeasure(CIRCULAR_MEASURES[1]), blob.getCircMeasure(CIRCULAR_MEASURES[2]),
							blob.getCircMeasure(CIRCULAR_MEASURES[3]) };
					if (blob.isFat())
					{
						fatList.add(resultSet);
						totalAreaBlob += blobArea;
					}
					else
					{
						totalAreaNonBlob += blobArea;
						tissueList.add(resultSet);
					}
				}

				final List<Double[]> backgroundList = aFatAreaSizes.get(2);
				for (final FatBlob blob : this.deletedRois)
				{
					final double blobArea = blob.getArea();
					final Double[] resultSet = { blobArea, aRoiNumber, blob.getCircMeasure(CIRCULAR_MEASURES[0]), blob.getCircMeasure(CIRCULAR_MEASURES[1]), blob.getCircMeasure(CIRCULAR_MEASURES[2]),
							blob.getCircMeasure(CIRCULAR_MEASURES[3]) };
					backgroundList.add(resultSet);
				}

				totalArea = (this.currentImage.getWidth() * this.currentImage.getHeight() * this.currentImage.getCalibration().pixelWidth * this.currentImage.getCalibration().pixelHeight)
						- (totalAreaRoi - totalAreaBlob - totalAreaNonBlob);

				this.currentImage.setOverlay(null);
			}

			if (ResultsTable.getResultsTable() != null)
			{
				ResultsTable.getResultsWindow().close(false);
			}
		}

		Prefs.savePreferences();

		final double[] result = { totalArea, totalAreaBlob };
		return result;
	}


	public void setFatRoi(final int x, final int y, final boolean aIsFat)
	{
		for (final FatBlob blob : this.selectedRois)
		{
			final Roi roi = blob.getRoi();
			if (roi.contains(x, y))
			{
				if (blob.isFat() != aIsFat)
				{
					setIsFat(blob, aIsFat);
					this.currentImage.killRoi();
					this.currentImage.updateAndDraw();
				}

				break;
			}
		}
	}


	private void setIsFat(final FatBlob aBlob, final boolean aIsFat)
	{
		aBlob.setIsFat(aIsFat);
		final Roi roi = aBlob.getRoi();
		this.currentOverlay.remove(roi);
		final Roi newRoi = (Roi) roi.clone();
		if (this.fillRoi)
		{
			newRoi.setFillColor(aIsFat ? FAT_COLOUR : TISSUE_COLOUR);
		}
		else
		{
			newRoi.setFillColor(null);
		}
		newRoi.setStrokeColor(aIsFat ? FAT_COLOUR : TISSUE_COLOUR);
		aBlob.setRoi(newRoi);
		this.currentOverlay.add(newRoi);
	}
}
