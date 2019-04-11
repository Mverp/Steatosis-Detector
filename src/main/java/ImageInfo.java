import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ImageInfo
{
	private final int width;
	private final int height;
	private double mppX;
	private double mppY;
	private final String baseUrl;
	private final String fileName;
	private final TimeFrame[] timeFrames;


	public ImageInfo(final Element element)
	{
		final java.text.DecimalFormat df = new java.text.DecimalFormat();
		final java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		symbols.setGroupingSeparator(' ');
		df.setDecimalFormatSymbols(symbols);

		this.width = Integer.parseInt(element.getElementsByTagName("Width").item(0).getTextContent());
		this.height = Integer.parseInt(element.getElementsByTagName("Height").item(0).getTextContent());
		try
		{
			this.mppX = df.parse(element.getElementsByTagName("MicrometresPerPixelX").item(0).getTextContent()).doubleValue();
			this.mppY = df.parse(element.getElementsByTagName("MicrometresPerPixelY").item(0).getTextContent()).doubleValue();
		}
		catch (final org.w3c.dom.DOMException e)
		{
			e.printStackTrace();
		}
		catch (final java.text.ParseException e)
		{
			e.printStackTrace();
		}

		this.baseUrl = element.getElementsByTagName("BaseUrl").item(0).getTextContent();
		this.fileName = element.getElementsByTagName("Filename").item(0).getTextContent();

		final NodeList tf = element.getElementsByTagName("TimeFrames").item(0).getChildNodes();
		this.timeFrames = new TimeFrame[tf.getLength()];
		for (int i = 0; i < this.timeFrames.length; i++)
		{
			this.timeFrames[i] = new TimeFrame(tf.item(i));
		}
	}


	public String getBaseUrl()
	{
		return this.baseUrl;
	}


	public String getFilename()
	{
		return this.fileName;
	}


	public int getHeight()
	{
		return this.height;
	}


	public double getMicrometresPerPixelX()
	{
		return this.mppX;
	}


	public double getMicrometresPerPixelY()
	{
		return this.mppY;
	}


	public TimeFrame[] getTimeFrames()
	{
		return this.timeFrames;
	}


	public int getWidth()
	{
		return this.width;
	}
}