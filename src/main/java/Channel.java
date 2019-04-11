import org.w3c.dom.Node;

public class Channel
{
	private int channelID;
	private String channelName;


	public Channel(final Node node)
	{
		final org.w3c.dom.NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			final Node n = children.item(i);
			if (n.getNodeName() == "ChannelID")
			{
				this.channelID = Integer.parseInt(n.getTextContent());
			}
			else if (n.getNodeName() == "Name")
			{
				this.channelName = n.getTextContent();
			}
		}
	}


	public Integer getChannelID()
	{
		return Integer.valueOf(this.channelID);
	}


	public String getName()
	{
		return this.channelName;
	}
}