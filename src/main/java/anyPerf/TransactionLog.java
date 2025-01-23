package anyPerf;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import anyPerf.utility.FileOperation;
import anyPerf.utility.StringOps;

public class TransactionLog {

	static Map<String, List<Long>> responseTimes = new TreeMap<>();
	static TreeMap<String, List<Long>> responseTimesInterim = new TreeMap<>();
	static TreeMap<String, List<String>> errorMap = new TreeMap<>();
	static Map<String, Integer> errorOffset = new HashMap<>();
	static Map<String, Integer> responseTimesOffset = new HashMap<>();
	public static Long startTime = null;
	static ScheduledExecutorService sesx;
	public static String influxurl = null; // http://loadgenw10p4v16.ad.XYZ.com:8086/api/v2/write?org=XYZ&bucket=testcustom&precision=ns
	public static List<String> influxdbheader = null;
	static ConnectionPool cp = new ConnectionPool();
	static AtomicBoolean shouldPrint = new AtomicBoolean(false);
	static AtomicBoolean shouldErrorPrint = new AtomicBoolean(false);

	public static List<Long> users = new ArrayList<>();

	public static int getTotalTransactions() {
		int toReturn = 0;
		for (Map.Entry<String, List<Long>> s : responseTimes.entrySet()) {
			toReturn = toReturn + s.getValue().size();
		}
		return toReturn;
	}

