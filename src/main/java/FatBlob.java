import java.util.HashMap;
import java.util.Map;

import ij.gui.Roi;

public class FatBlob
{
	private Roi roi;
	private final double area;
	private boolean isFat;
	private final Map<String, Double> circularMeasurements;


	public FatBlob(final Roi aRoi, final double aArea)
	{
		this.roi = aRoi;
		this.area = aArea;
		this.isFat = true;
		this.circularMeasurements = new HashMap<>();
	}


	public void addCircMeasure(final String aName, final Double aValue)
	{
		this.circularMeasurements.put(aName, aValue);
	}


	public double getArea()
	{
		return this.area;
	}


	public Double getCircMeasure(final String aName)
	{
		return this.circularMeasurements.get(aName);
	}


	public Roi getRoi()
	{
		return this.roi;
	}


	public boolean isFat()
	{
		return this.isFat;
	}


	public void setIsFat(final boolean aIsFat)
	{
		this.isFat = aIsFat;
	}


	public void setRoi(final Roi aRoi)
	{
		this.roi = aRoi;
	}
}
