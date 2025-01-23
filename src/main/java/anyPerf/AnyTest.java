package anyPerf;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AnyTest {

	protected long userid;
	private long duration = 0;
	private double tpm;
	private long wait = 0;
	private int iteration = 0;
	protected String configFileName;
	private int currentIteration = 0;
	protected String APPLICATION = "DEFAULT_APPLICATION_NAME";

	Map<String, List<Long>> responseTimes = new HashMap<>();
	Map<String, List<String>> errMsgMap = new HashMap<>();
	Map<String, Integer> responseTimesOffset = new HashMap<>();
	Map<String, Integer> errMsgOffset = new HashMap<>();
	Map<String, Long> startTimes = new HashMap<>();
	ArrayList<Long> testTimetaken = new ArrayList<>();
	
	public Map<String, List<Long>> getResponseTimes()
	{
		return responseTimes;
	}

	public AnyTest() {
		userid = UserID.getID();
	}

	
	public void cancelTransaction(String name, String errMsg) {
		name = name +"<@>"+APPLICATION;
		startTimes.remove(name);
		errMsgMap.putIfAbsent(name, new ArrayList<String>());
		errMsgMap.get(name).add(errMsg);
		System.out.println("Transaction cancelled : " + name);
	}

	public long randomBetween(long min, long max) {
		return (min + (long) (Math.random() * (max - min)));
	}
	
	public void startTransaction(String name) {
		name = name +"<@>"+APPLICATION;
		startTimes.put(name, new Date().getTime());
	}
	
	public void endTransaction(String name) {
		name = name +"<@>"+APPLICATION;
		if (!responseTimes.containsKey(name))
			responseTimes.put(name, new ArrayList<>());

		responseTimes.get(name).add(new Date().getTime() - startTimes.get(name));
		startTimes.remove(name);
	}

	public void thinkTime(long wait) {
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			System.out.println("Think time interrupted.");
		}
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public void setTpm(double tpm) {
		this.tpm = tpm;
	}

	boolean interrupted = false;

	abstract protected void init();

	abstract protected void test();

	abstract protected void cleanup();

	protected void setConfigFile(String s) {
		configFileName = s;
	}

	private void stop() {
		rtf();
		System.out.println("Stopping user : " + userid + " @ : " + new Date());
		interrupted = true;
		cleanup();
	}

	private synchronized void rtf() {
		Map<String, List<Long>> rtf = new HashMap<>(responseTimes);

		rtf.forEach((k, v) -> {
			int offset;
			int newoffset;
			if (responseTimesOffset.containsKey(k))
				offset = responseTimesOffset.get(k);
			else
				offset = 0;

			newoffset = v.size();
			if (newoffset > offset) {
					responseTimesOffset.put(k, newoffset);
				List<Long> vsub = new ArrayList<>();
				for (int i = offset; i < newoffset; i++)
					vsub.add(v.get(i));
				TransactionLog.responseTimesToFile(userid, k, vsub,
						"ResponseTimeLog_" + TransactionLog.startTime + "_" + userid + ".txt");
				TransactionLog.updateInterimResults(k,vsub);
			}
		});
		
		Map<String, List<String>> err = new HashMap<>(errMsgMap);
		err.forEach((k, v) -> {
			int offset;
			int newoffset;
			if (errMsgOffset.containsKey(k))
				offset = errMsgOffset.get(k);
			else
				offset = 0;

			newoffset = v.size();
			if (newoffset > offset) {
				errMsgOffset.put(k, newoffset);
				List<String> vsub = new ArrayList<>();
				for (int i = offset; i < newoffset; i++)
					vsub.add(v.get(i));
				TransactionLog.updateErrResults(k,vsub);
			}
		});
		
	}

	private void pace() {
		double timeAvg = testTimetaken.stream().mapToDouble(d -> d).average().getAsDouble();
		wait = Math.round((60 / (tpm * 1.03) - timeAvg / 1000) * 1000);
		if (wait < 0) {
			wait = 0;
			System.out.println(
					"WARNING: Test may not be able to achieve required transaction per minute. Increase number of users or test transactions should response faster. Average time to run test steps is (ms): "
							+ timeAvg);
		}
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
		}
	}

	public void runTest() {
		TransactionLog.addUser(userid);
		init();
		ScheduledExecutorService sesx = Executors.newScheduledThreadPool(1);
		Runnable rtf = this::rtf;
		sesx.scheduleAtFixedRate(rtf, randomBetween(5000, 15000), 5000, TimeUnit.MILLISECONDS);
		Date starttime = new Date();
		System.out.println("Starting user : " + userid + " @ : " + starttime);

		if (duration > 0) {
			ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
			Runnable interrupt = this::stop;
			ses.schedule(interrupt, this.duration, TimeUnit.MILLISECONDS);
			while (!interrupted) {
				long st = new Date().getTime();
				test();
				long ed = new Date().getTime();
				testTimetaken.add(ed - st);
				pace();
			}
			sesShutdown(ses);
		} else {
			while (!interrupted) {
				if (currentIteration<iteration)
				{
					long st = new Date().getTime();
					test();
					long ed = new Date().getTime();
					testTimetaken.add(ed - st);
					pace();
					currentIteration++;
				}
				else
				{
					stop();
				}
			}

		}
		sesShutdown(sesx);
		TransactionLog.submitResults(responseTimes, userid);
		Date endtime = new Date();
		System.out.println("Stopped user : " + userid + " @ : " + endtime);
		System.out.println("Run duration for user : " + userid + " @ : " + (endtime.getTime() - starttime.getTime()));
		
		TransactionLog.removeUser(userid);
	}

	public void sesShutdown(ScheduledExecutorService ses) {
		ses.shutdown();
		try {
			if (!ses.awaitTermination(300, TimeUnit.SECONDS)) {
				ses.shutdownNow();
			}
		} catch (InterruptedException ex) {
			ses.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

}