	public static synchronized void addUser(Long id) {
		if (startTime == null) {
			startTime = new Date().getTime();
			sesx = Executors.newScheduledThreadPool(1);
			sesx.scheduleAtFixedRate(new Runnable() {
				public void run() {
					try {
						if (shouldPrint.get()) {

							synchronized (responseTimesInterim) {
								printInterimResults();
								postInterimResults();
								shouldPrint.set(false);
							}
							if (shouldErrorPrint.get()) {
								synchronized (errorMap) {
									postInterimErrors();
									shouldErrorPrint.set(true);
								}
							}

						} else
							System.out.println("No new result update... checking again in 10 seconds...");

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 10000, 10000, TimeUnit.MILLISECONDS);
		}
		users.add(id);
	}

	public static synchronized void responseTimesToFile(long userid, String transaction, List<Long> respTime,
			String filename) {

		for (long rt : respTime) {
			FileOperation.writeFile("", filename, userid + "," + transaction + "," + rt + "\n", true);
		}

	}

	public static synchronized void removeUser(Long id) {
		users.remove(id);
	}

	public static void updateInterimResults(String transaction, List<Long> rt) {

		responseTimesInterim.putIfAbsent(transaction, new ArrayList<Long>());
		synchronized (responseTimesInterim) {
			rt.forEach((v) -> responseTimesInterim.get(transaction).add(v));
			shouldPrint.set(true);
		}

	}

	public static void updateErrResults(String transaction, List<String> rt) {

		errorMap.putIfAbsent(transaction, new ArrayList<String>());
		synchronized (errorMap) {
			rt.forEach((v) -> errorMap.get(transaction).add(v));
			shouldErrorPrint.set(true);
		}
	}

	public static synchronized void submitResults(Map<String, List<Long>> rt, long userid) {
		System.out.println("Submitting results for user id : " + userid + " @ : " + new Date());
		rt.forEach((k, v) -> {
			if (!responseTimes.containsKey(k))
				responseTimes.put(k, new ArrayList<>());

			v.forEach(x -> responseTimes.get(k).add(x));

			responseTimesToFile(userid, k, v, "ResponseTimeLog_" + startTime + ".txt");
		});
	}

	static void postToInfluxDB(String toPost) {

		Request.Builder requestB = new Request.Builder().url(influxurl);

		if (!influxdbheader.isEmpty()) {
			for (String header : influxdbheader) {
				String name = StringOps.getInBetweenFast(header, "<name>", "</name>", true, false).get(0);
				String value = StringOps.getInBetweenFast(header, "<value>", "</value>", true, false).get(0);
				requestB.addHeader(name, value);
			}
		}
		Request request = requestB.post(RequestBody.create(toPost, MediaType.parse("text/plain; charset=utf-8")))
				.build();

		OkHttpClient client = new OkHttpClient.Builder().readTimeout(120, TimeUnit.SECONDS).connectionPool(cp).build();

		Call call = client.newCall(request);

		try {
			Response response = call.execute();
			if (response.code() >= 400 && response.code() <= 499)
				System.out.println("Influx DB Post: Request has errors:" + response.code());
			if (response.code() >= 500 && response.code() <= 599)
				System.out.println("Influx DB Post: System error:" + response.code());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void postInterimErrors() {

		errorMap.forEach((k, v) -> {

			int offset;
			int newoffset;

			if (errorOffset.containsKey(k))
				offset = errorOffset.get(k);
			else
				offset = 0;

			newoffset = v.size();

			if (newoffset > offset) {
				errorOffset.put(k, newoffset);
				List<String> vsub = new ArrayList<>();
				for (int i = offset; i < newoffset; i++)
					vsub.add(v.get(i));
				Map<String, Integer> errCount = new HashMap<>();
				for (String s : vsub) {

					errCount.putIfAbsent(s, 0);
					if (errCount.containsKey(s))
						errCount.put(s, errCount.get(s) + 1);
				}

				for (Map.Entry<String, Integer> s : errCount.entrySet()) {
					StringBuilder sb = new StringBuilder();
					List<String> ls = StringOps.fastSplit(k, "<@>");
					sb.append("selperf,transaction=").append(ls.get(0));
					sb.append(",application=").append(ls.get(1));
					sb.append(" errormsg=\"").append(s.getKey().replace(" ", "_")).append("\"");
					sb.append(",errorcount=").append(s.getValue());
					System.out.println("ERROR MESSAGE:" + sb.toString());
					postToInfluxDB(sb.toString());
				}
			}

		});

	}

	static void postInterimResults() {

		try {
			if (influxurl != null) {
				responseTimesInterim.forEach((k, v) -> {

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

						Collections.sort(vsub);
						StringBuilder sb = new StringBuilder();
						List<String> ls = StringOps.fastSplit(k, "<@>");
						sb.append("selperf,transaction=").append(ls.get(0));
						sb.append(",application=").append(ls.get(1));
						sb.append(" iterations=").append(vsub.size());
						sb.append(",percentile90=").append(vsub.get(vsub.size() * 9 / 10));
						sb.append(",percentile95=").append(vsub.get(vsub.size() * 95 / 100));
						sb.append(",percentile99=").append(vsub.get(vsub.size() * 99 / 100));
						sb.append(",avg=").append(
								String.format("%.2f", vsub.stream().mapToLong(val -> val).average().orElse(0.0)));
						sb.append(",min=").append(vsub.get(0));
						sb.append(",max=").append(vsub.get(vsub.size() - 1));
						sb.append(",throughput=").append(String.format("%.2f",
								(double) vsub.size() / ((new Date().getTime() - startTime) / 1000)));

						postToInfluxDB(sb.toString());
					}

				});

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void printInterimResults() {

		try {
			System.out.println(
					"Transaction\tIterations\t90%%tile\t95%%tile\t99%%tile\tAverage\tMin\tMax\tThroughput(/sec)");

			responseTimesInterim.forEach((k, v) -> {

				Collections.sort(v);
				System.out.printf("%s\t%d\t%d\t%d\t%d\t%.2f\t%d\t%d\t%.2f %n", k, v.size(), v.get(v.size() * 9 / 10),
						v.get(v.size() * 95 / 100), v.get(v.size() * 99 / 100),
						v.stream().mapToLong(val -> val).average().orElse(0.0), v.get(0), v.get(v.size() - 1),
						(double) v.size() / ((new Date().getTime() - startTime) / 1000));

			});

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void printResult() {
		printInterimResults();
		postInterimResults();
		System.out.println("shutting thread down");
		sesx.shutdown();
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
		}
		int count = 0;
		while (!users.isEmpty() && count < 12) {
			count++;
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
			}
			System.out.println("Waiting for users to finish tasks... max wait 2 minutes");
		}

		System.out
				.println("Transaction\tIterations\t90%%tile\t95%%tile\t99%%tile\tAverage\tMin\tMax\tThroughput(/sec)");
		for (Map.Entry<String, List<Long>> s : responseTimes.entrySet()) {
			if (s != null) {
				Collections.sort(s.getValue());
				System.out.printf("%s\t%d\t%d\t%d\t%d\t%.2f\t%d\t%d\t%.2f %n", s.getKey(), s.getValue().size(),
						s.getValue().get(s.getValue().size() * 9 / 10),
						s.getValue().get(s.getValue().size() * 95 / 100),
						s.getValue().get(s.getValue().size() * 99 / 100),
						s.getValue().stream().mapToLong(val -> val).average().orElse(0.0), s.getValue().get(0),
						s.getValue().get(s.getValue().size() - 1),
						(double) s.getValue().size() / ((new Date().getTime() - startTime) / 1000));
			}
		}
	}
}
