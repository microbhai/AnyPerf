package anyPerf;

import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import anyPerf.utility.FileOperation;
import anyPerf.utility.StringOps;

public class App {
	public static void main(String[] args) {
		List<String> testConfig = FileOperation.getContentAsList("test_config.txt", "utf8");
		List<String> influxConfig = FileOperation.getContentAsList("influxdb_config.txt", "utf8");
		List<String> codeFiles = FileOperation.getListofFiles("code", false, false);

		if (codeFiles != null && !codeFiles.isEmpty()) {
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

			for (String codeFile : codeFiles) {
				if (codeFile.endsWith(".java"))
					compiler.run(null, null, null, codeFile);
			}
		}
		if (influxConfig != null)
			for (String s1 : influxConfig) {
				if (!s1.isEmpty() && !s1.trim().startsWith("#")) {
					if (s1.contains("url"))
						TransactionLog.influxurl = StringOps.fastSplit(s1, "->").get(1).trim();
					if (s1.contains("headers"))
						TransactionLog.influxdbheader = StringOps.getInBetweenFast(StringOps.fastSplit(s1, "->").get(1).trim(),"<header>","</header>", true, true);

				}
			}

		Scenario scn = new Scenario();
		for (String s : testConfig) {
			if (!s.isEmpty() && !s.trim().startsWith("#")) {
				List<String> ls = StringOps.fastSplit(s, ",");
				scn.inject(ls.get(0).trim(), Integer.parseInt(ls.get(1).trim()), Integer.parseInt(ls.get(2).trim()),
						Integer.parseInt(ls.get(3).trim()), Integer.parseInt(ls.get(4).trim()), ls.get(5).trim(),
						(ls.get(6).trim().equals("true")) ? true : false);
			}
		}

		scn.start();
		scn.results();
	}
}
