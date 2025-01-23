package anyPerf;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import anyPerf.utility.FileOperation;

public class Scenario {
	List<Runnable> runnables = new ArrayList<>();
	boolean flag = false;
	ExecutorService executor = Executors.newCachedThreadPool();
	private Integer duration = 0;
	private int totalTransactions = 0;

	public void start() {
		System.out.println("Test starting at :" + new Date());
		try {
			runnables.forEach(r -> {
				executor.submit(r);
			});
		} catch (Exception e) {
			System.out.println("Exception in executor service submits");
			e.printStackTrace();
		} finally {
			executor.shutdown();

			try {
				if (!executor.awaitTermination(duration, TimeUnit.SECONDS)) {
					int lastTransactionCount = 0;
					int count = 0;

					while (totalTransactions > TransactionLog.getTotalTransactions()) {
						System.out.println("Total Expected: " + totalTransactions + "Total Posted: "
								+ TransactionLog.getTotalTransactions());
						if (count > 11)
							break;
						if (lastTransactionCount == 0)
							lastTransactionCount = TransactionLog.getTotalTransactions();
						Thread.sleep(10000);
						if (lastTransactionCount == TransactionLog.getTotalTransactions()) {
							System.out.println(
									"No new transactions posted in past 10 seconds. Test duration has ended but total transactions have not been reached... check count: "
											+ count + " max wait 2 minute.");
							count++;
						} else {
							System.out.println(
									"Test duration has ended but transactions are still posting, TPM may not have been realistic.");
						}

						lastTransactionCount = TransactionLog.getTotalTransactions();
					}

					executor.shutdownNow();
				}
			} catch (InterruptedException ex) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}

		}
		System.out.println("Test ending at :" + new Date());
	}

	public void results() {
		TransactionLog.printResult();
	}

	private Class<? extends AnyTest> getClassFromName(String name) {
		Class<? extends AnyTest> classTemp = null;
		try {
			classTemp = Class.forName(name).asSubclass(AnyTest.class);
		} catch (ClassNotFoundException e) {
			System.out.println(
					"Test class not found in AnyTest jar, looking in the code directory for classes compiled at run time");
			try {
				List<String> codeFiles = FileOperation.getListofFiles("code", false, false);
				if (codeFiles != null && !codeFiles.isEmpty()) {
					String dir = codeFiles.get(0).substring(0, codeFiles.get(0).lastIndexOf(File.separator));
					Path sourcePath = Paths.get(dir, name + ".class");
					URL classUrl = sourcePath.getParent().toFile().toURI().toURL();
					URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { classUrl });
					classTemp = Class.forName(name, true, classLoader).asSubclass(AnyTest.class);
				}
			} catch (Exception ex) {
				System.out.println("Test class couldn't be created. Exception in creating runnable tasks per user");
				ex.printStackTrace();
			}

		}
		return classTemp;
	}

	public void inject(String name, int rampUp, int users, int durationOrIteration, int tpm, String configFileName,
			boolean isDuration_OrIteration) {
		if (isDuration_OrIteration) {
			int ramp = rampUp * 1000;
			int dura = durationOrIteration * 1000;
			duration = Math.round(durationOrIteration * 1.01f);
			Double rate = rampUp * 1000 / (double) users;
			System.out.println("Rate/time per user : + " + rate);
			for (int i = 0; i < users; i++) {
				final int x = i;
				Runnable r = new Thread(() -> {
					Class<? extends AnyTest> classTemp = getClassFromName(name);
					try {
						if (classTemp != null) {
							AnyTest obj = classTemp.getDeclaredConstructor().newInstance();
							obj.setDuration(ramp + dura - Math.round(rate) * x);
							obj.setTpm(Math.round(tpm / users));
							obj.setConfigFile(configFileName);
							Thread.sleep(Math.round(rate) * x);
							obj.runTest();
						}
					} catch (Exception e) {
						System.out.println("Exception in creating runnable tasks per user");
						e.printStackTrace();
					}
				});
				runnables.add(r);
			}
		} else {
			Double rate = rampUp * 1000 / (double) users;
			duration = Math.round(1.01f * durationOrIteration * users / tpm * 60);
			totalTransactions = totalTransactions + durationOrIteration * users;
			System.out.println("Rate/time per user : + " + rate);
			for (int i = 0; i < users; i++) {
				final int x = i;
				Runnable r = new Thread(() -> {
					Class<? extends AnyTest> classTemp = getClassFromName(name);
					try {
						if (classTemp != null) {
							AnyTest obj = classTemp.getDeclaredConstructor().newInstance();
							obj.setIteration(durationOrIteration);
							obj.setTpm(Math.round(tpm / users));
							obj.setConfigFile(configFileName);
							Thread.sleep(Math.round(rate) * x);
							obj.runTest();
						}
					} catch (Exception e) {
						System.out.println("Exception in creating runnable tasks per user");
						e.printStackTrace();
					}
				});
				runnables.add(r);
			}
		}
	}
}
