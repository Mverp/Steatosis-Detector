import java.awt.event.MouseEvent;

import ij.ImagePlus;
import ij.plugin.tool.PlugInTool;

public class Roi_Removal_Tool extends PlugInTool
{
	public static final String NAME = "RRT";
	private static FatBlob_Detector detector;


	/**
	 * @return the detector
	 */
	public static FatBlob_Detector getDetector()
	{
		return detector;
	}


	/**
	 * Sets the FatBlob_Detector
	 *
	 * @param aDetector
	 *            the detector to set
	 */
	public static void setDetector(final FatBlob_Detector aDetector)
	{
		Roi_Removal_Tool.detector = aDetector;
	}


	@Override
	public String getToolName()
	{
		return NAME;
	}


	@Override
	public void mouseClicked(final ImagePlus aImage, final MouseEvent e)
	{
		final int button = e.getButton();
		final int modifiers = e.getModifiersEx();

		final int x = aImage.getCanvas().offScreenX(e.getX());
		final int y = aImage.getCanvas().offScreenY(e.getY());

		if (button == MouseEvent.BUTTON1)
		{
			if ((modifiers & MouseEvent.SHIFT_DOWN_MASK) != 0)
			{
				detector.addRoi(x, y);
			}
			else if ((modifiers & MouseEvent.ALT_DOWN_MASK) != 0)
			{
				detector.removeRoi(x, y);
			}
			else if ((modifiers & MouseEvent.CTRL_DOWN_MASK) != 0)
			{
				detector.setFatRoi(x, y, true);
			}
			else
			{
				detector.setFatRoi(x, y, false);
			}
		}
	}


	@Override
	public void mouseDragged(final ImagePlus aImage, final MouseEvent e)
	{
		final int modifiers = e.getModifiersEx();
		if (modifiers == MouseEvent.BUTTON1_DOWN_MASK)
		{
			final int x = aImage.getCanvas().offScreenX(e.getX());
			final int y = aImage.getCanvas().offScreenY(e.getY());
			detector.setFatRoi(x, y, false);
		}
	}
}
