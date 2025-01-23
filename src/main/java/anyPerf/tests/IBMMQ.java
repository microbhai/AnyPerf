package anyPerf.tests;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
//import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
//import javax.jms.TextMessage;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import anyPerf.AnyTest;
import anyPerf.utility.FileDistributor;
import anyPerf.utility.FileOperation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class IBMMQ extends AnyTest {

	String HOST_NAME;
	String FILE_DIR;
	String CHANNEL;
	Integer PORT;
	String QUEUE_MANAGER;
	String DESTINATION;
	String ARCHIVE_DIR;
	String[] TRANSACTION_NAME;

	Connection connection = null;
	Session Qsession = null;
	MessageProducer producer = null;

	int counterLimit;
	List<String> config;
	List<String> auth;

	void setConfig() {
		if (config == null)
			config = FileOperation.getContentAsList(super.configFileName, "utf8");
	}

	public IBMMQ() {
	}

	protected void init() {
		setConfig();

		
		for (String s : config)
		{
			if (s.trim().startsWith("HOST_NAME")) HOST_NAME = s.split("=")[1].trim();
			if (s.trim().startsWith("CHANNEL")) CHANNEL = s.split("=")[1].trim();
			if (s.trim().startsWith("PORT")) PORT = Integer.parseInt(s.split("=")[1].trim());
			if (s.trim().startsWith("QUEUE_MANAGER")) QUEUE_MANAGER = s.split("=")[1].trim();
			if (s.trim().startsWith("DESTINATION")) DESTINATION = s.split("=")[1].trim();
			if (s.trim().startsWith("FILE_DIR")) FILE_DIR = s.split("=")[1].trim();
			if (s.trim().startsWith("ARCHIVE_DIR")) ARCHIVE_DIR = s.split("=")[1].trim();
			if (s.trim().startsWith("TRANSACTION_NAME")) TRANSACTION_NAME = s.split("=")[1].trim().split(",");
			if (s.trim().startsWith("APPLICATION_NAME")) APPLICATION = s.split("=")[1].trim();// defined in AnyTest
		}

		FileDistributor.getInstance().register(FILE_DIR);

		try {

			JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);

			JmsConnectionFactory cf = ff.createConnectionFactory();
			cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST_NAME);
			cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
			cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
			cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QUEUE_MANAGER);
			connection = cf.createConnection();
			Qsession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = Qsession.createQueue(DESTINATION);
			producer = Qsession.createProducer(destination);
			

			connection.start();
		} catch (Exception e) {
			System.out.println("Exception in init..." + e.getMessage());
			e.printStackTrace();
		}

	}

	private void moveFile(String file) {
		File orig_file= new File(file);
		String newpath = file.replace(FILE_DIR,ARCHIVE_DIR);
		File target_file= new File(newpath);
		orig_file.renameTo(target_file);
		orig_file.delete();	
		//System.out.println(Thread.currentThread().getId() + "Moved to Archive : " + newpath);
	}
	
	protected void test() {
		try {
			String trans = "";
			if (TRANSACTION_NAME==null)
				trans = "Test1_Step1";
			else
				trans = TRANSACTION_NAME[0];
			startTransaction(trans);
			step1();
			endTransaction(trans);
			// thinkTime(100);

		} catch (Exception e) {
			System.out.println("Exception in test 1 execution" + e.getMessage());
		} finally {

		}
	}

	protected void cleanup() {
		try {
		Qsession.close();
		connection.close();
		}
		catch (Exception e)
		{
			System.out.println("Exception in test 1 execution" + e.getMessage());
			e.printStackTrace();
		}
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
		try {
		byte[] byteBuffer = getObjectFile(file);
		BytesMessage textMessage = Qsession.createBytesMessage();
		textMessage.writeBytes(byteBuffer);
		producer.send(textMessage);
		moveFile(file);
		}
		catch (Exception e)
		{
			System.out.println("No injection for file :" + file);
			System.out.println("Exception message:" +e.getMessage());
			throw new RuntimeException(e);
		}

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
