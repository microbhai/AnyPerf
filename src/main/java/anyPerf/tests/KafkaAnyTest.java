package anyPerf.tests;

import org.apache.kafka.clients.CommonClientConfigs;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SaslConfigs;

import anyPerf.AnyTest;
import anyPerf.utility.FileDistributor;
import anyPerf.utility.FileOperation;
import anyPerf.utility.StringOps;

import java.util.Properties;
import java.io.File;
import java.util.List;

public class KafkaAnyTest extends AnyTest {

	String authfilename = "auth.txt";
	String BROKER_URL;
	String TOPIC;
	String OAUTH_TOKEN_URL;
	String OAUTH_CLIENT_ID;
	String CLIENT_ID;
	String OAUTH_CLIENT_SECRET;
	String FILE_DIR;
	String COMPRESSION;
	String ARCHIVE_DIR;
	String[] TRANSACTION_NAME;

	int counterLimit;
	List<String> config;
	List<String> auth;

	Producer<String, String> producer;

	static int suffix = 0;

	private static synchronized int getSuffix() {
		return suffix++;
	}

	void setConfig() {
		if (config == null)
			config = FileOperation.getContentAsList(super.configFileName, "utf8");
		if (auth == null)
			auth = FileOperation.getContentAsList(authfilename, "utf8");
		
		for (String s : auth)
		{
			if (s.trim().startsWith("OAUTH_TOKEN_URL")) OAUTH_TOKEN_URL = s.split("=")[1].trim();
			if (s.trim().startsWith("OAUTH_CLIENT_ID")) OAUTH_CLIENT_ID = s.split("=")[1].trim();
			if (s.trim().startsWith("OAUTH_CLIENT_SECRET")) OAUTH_CLIENT_SECRET = s.split("=")[1].trim();
			CLIENT_ID = OAUTH_CLIENT_ID + "_" + getSuffix();
		}
		
		for (String s : config)
		{
			if (s.trim().startsWith("BROKER_URL")) BROKER_URL = s.split("=")[1].trim();
			if (s.trim().startsWith("TOPIC")) TOPIC = s.split("=")[1].trim();
			if (s.trim().startsWith("COMPRESSION")) COMPRESSION = s.split("=")[1].trim();
			if (s.trim().startsWith("FILE_DIR")) FILE_DIR = s.split("=")[1].trim();
			if (s.trim().startsWith("ARCHIVE_DIR")) ARCHIVE_DIR = s.split("=")[1].trim();
			if (s.trim().startsWith("TRANSACTION_NAME")) TRANSACTION_NAME = s.split("=")[1].trim().split(",");
			if (s.trim().startsWith("APPLICATION_NAME")) APPLICATION = s.split("=")[1].trim(); // defined in AnyTest
		}
	}

	public KafkaAnyTest() {
	}

	protected void init() {
		setConfig();
		
		/*OAUTH_TOKEN_URL = auth.get(0).split("=")[1].trim();
		OAUTH_CLIENT_ID = auth.get(1).split("=")[1].trim();
		OAUTH_CLIENT_SECRET = auth.get(2).split("=")[1].trim();

		System.setProperty("oauth.token.endpoint.uri", OAUTH_TOKEN_URL);
		System.setProperty("oauth.client.id", OAUTH_CLIENT_ID);
		System.setProperty("oauth.client.secret", OAUTH_CLIENT_SECRET);*/

		FileDistributor.getInstance().register(FILE_DIR);
		
		producer = getProducer();
	}

	protected void test() {
		
			String trans = "";
			if (TRANSACTION_NAME==null)
				trans = "Test1_Step1";
			else
				trans = TRANSACTION_NAME[0];
			
			String file = FileDistributor.getInstance().getFileName(FILE_DIR);
			String toPost = FileOperation.getFileContentAsString(file);
			startTransaction(trans);
			try {
				step1(toPost, file);
				endTransaction(trans);
				moveFile(file);
			} catch (Exception e) {
				cancelTransaction(trans, e.getMessage());
				//throw new RuntimeException(e);
			}

	}

	protected void cleanup() {
		producer.close();
	}

	public void step2() throws Exception {
		System.out.println("Step executed... by user: " + this.userid);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}

	public void step1(String toPost, String file) throws Exception {
		
		// System.out.println(file);
		try {
			
			ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, toPost);
			//RecordMetadata res = 
					producer.send(record).get();
			
			// System.out.println("partition,offset = " + res.partition() + "," +
			// res.offset());
		} catch (Exception e) {
			System.out.println("No injection for file :" + file);
			System.out.println("Exception message:" + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	/*
	 * private String getFileData() { String file =
	 * FileDistributor.getInstance().getFileName(FILE_DIR); String toReturn =
	 * FileOperation.getFileContentAsString(file);
	 * //System.out.println(Thread.currentThread().getId() + ":" + file);
	 * moveFile(file); return toReturn; }
	 */

	private void moveFile(String file) {
		File orig_file = new File(file);
		String newpath = file.replace(FILE_DIR, ARCHIVE_DIR);
		File target_file = new File(newpath);
		orig_file.renameTo(target_file);
		orig_file.delete();
		// System.out.println(Thread.currentThread().getId() + "Moved to Archive : " +
		// newpath);
	}

	private Producer<String, String> getProducer() {

		String sasl_jaas_config = StringOps.append("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required",
				" oauth.token.endpoint.uri=\"", OAUTH_TOKEN_URL,
				"\" oauth.client.id=\"", OAUTH_CLIENT_ID,
				"\" oauth.client.secret=\"", OAUTH_CLIENT_SECRET,"\";");
		
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER_URL);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, CLIENT_ID);
		props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "" + 1024 * 1024 * 10);

		props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
		props.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
		//props.put(SaslConfigs.SASL_JAAS_CONFIG,
		//		"org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");
		props.put(SaslConfigs.SASL_JAAS_CONFIG, sasl_jaas_config);
		props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS,
				"io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");

		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer");

		if (!COMPRESSION.equals("none"))
			props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION);

		return new KafkaProducer<>(props);
	}

}
