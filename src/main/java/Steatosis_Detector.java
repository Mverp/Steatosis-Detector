import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Steatosis_Detector implements PlugIn
{
	public static class MyItemListener implements ItemListener
	{
		private Label m_lb = null;
		private final Double w;
		private final Double h;
		private Double scalesmall = Double.valueOf(1.0D);
		private final Double scalemedium;

		private final Double scalelarge;


		MyItemListener(final Label lb, final double width, final double height, final Double Scalesmall, final Double Scalemedium, final Double Scalelarge)
		{
			this.m_lb = lb;
			this.w = Double.valueOf(width);
			this.h = Double.valueOf(height);
			this.scalesmall = Scalesmall;
			this.scalemedium = Scalemedium;
			this.scalelarge = Scalelarge;
		}


		@Override
		public void itemStateChanged(final ItemEvent e)
		{
			Double scale = Double.valueOf(1.0D);
			Double w1 = Double.valueOf(0.0D);
			Double h1 = Double.valueOf(0.0D);

			final String label = (String) e.getItem();
			if (label == "Small")
			{
				scale = this.scalesmall;
			}
			else if (label == "Medium")
			{
				scale = this.scalemedium;
			}
			else if (label == "Large")
			{
				scale = this.scalelarge;
			}

			if (scale.doubleValue() != 0.0D)
			{
				w1 = Double.valueOf(scale.doubleValue() * this.w.doubleValue());
				h1 = Double.valueOf(this.h.doubleValue() * scale.doubleValue());
			}
			final NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(0);
			nf.setGroupingUsed(false);
			nf.setParseIntegerOnly(true);
			final String value = nf.format(w1) + "x" + nf.format(h1);
			this.m_lb.setText(value);
		}
	}

	private static final double WORKING_ZOOM = 1.0; // The zoom level on which the working section will be shown. 1.0 is completely zoomed in and zooming out is done by dividing. E.g. 0.5 is 2x zoomed out.
	private static final int CHUNK_SIZE = (int) (3000 / WORKING_ZOOM); // This is the pixel size of the working sections based on the zoom level of 1. The numerator is the actual pixel size of the working section.
	private static final String IMAGE_DIR = "Steatosis Images";


	// private static final String SAVE_EXTENSION = "_savedWork.txt";
	// private static final String END_OF_TEMP_FILE = "EndOfTheFile";

	public static double getFittingScale(final int aWidth, final int aHeight)
	{
		// Get the largest dimension of the image
		long maxDimension = aHeight;
		if (aWidth > aHeight)
		{
			maxDimension = aWidth;
		}

		double scale = (1000.0D / maxDimension);
		if (scale > 1.0D)
		{
			scale = 1.0D;
		}

		return scale;
	}

	public boolean selectionMade;
	private String pathoruid;

	private ImageInfo imageInfo;
	private PmaCoreClient client;

	private Roi cropRegion = null;

	private final int finalChannel = 0;

	private double scalePreview = 0.0D;


	private void addWindowListener(final ImageWindow aImageWindow)
	{
		aImageWindow.addWindowListener(new WindowListener()
		{
			@Override
			public void windowActivated(final WindowEvent e)
			{
			}


			@Override
			public void windowClosed(final WindowEvent e)
			{
			}


			@Override
			public void windowClosing(final WindowEvent e)
			{
			}


			@Override
			public void windowDeactivated(final WindowEvent e)
			{
			}


			@Override
			public void windowDeiconified(final WindowEvent e)
			{
			}


			@Override
			public void windowIconified(final WindowEvent e)
			{
			}


			@Override
			public void windowOpened(final WindowEvent e)
			{
			}
		});
	}


	public void continueAfterRoi(final Rectangle aCurrentRoi, final Double[] aCurrentCounts, final boolean aAutomatic, final String aFileName, final String aDir)
	{
		final int selectionWidth = (int) (this.cropRegion.getBounds().width / this.scalePreview);
		final int selectionHeight = (int) (this.cropRegion.getBounds().height / this.scalePreview);
		final int realX = (int) (this.cropRegion.getBounds().x / this.scalePreview);
		final int realY = (int) (this.cropRegion.getBounds().y / this.scalePreview);
		final int endX = realX + selectionWidth;
		final int endY = realY + selectionHeight;
		boolean cont = true;
		double totalTissueArea = 0;
		double totalFatArea = 0;

		final double scale = getFittingScale(selectionWidth, selectionHeight);
		final ImagePlus selectionImage = fetchImage((int) (realX * scale), (int) (realY * scale), selectionWidth * scale, selectionHeight * scale, scale, Integer.valueOf(this.finalChannel),
				this.imageInfo.getFilename() + " - Pathomation v1.0");
		saveImage(selectionImage, aFileName, aDir);
		final ImagePlus thresholdImage = BackgroundRemover.removeBackground(selectionImage);
		selectionImage.close();

		thresholdImage.show();

		final List<List<Double[]>> fatAreaSizes = new ArrayList<>();
		for (int i = 0; i < 3; i++)
		{
			fatAreaSizes.add(new ArrayList<Double[]>());
		}
		final ArrayList<Double[]> tissueAreaSizes = new ArrayList<>();

		final FatBlob_Detector detector = new FatBlob_Detector();

		if (!IJ.setTool(Roi_Removal_Tool.NAME))
		{
			Toolbar.addPlugInTool(new Roi_Removal_Tool());
			IJ.setTool(Roi_Removal_Tool.NAME);
		}
		Roi_Removal_Tool.setDetector(detector);

		int startY = realY;
		int startX = 0;
		int y = Math.min(CHUNK_SIZE, endY - startY + 1);
		int x = 0;
		double[] measurements = null;
		Double chunkNr = 1.0;

		// Filename for results (placed here for the autosave name)
		String fileName = this.imageInfo.getFilename();
		fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));
		final String filenameTemp = fileName + "_tmp";

		while (startY <= endY && cont)
		{
			startX = realX;
			x = Math.min(CHUNK_SIZE, endX - startX + 1);
			while (startX <= endX && cont)
			{
				setSelectionMask(thresholdImage, startX - realX, startY - realY, x, y, scale);

				final int[] maskHist = thresholdImage.getProcessor().getHistogram();
				if (maskHist[1] != 0 || maskHist[maskHist.length - 1] != 0) // If the mask contains any non-background pixels
				{
					final ImagePlus impfinal = fetchImage((int) (startX * WORKING_ZOOM), (int) (startY * WORKING_ZOOM), (int) (x * WORKING_ZOOM), (int) (y * WORKING_ZOOM), WORKING_ZOOM,
							Integer.valueOf(this.finalChannel), this.imageInfo.getFilename() + " - Pathomation v1.0");

					impfinal.show();
					IJ.selectWindow(impfinal.getID());

					measurements = detector.run(fatAreaSizes, chunkNr, aAutomatic);
					if (measurements == null || measurements.length == 0)
					{
						cont = false;
					}
					else
					{
						final Double[] tissueArea = { chunkNr, measurements[0] };
						tissueAreaSizes.add(tissueArea);
						totalTissueArea += measurements[0];
						totalFatArea += measurements[1];
					}

					impfinal.close();

					// Autosave results
					if (measurements != null)
					{
						final double percFatArea = (totalFatArea / totalTissueArea) * 100;
						writeToFile(totalTissueArea, totalFatArea, percFatArea, fatAreaSizes, tissueAreaSizes, filenameTemp, aDir);
					}
				}

				if (cont)
				{
					startX = Math.min(startX + CHUNK_SIZE + 1, endX + 1);
					x = Math.min(CHUNK_SIZE, endX - startX + 1);
				}

				chunkNr = chunkNr + 1.0;
			}

			if (cont)
			{
				startY = Math.min(startY + CHUNK_SIZE + 1, endY + 1);
				y = Math.min(CHUNK_SIZE, endY - startY + 1);
			}

		}

		// IJ.log("Total area : " + totalTissueArea + " micrometer^2\nTotal fat area of : " + totalFatArea + " micrometer^2\nFat percentage : " + percFatArea + "%");
		// int areaNr = 1;
		// for (final Double area : fatAreaSizes)
		// {
		// IJ.log("Area " + areaNr + " has a surface of " + area + " micrometer^2");
		// areaNr++;
		// }

		if (measurements != null)
		{
			final double percFatArea = (totalFatArea / totalTissueArea) * 100;
			writeToFile(totalTissueArea, totalFatArea, percFatArea, fatAreaSizes, tissueAreaSizes, fileName, aDir);
		}

		thresholdImage.changes = false;
		thresholdImage.close();
		Roi_Removal_Tool.setDetector(null);
	}


	private void createSelectionWindow(final ImagePlus aImageCrop)
	{
		final ImageWindow imageWindow = new ImageWindow(aImageCrop);
		imageWindow.running = true;

		final Panel panel = new Panel(new FlowLayout(1));
		final Button okButton = new Button("    OK    ");
		final Button cancelButton = new Button("Cancel");
		imageWindow.add(new Label("Use the rectangle tool to optionally select an area"));
		panel.add(okButton);
		panel.add(cancelButton);

		imageWindow.add(panel);
		imageWindow.invalidate();
		imageWindow.validate();

		imageWindow.setSize(Math.max(panel.getWidth(), imageWindow.getWidth()), imageWindow.getHeight() + 60);
		imageWindow.validate();

		okButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				imageWindow.close();
				Steatosis_Detector.this.selectionMade = true;
			}

		});

		cancelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				imageWindow.close();
			}

		});

		imageWindow.getCanvas().addMouseMotionListener(new MouseMotionListener()
		{
			@Override
			public void mouseDragged(final MouseEvent arg0)
			{
				update();
			}


			@Override
			public void mouseMoved(final MouseEvent e)
			{
			}


			private void update()
			{
				if (imageWindow != null)
				{
					Steatosis_Detector.this.cropRegion = imageWindow.getImagePlus().getRoi();
				}
			}

		});

		IJ.setTool(Toolbar.RECTANGLE);
		addWindowListener(imageWindow);
	}


	public ImagePlus fetchImage(final int aXCoordinate, final int aYCoordinate, final double aWidth, final double aHeight, final double aScale, final Integer aChannelID, final String aTitle)
	{
		StringBuffer url = null;
		int x = aXCoordinate;
		int y = aYCoordinate;
		double fwidth = aWidth;
		double fheight = aHeight;

		boolean isFirst = true;
		ImagePlus impfinal = null;
		final int max = ((int) (fwidth / 2500.0D) + 1) * ((int) (fheight / 2500.0D) + 1);
		int progressi = 0;
		while ((int) fwidth > 0)
		{
			while ((int) fheight > 0)
			{
				final int tempwidth = fwidth < 2500.0D ? (int) (fwidth / aScale) : (int) (2500.0D / aScale);
				final int tempheight = fheight < 2500.0D ? (int) (fheight / aScale) : (int) (2500.0D / aScale);
				try
				{
					url = new StringBuffer(this.imageInfo.getBaseUrl() + "Region?drawScaleBar=false&sessionID=pma.view.lite" + "&PathOrUid=" + URLEncoder.encode(this.pathoruid, "UTF-8")
							+ "&timeframe=0&channels=" + aChannelID.toString() + "&layer=0&x=" + (int) (x / aScale) + "&y=" + (int) (y / aScale) + "&width=" + String.valueOf(tempwidth) + "&height="
							+ String.valueOf(tempheight) + "&scale=");
					url = url.append(aScale);
				}
				catch (final UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}

				final ImagePlus imp = new ImagePlus(url.toString());
				IJ.showProgress(progressi, max);

				if (isFirst)
				{
					final ImageProcessor ip = imp.getProcessor().createProcessor((int) fwidth, (int) fheight);
					impfinal = new ImagePlus(aTitle, ip);
					isFirst = false;
					impfinal.show();
				}
				impfinal.draw();
				impfinal.getProcessor().insert(imp.getProcessor(), x - aXCoordinate, y - aYCoordinate);
				y += 2500;
				fheight -= 2500.0D;
				progressi++;
			}
			fheight = aHeight;
			y = aYCoordinate;
			x += 2500;
			fwidth -= 2500.0D;
		}
		IJ.showProgress(progressi, max);
		impfinal.getCalibration().setUnit("um");
		impfinal.getCalibration().pixelWidth = (this.imageInfo.getMicrometresPerPixelX() / aScale);
		impfinal.getCalibration().pixelHeight = (this.imageInfo.getMicrometresPerPixelY() / aScale);

		return impfinal;
	}


	private void loadRoi(final String aFileName, final String aDir)
	{
		String filePath = Prefs.get("LiverSteatosis.fileDir", null);
		if (aDir.isEmpty())
		{
			filePath += File.separator + IMAGE_DIR + File.separator + aFileName;
		}
		else
		{
			filePath = aDir + File.separator + IMAGE_DIR + File.separator + aFileName;
		}
		final File roiFile = new File(filePath);
		if (roiFile.exists())
		{
			this.cropRegion = RoiDecoder.open(filePath);
		}
		else
		{
			this.cropRegion = null;
		}
	}


	// private List<List<Double>> readTempFromFile(final String aFileName)
	// {
	// String filePath = Prefs.get("LiverSteatosis.fileDir", null);
	// filePath += File.separator + "Steatosis results";
	// final File dir = new File(filePath);
	// final List<List<Double>> result = new ArrayList<>();
	// if (dir.exists())
	// {
	// filePath += File.separator + aFileName + SAVE_EXTENSION;
	// final File correctionsFile = new File(filePath);
	// try
	// {
	// final FileReader reader = new FileReader(correctionsFile);
	// final BufferedReader buffer = new BufferedReader(reader);
	// final int selX = Double.valueOf(buffer.readLine()).intValue();
	// final int selY = Double.valueOf(buffer.readLine()).intValue();
	// final int selWidth = Double.valueOf(buffer.readLine()).intValue();
	// final int selHeight = Double.valueOf(buffer.readLine()).intValue();
	// final Double curX = Double.valueOf(buffer.readLine());
	// final Double curY = Double.valueOf(buffer.readLine());
	// final Double curWidth = Double.valueOf(buffer.readLine());
	// final Double curHeight = Double.valueOf(buffer.readLine());
	// final Double tissueAreaTotal = Double.valueOf(buffer.readLine());
	// final int nrFatAreas = Double.valueOf(buffer.readLine()).intValue();
	// final Double fatAreaTotal = Double.valueOf(buffer.readLine());
	//
	// this.cropRegion = new Roi(new Rectangle(selX, selY, selWidth, selHeight));
	// final Double[] curTotals = { curX, curY, curWidth, curHeight, tissueAreaTotal, fatAreaTotal };
	// result.add(Arrays.asList(curTotals));
	//
	// final List<Double> fatAreaSizes = new ArrayList<>();
	// for (int i = 1; i <= nrFatAreas; i++)
	// {
	// final Double area = Double.valueOf(buffer.readLine());
	// fatAreaSizes.add(area);
	// }
	//
	// result.add(fatAreaSizes);
	//
	// buffer.close();
	// }
	// catch (final FileNotFoundException e)
	// {
	// // No such file exists, no problem.
	// return null;
	// }
	// catch (final IOException e)
	// {
	// // Oops, that isn't right.
	// IJ.handleException(e);
	// }
	// }
	//
	// return result;
	// }

	@Override
	public void run(final String arg)
	{
		// Connect to server via standard port
		if (this.client == null)
		{
			this.client = new PmaCoreClient("http://localhost:54001/");
			if (!this.client.checkConnection())
			{
				IJ.error("No connection", "Cannot get a connection to the Pathomation server.");
				return;
			}
		}

		if (Prefs.get(FatBlob_Detector.MIN_THRESHOLD_AREA, -1) == -1)
		{
			IJ.error("Settings missing", "No settings found. Please run Steatosis Detector settings tool first.");
		}

		// Adjust look and feel
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (final Exception e1)
		{
			e1.printStackTrace();
		}

		final String startDir = Prefs.get("LiverSteatosis.fileDir", null);
		final JFileChooser chooser = new JFileChooser(startDir);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		final int returnVal = chooser.showDialog(IJ.getInstance(), "Open");

		// If a file has been chosen, handle it
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			// Get file
			final File file = chooser.getSelectedFile();
			Prefs.set("LiverSteatosis.fileDir", file.getParent());
			Prefs.savePreferences();

			List<File> files;
			String dirName = "";
			if (file.isDirectory())
			{
				files = Arrays.asList(file.listFiles());
				dirName = file.getAbsolutePath();
			}
			else
			{
				files = new ArrayList<>();
				files.add(file);
			}

			for (final File currentFile : files)
			{
				if (!currentFile.isDirectory() && currentFile.getName().endsWith(".ndpi"))
				{
					// Get UID for file
					this.pathoruid = currentFile.getPath().replace("\\", "/");
					final int i = this.pathoruid.indexOf("/");
					final String stationName = FileSystemView.getFileSystemView().getSystemDisplayName(new File(this.pathoruid.substring(0, i) + File.separator));
					this.pathoruid = stationName + "/" + this.pathoruid.substring(i + 1, this.pathoruid.length());

					// Get file info
					this.imageInfo = this.client.getImageInfo(this.pathoruid);
					if (this.imageInfo == null)
					{
						IJ.error(this.pathoruid + "  Cannot get image info!");
						return;
					}

					this.scalePreview = getFittingScale(this.imageInfo.getWidth(), this.imageInfo.getHeight());

					String fileName = this.imageInfo.getFilename();
					fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));

					loadRoi(fileName + ".roi", dirName); // Check if a roi has already been stored
					if (this.cropRegion == null)
					{
						final ImagePlus impCrop = fetchImage(0, 0, this.imageInfo.getWidth() * this.scalePreview, this.imageInfo.getHeight() * this.scalePreview, this.scalePreview, Integer.valueOf(0),
								"Select a cropping region - Pathomation v1.0");

						this.selectionMade = false;
						createSelectionWindow(impCrop);

						while (!this.selectionMade)
						{
							IJ.wait(500);
						}

						saveRoi(fileName + ".roi", dirName);
					}

					continueAfterRoi(null, null, arg.equals("Automated"), fileName, dirName);
				}
			}
		}
	}


	private void saveImage(final ImagePlus aImage, final String aFileName, final String aDir)
	{
		String filePath = Prefs.get("LiverSteatosis.fileDir", null);
		if (aDir.isEmpty())
		{
			filePath += File.separator + IMAGE_DIR;
		}
		else
		{
			filePath = aDir + File.separator + IMAGE_DIR;
		}
		final File dir = new File(filePath);
		if (!dir.exists())
		{
			try
			{
				dir.mkdir();
			}
			catch (final SecurityException se)
			{
				IJ.handleException(se);
			}
		}

		filePath += File.separator + aFileName;
		if (!(new File(filePath + ".tif")).exists())
		{
			IJ.saveAsTiff(aImage, filePath);
		}
	}


	private void saveRoi(final String aFileName, final String aDir)
	{
		String filePath = Prefs.get("LiverSteatosis.fileDir", null);
		if (aDir.isEmpty())
		{
			filePath += File.separator + IMAGE_DIR;
		}
		else
		{
			filePath = aDir + File.separator + IMAGE_DIR;
		}
		final File dir = new File(filePath);
		if (!dir.exists())
		{
			try
			{
				dir.mkdir();
			}
			catch (final SecurityException se)
			{
				IJ.handleException(se);
			}
		}

		RoiEncoder.save(this.cropRegion, filePath + File.separator + aFileName);
	}


	private void setSelectionMask(final ImagePlus aMaskImage, final int aXStart, final int aYStart, final int aWidth, final int aHeight, final double aScale)
	{
		final int xStart = (int) (aXStart * aScale);
		final int yStart = (int) (aYStart * aScale);
		final int width = (int) (aWidth * aScale);
		final int height = (int) (aHeight * aScale);

		final Roi roi = new Roi(xStart, yStart, width, height);
		roi.setStrokeColor(Color.RED);
		aMaskImage.setRoi(roi);
	}


	private void writeToFile(final double aTissueAreaTotal, final double aFatAreaTotal, final double aPercentageFatArea, final List<List<Double[]>> aFatAreaSizes,
			final ArrayList<Double[]> aTissueAreaSizes, final String aFileName, final String aDir)
	{
		PrintWriter resultsFile;
		String filePath = Prefs.get("LiverSteatosis.fileDir", null);
		if (aDir.isEmpty())
		{
			filePath += File.separator + "Steatosis results";
		}
		else
		{
			filePath = aDir + File.separator + "Steatosis results";
		}

		final File dir = new File(filePath);
		if (!dir.exists())
		{
			try
			{
				dir.mkdir();
			}
			catch (final SecurityException se)
			{
				IJ.handleException(se);
			}
		}

		filePath += File.separator + aFileName + ".xls";
		try
		{
			final List<Double[]> fatList = aFatAreaSizes.get(0);
			final List<Double[]> tissueList = aFatAreaSizes.get(1);
			final List<Double[]> backgroundList = aFatAreaSizes.get(2);

			resultsFile = new PrintWriter(filePath);
			resultsFile.append("Total tissue area in micrometers\t" + aTissueAreaTotal + "\n");
			resultsFile.append("Number of fat areas\t" + fatList.size() + "\n");
			resultsFile.append("Number of non-fat white tissue areas\t" + tissueList.size() + "\n");
			resultsFile.append("Number of background areas\t" + backgroundList.size() + "\n");
			resultsFile.append("Total fat area in micrometers\t" + aFatAreaTotal + "\n");
			resultsFile.append("Percentage fat are of total tissue\t" + aPercentageFatArea + "\n\n\n");
			resultsFile.append("Fat area nr\tArea size in micrometerssquared\tSelection area nr");
			for (int feat = 0; feat < FatBlob_Detector.CIRCULAR_MEASURES.length; feat++)
			{
				resultsFile.append("\t" + FatBlob_Detector.CIRCULAR_MEASURES[feat]);
			}
			resultsFile.append("\n");

			for (int i = 1; i <= fatList.size(); i++)
			{
				resultsFile.append("" + i);
				for (int feat = 0; feat < 2 + FatBlob_Detector.CIRCULAR_MEASURES.length; feat++)
				{
					resultsFile.append("\t" + fatList.get(i - 1)[feat]);
				}
				resultsFile.append("\n");
			}

			resultsFile.append("\n\nNon-fat white tissue area nr\tArea size in micrometerssquared\tSelection area nr");
			for (int feat = 0; feat < FatBlob_Detector.CIRCULAR_MEASURES.length; feat++)
			{
				resultsFile.append("\t" + FatBlob_Detector.CIRCULAR_MEASURES[feat]);
			}
			resultsFile.append("\n");
			for (int i = 1; i <= tissueList.size(); i++)
			{
				resultsFile.append("" + i);
				for (int feat = 0; feat < 2 + FatBlob_Detector.CIRCULAR_MEASURES.length; feat++)
				{
					resultsFile.append("\t" + tissueList.get(i - 1)[feat]);
				}
				resultsFile.append("\n");
			}

			resultsFile.append("\n\nBackground area nr\tArea size in micrometerssquared\tSelection area nr");
			for (int feat = 0; feat < FatBlob_Detector.CIRCULAR_MEASURES.length; feat++)
			{
				resultsFile.append("\t" + FatBlob_Detector.CIRCULAR_MEASURES[feat]);
			}
			resultsFile.append("\n");
			for (int i = 1; i <= backgroundList.size(); i++)
			{
				resultsFile.append("" + i);
				for (int feat = 0; feat < 2 + FatBlob_Detector.CIRCULAR_MEASURES.length; feat++)
				{
					resultsFile.append("\t" + backgroundList.get(i - 1)[feat]);
				}
				resultsFile.append("\n");
			}

			resultsFile.append("\n\nSelection nr\tTissue area size in micrometers squared\n");
			for (int i = 1; i <= aTissueAreaSizes.size(); i++)
			{
				final Double[] tissueSize = aTissueAreaSizes.get(i - 1);
				resultsFile.append(tissueSize[0] + "\t" + tissueSize[1] + "\n");
			}

			resultsFile.append("\n\nSetting\tValue\n");
			resultsFile.append("Minimum area\t" + Prefs.get(FatBlob_Detector.MIN_THRESHOLD_AREA, -1.0) + "\n");
			resultsFile.append("Maximum area\t" + Prefs.get(FatBlob_Detector.MAX_THRESHOLD_AREA, -1.0) + "\n");
			resultsFile.append("Minimum saturation\t" + Prefs.get(FatBlob_Detector.SAT_THRESHOLD, -1.0) + "\n");
			resultsFile.append("Use core saturation\t" + Prefs.get(FatBlob_Detector.USE_CORE_SAT, true) + "\n");

			resultsFile.close();
		}
		catch (final FileNotFoundException aFnFEx)
		{
			IJ.handleException(aFnFEx);
		}
	}
}