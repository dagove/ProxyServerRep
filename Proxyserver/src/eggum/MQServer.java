package eggum;

import java.io.*; 
import java.net.*; 
import java.nio.charset.Charset;

import java.awt.Toolkit;

import org.xmlpull.v1.XmlPullParserException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

public class MQServer implements Runnable
{
	private static int counter = 1;
	private static int errorCounter = 1;
	private String name;
	private String compression = "no";
	private String URL; 
	private String SOAP_ACTION;
	private String RPC_QUEUE_NAME;
	private ProxyTransport transporter;
	
	public MQServer(String queueName, String comp, String url, String SOAP_ACTION)
	{
		this.RPC_QUEUE_NAME = queueName;
		this.compression = comp;	//	no, gzip, exi
		this.URL = url;
		this.SOAP_ACTION = SOAP_ACTION;
		this.transporter = new ProxyTransport(URL);
	}
	
	public void run() {
		name = Thread.currentThread().getName();
		System.out.println("MQ server: " + name + " started");
		Connection connection = null;
	    Channel channel = null;

	    try {
	    	
		    ConnectionFactory factory = new ConnectionFactory();
		    factory.setHost("localhost");    
		    byte[] receiveData = new byte[1024];
		    byte[] sendData = null; 	      
		    connection = factory.newConnection();
		    channel = connection.createChannel();	      
		    channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);	  
		    channel.basicQos(1);	  
		    QueueingConsumer consumer = new QueueingConsumer(channel);
		    channel.basicConsume(RPC_QUEUE_NAME, false, consumer);	  
		    System.out.println(" [x] Awaiting RPC requests from Queue: " + RPC_QUEUE_NAME);
	  
		    while (true) 
		    {		    	
		    	QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		    	BasicProperties props = delivery.getProperties();
		    	BasicProperties replyProps = new BasicProperties
	                                         .Builder()
	                                         .correlationId(props.getCorrelationId())
	                                         .build();
		    	try {
		    		receiveData = delivery.getBody();
		    		System.out.println("\n" + RPC_QUEUE_NAME + " MQ Request received");
	        	
		    		sendData = sendRequest(receiveData, compression);	// Kan bli null
		    		int s = sendData.length;
		    		System.out.println("PROXY: Request Data Size: " + receiveData.length);
		    		System.out.println("PROXY: Response Data Size: " + s);
			        System.out.println("Counter: " + counter);
			        counter++;
			      	errorCounter = 1;
		    		}
		    		catch (Exception e){}
	        finally {  
	          channel.basicPublish( "", props.getReplyTo(), replyProps, sendData);
	          System.out.println("published");
	          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);


	        }
	      }
	    }
	    catch  (Exception e) {
	      e.printStackTrace();
	    }
	    finally {
	      if (connection != null) {
	        try {
	          connection.close();
	        }
	        catch (Exception ignore) {}
	      }
	    }      		      
	  }		
	
	byte[] sendRequest(byte[] SOAPrequest, String compression)
    {   	
    	byte[] b = null;	    	
    	
    	String contentType =  "text/xml;charset=utf-8";	    	
    	
    	try {
			b = transporter.call(SOAPrequest, compression, SOAP_ACTION, contentType);
		} catch (IOException | XmlPullParserException e1) {
			//e1.printStackTrace();
			System.out.println("Error contacting WS server " + errorCounter);
			errorCounter++;
			Toolkit.getDefaultToolkit().beep();
		}    
        return b;
    }
	
    public static byte[] convertInputStreamToByteArray(InputStream is) throws IOException
    {
  	  byte[] buff = new byte[8192];
  	  ByteArrayOutputStream out = new ByteArrayOutputStream(8192);	    
  	  int n = 0;
  	  while ((n = is.read(buff)) >= 0) {
  		  out.write(buff, 0, n);
  	  }
  	  is.close();
  	  out.close();
    
  	  return out.toByteArray();
    }
}