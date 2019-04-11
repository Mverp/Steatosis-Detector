import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ij.IJ;

public class PmaCoreClient
{
	private String pmaCoreServerUrl;


	public PmaCoreClient(final String serverUrl)
	{
		this.pmaCoreServerUrl = serverUrl.toLowerCase();

		if ((!this.pmaCoreServerUrl.startsWith("http://")) && (!this.pmaCoreServerUrl.startsWith("https://")))
		{
			this.pmaCoreServerUrl = ("http://" + this.pmaCoreServerUrl);
		}

		if (this.pmaCoreServerUrl.endsWith("api"))
		{
			this.pmaCoreServerUrl += "/xml/";
		}
		else if (this.pmaCoreServerUrl.endsWith("api/"))
		{
			this.pmaCoreServerUrl += "soap";
		}
		else if ((!this.pmaCoreServerUrl.endsWith("api/xml")) && (!this.pmaCoreServerUrl.endsWith("api/xml/")))
		{
			if (!this.pmaCoreServerUrl.endsWith("/"))
			{
				this.pmaCoreServerUrl += "/";
			}

			this.pmaCoreServerUrl += "api/xml/";
		}
	}


	public boolean checkConnection()
	{
		HttpURLConnection connection = null;
		try
		{
			final URL url = new URL(this.pmaCoreServerUrl);

			connection = (HttpURLConnection) url.openConnection();
			connection.connect();
		}
		catch (final IOException e)
		{
			if (connection != null)
			{
				connection.disconnect();
			}
			return false;
		}

		connection.disconnect();
		return true;
	}


	public String[] getDirectories(final String path)
	{
		try
		{
			final Document document = GetRequest(this.pmaCoreServerUrl + "GetDirectories?path=" + URLEncoder.encode(path, "UTF-8") + "&sessionID=pma.core.lite");
			final Element docEl = document.getDocumentElement();

			final NodeList directories = docEl.getChildNodes();
			final String[] result = new String[directories.getLength()];
			for (int i = 0; i < result.length; i++)
			{
				result[i] = directories.item(i).getTextContent();
			}

			return result;
		}
		catch (final IOException e)
		{
			IJ.error("PMA.core.lite not running!");
			return null;
		}
		catch (final ParserConfigurationException e)
		{
			IJ.error("PMA.core.lite not running!");
			return null;
		}
		catch (final SAXException e)
		{
			IJ.error("PMA.core.lite not running!");
		}
		return null;
	}


	public String[] getFiles(final String path)
	{
		try
		{
			final Document document = GetRequest(this.pmaCoreServerUrl + "GetFiles?path=" + URLEncoder.encode(path, "UTF-8") + "&sessionID=pma.core.lite");
			final Element docEl = document.getDocumentElement();

			final NodeList files = docEl.getChildNodes();
			final String[] result = new String[files.getLength()];
			for (int i = 0; i < result.length; i++)
			{
				result[i] = files.item(i).getTextContent();
			}

			return result;
		}
		catch (final IOException e)
		{
			IJ.error("PMA.core.lite not running!");
			return null;
		}
		catch (final ParserConfigurationException e)
		{
			IJ.error("PMA.core.lite not running!");
			return null;
		}
		catch (final SAXException e)
		{
			IJ.error("PMA.core.lite not running!");
		}
		return null;
	}


	public ImageInfo getImageInfo(final String path)
	{
		try
		{
			final Document document = GetRequest(this.pmaCoreServerUrl + "GetImageInfo?pathOrUid=" + URLEncoder.encode(path, "UTF-8") + "&sessionID=pma.core.lite");
			return new ImageInfo(document.getDocumentElement());
		}
		catch (final IOException e)
		{
			IJ.error("PMA.core.lite not running!");
			return null;
		}
		catch (final ParserConfigurationException e)
		{
			IJ.error("PMA.core.lite not running!");
			return null;
		}
		catch (final SAXException e)
		{
			IJ.error("PMA.core.lite not running!");
		}
		return null;
	}


	private Document GetRequest(final String aURL) throws IOException, ParserConfigurationException, SAXException
	{
		HttpURLConnection connection = null;
		try
		{
			final URL url = new URL(aURL);

			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(connection.getInputStream());
		}
		finally
		{
			if (connection != null)
			{
				connection.disconnect();
			}
		}
	}
}