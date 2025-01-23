package anyPerf.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileDistributor {
	private static FileDistributor fd = new FileDistributor();
	private FileDistributor() {};
	public static FileDistributor getInstance() { return fd;}
	
	private Map<String, List<String>> filemap = new HashMap<>();
	private Map<String, Integer> dirIndex = new HashMap<>();
	
	public String getFileName(String dirname)
	{
		List<String> filenames = filemap.get(dirname);
		int index;
		synchronized(filenames)
		{
			index = dirIndex.get(dirname);
			if (index == filenames.size()) index = 0;
			dirIndex.put(dirname, index+1);
		}
		return filenames.get(index);
	}
	
	public synchronized void register(String dirname)
	{
		if (!filemap.containsKey(dirname))
		{
			List<String> ls = FileOperation.getListofFiles(dirname, false, false);
			filemap.put(dirname, ls);
			dirIndex.put(dirname, 0);
		}
	}

}
