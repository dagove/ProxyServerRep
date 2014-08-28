package eggum;

import java.io.*; 
import java.net.*; 
import java.nio.charset.Charset;

import java.awt.Toolkit;

import org.xmlpull.v1.XmlPullParserException;

public class UDPServer implements Runnable
{	
	private static int counter = 1;
	private static int errorCounter = 1;
	private String name;
	private String compression = "no";
	private String URL;
	private String SOAP_ACTION;		
	private int port; 
	private ProxyTransport transporter;
	
	public UDPServer(int port, String url, String SOAP_ACTION)
	{
		this.port = port;
		this.URL = url;
		this.SOAP_ACTION = SOAP_ACTION;
	    if( (port == 8083) || (port == 8086) || (port == 8089) ) {compression = "gzip";}
	    if( (port == 8084) || (port == 8087) || (port == 8090) ) {compression = "exi";}
    	this.transporter = new ProxyTransport(URL);
	}
	
	public void run() {
		name = Thread.currentThread().getName();
		System.out.println("UDP server: " + name + " started");
		try{
			DatagramSocket serverSocket = new DatagramSocket(port);;
			byte[] receiveData = new byte[81920];
			byte[] sendData; 
			while(true)                
			{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

				serverSocket.receive(receivePacket);            
	        	System.out.println("\n" + name + ": UDP Request received");		
				InetAddress IPAddress = receivePacket.getAddress();                  
				int port = receivePacket.getPort();				
				
				sendData = sendRequest(receiveData, compression);
				try
				{
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					serverSocket.send(sendPacket);
					System.out.println("PROXY: Request Data Size: " + receivePacket.getLength());
			    	System.out.println("PROXY: Response Data Size: " + sendData.length);
		        	System.out.println("Counter: " + counter);
		        	counter++;
		        	errorCounter = 1;
				}
				catch (Exception e) {}
			}			
		}catch (IOException e) {
			System.out.println("Faulty port " + port);
            e.printStackTrace();
        }
		
		Toolkit.getDefaultToolkit().beep();
		}
		
		byte[] sendRequest(byte[] SOAPrequest, String compression)
	    {			
	    	byte[] b = null;	    	
	    	
	    	String contentType =  "text/xml;charset=utf-8";	    	
	    	
	    	try {
				b = transporter.call(SOAPrequest, compression, SOAP_ACTION, contentType);
			} catch (IOException | XmlPullParserException e1) {
				e1.printStackTrace();
				System.out.println("Error contacting WS server " + errorCounter);
				Toolkit.getDefaultToolkit().beep();
				errorCounter++;
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