package anyPerf;

public class UserID {

	static long userid = 0l;
	public synchronized static long getID()
	{
		return userid++;
	}
}
