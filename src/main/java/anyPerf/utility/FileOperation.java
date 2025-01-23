package anyPerf.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.List;

public class FileOperation {
	private FileOperation() {
	}

	private static final String IOEXCEPTIONMSG = "ERROR: IO Exception.\n";

	public static void deleteFile(String name, String fileExtension) {
		File f = new File(name);
		if (f.isDirectory()) {
			File[] contents;
			if (fileExtension.length()>0) {
				final String ext = fileExtension;
				contents = f.listFiles((dir, filename) -> filename.endsWith(ext));
			}
			else
				contents = f.listFiles();
			for (File f1 : contents) {
				deleteFile(f1.getAbsolutePath(), "");
			}
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				System.out.println("ERROR: IO Exception deleting files.\n" + LogStackTrace.get(e));
			}
		} else
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}

	}

	public static String getFileContentAsString(String filePath) {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(filePath));
			return new String(encoded, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));

			return null;
		}
	}

	public static List<String> getListofFiles(String filePath, boolean printDirName, boolean recursive) {
		List<String> toReturn = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(filePath))) {
			for (Path entry : stream) {
				if (!entry.toFile().isDirectory())
					toReturn.add(entry.toFile().getAbsolutePath());
				else {
					if (printDirName)
						toReturn.add(entry.toFile().getAbsolutePath());

					if (recursive)
						toReturn.addAll(getListofFiles(entry.toFile().getAbsolutePath(), printDirName, true));
				}
			}

		} catch (IOException e) {
			System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
		}
		return toReturn;
	}

	public static File[] getListofFiles(String filePath, String filter) {
		File dir = new File(filePath);
		return dir.listFiles((d, name) -> name.endsWith(filter));

	}

	public static String getContentAsString(String filePath, String encoding) {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(filePath));
			return new String(encoded, encoding);
		} catch (IOException e) {
			System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			return null;
		}
	}

	
	public static List<String> getContentAsList(String filePath, String encoding) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), encoding));) {
			List<String> list = new ArrayList<>();
			String str;
			while ((str = br.readLine()) != null) {
				list.add(str);
			}
			return list;
		} catch (IOException e) {
			System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			return new ArrayList<>();
		}
	}

	public static List<String> getContentAsList(String filePath, String encoding, String lineStart, String lineEnd,
			boolean excludeStartEnd, boolean excludeEmptyLines) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), encoding));) {
			List<String> list = new ArrayList<>();
			String str;
			boolean flag = false;
			while ((str = br.readLine()) != null) {
				if (str.contains(lineEnd)) {
					flag = false;
					if (!excludeStartEnd)
						list.add(str);
				}
				if (str.contains(lineStart)) {
					flag = true;
					if (excludeStartEnd)
						continue;
				}
				if (flag) {
					if (excludeEmptyLines) {
						if (str.length() > 0)
							list.add(str);
					} else
						list.add(str);
				}

			}
			return list;
		} catch (IOException e) {
			System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			return new ArrayList<>();
		}
	}

	public static List<String> getContentAsList(String filePath, String encoding, boolean excludeBlankLines) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), encoding));) {
			List<String> list = new ArrayList<>();
			String str;
			while ((str = br.readLine()) != null) {
				if (excludeBlankLines) {
					if (str.length() > 0)
						list.add(str);
				} else
					list.add(str);
			}
			return list;
		} catch (IOException e) {
			System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			return new ArrayList<>();
		}
	}

	public static void writeFile(String filePath, List<String> toPrint, boolean unixLineSeparator) {
		try (BufferedWriter pw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));) {
			for (String line : toPrint) {
				pw.append(line);
				if (unixLineSeparator)
					pw.append("\n");
				else
					pw.append(System.lineSeparator());
			}
			pw.flush();
		} catch (IOException e) {
			System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
		}
	}

	public static void writeFile(String filePath, String fileName, String toPrint, String encoding) {
		if (filePath.length() > 0) {
			File directory = new File(filePath);
			if (!directory.exists())
				directory.mkdirs();
			try (BufferedWriter pw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(filePath + File.separator + fileName), encoding));) {
				pw.append(toPrint);
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		} else {
			try (BufferedWriter pw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fileName), encoding));) {
				pw.append(toPrint);
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		}

	}

	public static void writeFile(String filePath, String fileName, List<String> toPrint, boolean unixLineSeparator) {
		if (filePath.length() > 0) {
			File directory = new File(filePath);
			if (!directory.exists())
				directory.mkdirs();
			try (BufferedWriter pw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(filePath + File.separator + fileName), StandardCharsets.UTF_8));) {
				for (String line : toPrint) {
					pw.append(line);
					if (unixLineSeparator)
						pw.append("\n");
					else
						pw.append(System.lineSeparator());
				}
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		} else {
			try (BufferedWriter pw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8));) {
				for (String line : toPrint) {
					pw.append(line);
					if (unixLineSeparator)
						pw.append("\n");
					else
						pw.append(System.lineSeparator());
				}
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		}
	}

	public static void writeFile(String filePath, String fileName, List<String> toPrint, boolean unixLineSeparator,
			boolean append) {
		if (filePath.length() > 0) {
			File directory = new File(filePath);
			if (!directory.exists())
				directory.mkdirs();
			try (BufferedWriter pw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filePath + File.separator + fileName, append), StandardCharsets.UTF_8));) {
				for (String line : toPrint) {
					pw.append(line);
					if (unixLineSeparator)
						pw.append("\n");
					else
						pw.append(System.lineSeparator());
				}
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		} else {
			try (BufferedWriter pw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fileName, append), StandardCharsets.UTF_8));) {
				for (String line : toPrint) {
					pw.append(line);
					if (unixLineSeparator)
						pw.append("\n");
					else
						pw.append(System.lineSeparator());
				}
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		}
	}

	public static void writeFile(String filePath, String fileName, String toPrint) {

		if (filePath.length() > 0) {
			File directory = new File(filePath);
			if (!directory.exists())
				directory.mkdirs();

			try (BufferedWriter pw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filePath + File.separator + fileName), StandardCharsets.UTF_8));) {
				pw.append(toPrint);
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		} else {
			try (BufferedWriter pw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8));) {
				pw.append(toPrint);
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		}
	}

	public static void writeFile(String filePath, String fileName, String toPrint, boolean append) {

		if (filePath.length() > 0) {
			File directory = new File(filePath);
			if (!directory.exists())
				directory.mkdirs();

			try (BufferedWriter pw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filePath + File.separator + fileName, append), StandardCharsets.UTF_8));) {
				pw.append(toPrint);
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		} else {
			try (BufferedWriter pw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fileName, true), StandardCharsets.UTF_8));) {
				pw.append(toPrint);
				pw.flush();
			} catch (IOException e) {
				System.out.println(IOEXCEPTIONMSG + LogStackTrace.get(e));
			}
		}
	}
}
