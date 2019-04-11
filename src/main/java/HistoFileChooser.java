import java.io.File;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

public class HistoFileChooser extends JFileChooser
{
	private static final long serialVersionUID = 1L;
	private File currentDirectory;


	public HistoFileChooser(final FileSystemView fsv)
	{
		super(fsv);
	}


	@Override
	public boolean accept(final File f)
	{
		return true;
	}


	@Override
	public File getCurrentDirectory()
	{
		return this.currentDirectory;
	}


	@Override
	public Icon getIcon(final File f)
	{
		if (getFileSystemView().isTraversable(f).booleanValue())
		{
			return UIManager.getIcon("FileView.directoryIcon");
		}
		return super.getIcon(f);
	}


	@Override
	public boolean isTraversable(final File f)
	{
		final boolean is = getFileSystemView().isTraversable(f).booleanValue();
		return is;
	}


	@Override
	public void setCurrentDirectory(File dir)
	{
		final File oldValue = getCurrentDirectory();

		if (dir == null)
		{
			dir = getFileSystemView().getDefaultDirectory();
		}
		if (getCurrentDirectory() != null)
		{
			if (getCurrentDirectory().equals(dir))
			{
				return;
			}
		}

		File prev = null;
		if ((dir != null) && (!getFileSystemView().isTraversable(dir).booleanValue()))
		{
			while ((!isTraversable(dir)) && (prev != dir))
			{
				prev = dir;
				dir = getFileSystemView().getParentDirectory(dir);
			}
		}
		this.currentDirectory = dir;

		firePropertyChange("directoryChanged", oldValue, getCurrentDirectory());
	}
}