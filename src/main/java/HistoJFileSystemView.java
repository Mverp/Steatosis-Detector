import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

public class HistoJFileSystemView extends FileSystemView
{
	private final PmaCoreClient client;
	private String[] m_roots = null;
	private final HashMap<String, File> fileMap = new HashMap<>();


	public HistoJFileSystemView(final PmaCoreClient aClient) throws RemoteException
	{
		this.client = aClient;
		this.m_roots = aClient.getDirectories("");
	}


	@Override
	public File createNewFolder(final File containingDir) throws IOException
	{
		return null;
	}


	@Override
	public File getDefaultDirectory()
	{
		return getHomeDirectory();
	}


	@Override
	public File[] getFiles(final File dir, final boolean useFileHiding)
	{
		String[] files = null;
		String x = dir.getPath().replace("\\", "/");
		int i = x.indexOf("/");
		if (i != -1)
		{
			x = x.substring(i + 1);
		}

		files = this.client.getFiles(x);

		final List<File> arrFiles = new ArrayList<>();
		String[] arrayOfString1;
		int j = (arrayOfString1 = files).length;
		for (int k = 0; k < j; k++)
		{
			final String s = arrayOfString1[k];
			arrFiles.add(new File(dir, s.substring(s.lastIndexOf('/') + 1)));
		}

		String[] dirs = null;
		x = dir.getPath().replace("\\", "/");
		i = x.indexOf("/");
		if (i != -1)
		{
			x = x.substring(i + 1);
		}

		dirs = this.client.getDirectories(x);
		String[] arrayOfString2;
		final int k = (arrayOfString2 = dirs).length;
		for (j = 0; j < k; j++)
		{
			final String s = arrayOfString2[j];
			final File t = new File(dir, s.substring(s.lastIndexOf('/') + 1));
			this.fileMap.put(s.substring(s.lastIndexOf('/') + 1), t);
			arrFiles.add(t);
		}

		return arrFiles.toArray(new File[0]);
	}


	@Override
	public File getHomeDirectory()
	{
		File f = this.fileMap.get(this.m_roots[0]);
		if (f == null)
		{
			f = new File("/", this.m_roots[0]);
			this.fileMap.put(this.m_roots[0], f);
		}

		return f;
	}


	@Override
	public File getParentDirectory(final File dir)
	{
		if (isRoot(dir))
		{
			return dir;
		}
		final File parent = super.getParentDirectory(dir);

		return parent;
	}


	@Override
	public File[] getRoots()
	{
		final File[] arrFiles = new File[this.m_roots.length];
		for (int i = 0; i < this.m_roots.length; i++)
		{
			arrFiles[i] = new File("/", this.m_roots[i]);
			this.fileMap.put(this.m_roots[i], arrFiles[i]);
		}
		return arrFiles;
	}


	@Override
	public boolean isComputerNode(final File dir)
	{
		return super.isComputerNode(dir);
	}


	@Override
	public boolean isDrive(final File dir)
	{
		return false;
	}


	@Override
	public boolean isFileSystem(final File f)
	{
		return false;
	}


	@Override
	public boolean isFileSystemRoot(final File f)
	{
		String[] arrayOfString;
		final int j = (arrayOfString = this.m_roots).length;
		for (int i = 0; i < j; i++)
		{
			final String root = arrayOfString[i];
			if ((root.equals(f.getName())) || (("/" + root).equals(f.getName())))
				return true;
		}
		return false;
	}


	@Override
	public boolean isHiddenFile(final File f)
	{
		return false;
	}


	@Override
	public boolean isRoot(final File f)
	{
		String[] arrayOfString;
		final int j = (arrayOfString = this.m_roots).length;
		for (int i = 0; i < j; i++)
		{
			final String root = arrayOfString[i];
			if ((root.equals(f.getName())) || (("/" + root).equals(f.getName())))
				return true;
		}
		return false;
	}


	@Override
	public Boolean isTraversable(final File aFile)
	{
		if ((aFile == null) || (aFile.getName() == null))
		{
			return Boolean.valueOf(false);
		}
		String[] arrayOfString;
		final int j = (arrayOfString = this.m_roots).length;
		for (int i = 0; i < j; i++)
		{
			final String root = arrayOfString[i];
			if (root.equals(aFile.getName()))
				return Boolean.valueOf(true);
		}
		return Boolean.valueOf(this.fileMap.containsKey(aFile.getName()));
	}
}