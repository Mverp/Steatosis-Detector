import org.w3c.dom.Node;

public class ImageLayer
{
	private final Channel[] channels;


	public ImageLayer(final Node node)
	{
		final org.w3c.dom.NodeList nodes = node.getFirstChild().getChildNodes();
		this.channels = new Channel[nodes.getLength()];

		for (int i = 0; i < this.channels.length; i++)
		{
			this.channels[i] = new Channel(nodes.item(i));
		}
	}


	public Channel[] getChannels()
	{
		return this.channels;
	}
}