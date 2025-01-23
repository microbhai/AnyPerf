package anyPerf.tests;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import anyPerf.AnyTest;
import anyPerf.utility.FileDistributor;
import anyPerf.utility.FileOperation;

public class AwsS3AnyTest extends AnyTest {

	S3Client s3;
	Region region;

	String BUCKET;
	String FILE_DIR;
	String ACCESS_PROFILE;
	String KEY = "";
	String[] TRANSACTION_NAME;

	int counterLimit;
	List<String> config;
	List<String> auth;

	void setConfig() {
		if (config == null)
			config = FileOperation.getContentAsList(super.configFileName, "utf8");

		for (String s : config) {
			if (s.trim().startsWith("BUCKET"))
				BUCKET = s.split("=")[1].trim();
			if (s.trim().startsWith("FILE_DIR"))
				FILE_DIR = s.split("=")[1].trim();
			if (s.trim().startsWith("ACCESS_PROFILE"))
				ACCESS_PROFILE = s.split("=")[1].trim();
			if (s.trim().startsWith("TRANSACTION_NAME"))
				TRANSACTION_NAME = s.split("=")[1].trim().split(",");
			if (s.trim().startsWith("APPLICATION_NAME"))
				APPLICATION = s.split("=")[1].trim();// defined in AnyTest
			
			try {
				if (s.trim().startsWith("KEY"))
					KEY = s.split("=")[1].trim();
			} catch (Exception e) {
			}
		}
	}

	public AwsS3AnyTest() {
	}

	protected void init() {
		setConfig();

		region = Region.US_WEST_2;
		s3 = S3Client.builder().region(region).credentialsProvider(ProfileCredentialsProvider.create(ACCESS_PROFILE))
				.build();

		FileDistributor.getInstance().register(FILE_DIR);

	}

	protected void test() {

		try {
			String trans = "";
			if (TRANSACTION_NAME==null)
				trans = "Test1_Step1";
			else
				trans = TRANSACTION_NAME[0];
			
			startTransaction(trans);
			try {
				step1();
				endTransaction(trans);
			} catch (Exception e) {
				cancelTransaction(trans, e.getMessage());
				throw new RuntimeException(e);
			}

			// thinkTime(100);

		} catch (Exception e) {
			System.out.println("Exception in test 1 execution");
			e.printStackTrace();
		} finally {

		}
	
	}

	protected void cleanup() {
		s3.close();
	}

	public void step2() throws Exception {
		System.out.println("Step executed... by user: " + this.userid);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}

	public void step1() throws Exception {

		String file = FileDistributor.getInstance().getFileName(FILE_DIR);
		// System.out.println(file);
		String[] split = file.split(Pattern.quote(System.getProperty("file.separator")));
		//String result = 
				putS3Object(s3, BUCKET, KEY + split[split.length - 1], getObjectFile(file));
		// System.out.println(result);

	}

	public String putS3Object(S3Client s3, String bucketName, String objectKey, byte[] data) {

		try {

			Map<String, String> metadata = new HashMap<>();
			metadata.put("file-purpose", "perftest");

			PutObjectRequest putOb = PutObjectRequest.builder().bucket(bucketName).key(objectKey).metadata(metadata)
					.build();

			PutObjectResponse response = s3.putObject(putOb, RequestBody.fromBytes(data));

			return response.eTag();

		} catch (S3Exception e) {
			System.out.println(e.getMessage());
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return "";
	}

	private byte[] getObjectFile(String filePath) {

		FileInputStream fileInputStream = null;
		byte[] bytesArray = null;

		try {
			File file = new File(filePath);
			bytesArray = new byte[(int) file.length()];
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bytesArray);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bytesArray;
	}

}
