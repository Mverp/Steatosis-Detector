import org.w3c.dom.Node;

public class TimeFrame
{
	private final ImageLayer[] layers;


	public TimeFrame(final Node aNode)
	{
		final org.w3c.dom.NodeList nodes = aNode.getFirstChild().getChildNodes();
		this.layers = new ImageLayer[nodes.getLength()];

		for (int i = 0; i < this.layers.length; i++)
		{
			this.layers[i] = new ImageLayer(nodes.item(i));
		}
	}


	public ImageLayer[] getLayers()
	{
		return this.layers;
	}
}