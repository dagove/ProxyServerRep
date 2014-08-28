package eggum;

import java.awt.Toolkit;
import java.io.*;
import java.net.InetSocketAddress;
import org.xmlpull.v1.XmlPullParserException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HTTPServer {
	
	private static int counter = 1;
	private static int errorCounter = 0;
	
	static ProxyTransport helloTransporter;
	static ProxyTransport uploadTransporter;
	static ProxyTransport rawTransporter;
	
	private static String HELLO_URL = "http://localhost:8080//HelloService/HelloName?WSDL";
	private static String HELLO_SOAP_ACTION = "http://eggum/hello";  
	
	private static String UPLOAD_URL = "http://localhost:8080/PictureService/PictureService?WSDL";
	private static String UPLOAD_SOAP_ACTION = "http://service.picture.org/exchangePicture";
	
	private static String RAW_URL = "http://localhost:8080/NFFIService/NFFIService?WSDL";
	private static String RAW_SOAP_ACTION = "http://service.nffi.org/operation";
	
	private static Headers requestHeaders;

    public static void main(String[] args) throws Exception {
    	
    	Thread rawUdp = new Thread(new UDPServer(8082, RAW_URL, RAW_SOAP_ACTION));
    	Thread rawUdpGzip = new Thread(new UDPServer(8083, RAW_URL, RAW_SOAP_ACTION));
    	Thread rawUdpExi = new Thread(new UDPServer(8084, RAW_URL, RAW_SOAP_ACTION));
    	Thread helloUdp = new Thread(new UDPServer(8085, HELLO_URL, HELLO_SOAP_ACTION));
    	Thread helloUdpGzip = new Thread(new UDPServer(8086, HELLO_URL, HELLO_SOAP_ACTION));
    	Thread helloUdpExi = new Thread(new UDPServer(8087, HELLO_URL, HELLO_SOAP_ACTION));
    	Thread uploadUdp = new Thread(new UDPServer(8088, UPLOAD_URL, UPLOAD_SOAP_ACTION));
    	Thread uploadUdpGzip = new Thread(new UDPServer(8089, UPLOAD_URL, UPLOAD_SOAP_ACTION));
    	Thread uploadUdpExi = new Thread(new UDPServer(8090, UPLOAD_URL, UPLOAD_SOAP_ACTION));

    	Thread rawMq = new Thread(new MQServer("rawNoComp", "null", RAW_URL, RAW_SOAP_ACTION)); // noComp null 
    	Thread rawMqGzip = new Thread(new MQServer("rawGzipComp", "gzip", RAW_URL, RAW_SOAP_ACTION)); // gzipComp gzip
    	Thread rawMqEXi = new Thread(new MQServer("rawExiComp", "exi", RAW_URL, RAW_SOAP_ACTION)); // exiComp exi
    	Thread helloMq = new Thread(new MQServer("helloNoComp", "null", HELLO_URL, HELLO_SOAP_ACTION)); // noComp null 
    	Thread helloMqGzip = new Thread(new MQServer("helloGzipComp", "gzip", HELLO_URL, HELLO_SOAP_ACTION)); // gzipComp gzip
    	Thread helloMqEXi = new Thread(new MQServer("helloExiComp", "exi", HELLO_URL, HELLO_SOAP_ACTION)); // exiComp exi
    	Thread uploadMq = new Thread(new MQServer("uploadNoComp", "null", UPLOAD_URL, UPLOAD_SOAP_ACTION)); // noComp null 
    	Thread uploadMqGzip = new Thread(new MQServer("uploadGzipComp", "gzip", UPLOAD_URL, UPLOAD_SOAP_ACTION)); // gzipComp gzip
    	Thread uploadMqEXi = new Thread(new MQServer("uploadExiComp", "exi", UPLOAD_URL, UPLOAD_SOAP_ACTION)); // exiComp exi
    	
    	rawUdp.setName("rawNoCompression");
    	rawUdpExi.setName("rawExi");
    	rawUdpGzip.setName("rawGzip");
    	helloUdp.setName("helloNoCompression");
    	helloUdpExi.setName("helloExi");
    	helloUdpGzip.setName("helloGzip");
    	uploadUdp.setName("uploadNoCompression");
    	uploadUdpExi.setName("uploadExi");
    	uploadUdpGzip.setName("uploadGzip");
    	
      	rawMq.setName("Raw-MQ-server");
      	rawMqGzip.setName("Raw-MQ-server Gzip");
      	rawMqEXi.setName("Raw-MQ-server Exi");
    	helloMq.setName("Hello-MQ-server");
    	helloMqGzip.setName("Hello-MQ-server Gzip");
    	helloMqEXi.setName("Hello-MQ-server Exi");
    	uploadMq.setName("Upload-MQ-server");
    	uploadMqGzip.setName("Upload-MQ-server Gzip");
    	uploadMqEXi.setName("Upload-MQ-server Exi");

    	rawUdp.start();
    	rawUdpExi.start();
    	rawUdpGzip.start();
    	helloUdp.start();
    	helloUdpExi.start();
    	helloUdpGzip.start();
    	uploadUdp.start();
    	uploadUdpExi.start();
    	uploadUdpGzip.start();
    	
    	rawMq.start();
    	rawMqGzip.start();
    	rawMqEXi.start();
    	helloMq.start();
    	helloMqGzip.start();
    	helloMqEXi.start();
    	uploadMq.start();
    	uploadMqGzip.start();
    	uploadMqEXi.start();
    	
    	helloTransporter = new ProxyTransport(HELLO_URL);
    	uploadTransporter = new ProxyTransport(UPLOAD_URL);
    	rawTransporter = new ProxyTransport(RAW_URL);
    	
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/proxy", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("HTTP Server started");
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
        	boolean sendAsGzip = false;
        	boolean sendAsExi = false;
        	String compression = "null";
        	String requestType = "null";

        	System.out.println("\nHTTP  Request received");
        	
        	Headers requestHeaders = t.getRequestHeaders();
        	
        	if(requestHeaders.containsKey("SOAPAction"))
        	{
        		if(requestHeaders.getFirst("SOAPAction").equalsIgnoreCase(HELLO_SOAP_ACTION))
        		{
        			requestType = "hello";
        		}
        		else if(requestHeaders.getFirst("SOAPAction").equalsIgnoreCase(UPLOAD_SOAP_ACTION))
        		{
        			requestType = "upload";
        		}
        		else if(requestHeaders.getFirst("SOAPAction").equalsIgnoreCase(RAW_SOAP_ACTION))
        		{
        			requestType = "raw";
        		}
        	}
        	
        	if(requestHeaders.containsKey("Accept-Encoding"))
        	{
        		if(requestHeaders.getFirst("Accept-Encoding").equalsIgnoreCase("gzip"))
        		{
            		sendAsGzip = true;
            		compression = "gzip";
        		}
        		else if(requestHeaders.getFirst("Accept-Encoding").equalsIgnoreCase("exi"))
        		{
            		sendAsExi = true;
            		compression = "exi";
        		}
        	}

        	byte[] bytes = sendRequest(t, compression, requestType);

        	Headers h =  t.getResponseHeaders();
        	if(sendAsExi) { h.add("Content-Encoding", "exi"); }
        	if(sendAsGzip){ h.add("Content-Encoding", "gzip");}
        	t.sendResponseHeaders(200, bytes.length);
                
            OutputStream os = t.getResponseBody();
                
            os.write(bytes);
            os.close();
            System.out.println(compression);
        	System.out.println("Counter: " + counter);
        	counter++;
        }
    }
    
    static byte[] sendRequest(HttpExchange t, String compression, String requestType)
    {
    	InputStream i = t.getRequestBody();
    	
    	byte[] b = null;

    	byte[] data = null;
    	
		try {
			data = convertInputStreamToByteArray(i);
			System.out.println("PROXY: Request Data Size: " + data.length);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		requestHeaders = t.getRequestHeaders();
		String contentType = requestHeaders.getFirst("Content-Type");
		

    	System.out.println("Contacting Web Server");
    	try {
    		if(requestType.equalsIgnoreCase("hello"))
    		{b = helloTransporter.call(data, compression, HELLO_SOAP_ACTION, contentType);}
    		else if(requestType.equalsIgnoreCase("upload"))
    		{b = uploadTransporter.call(data, compression, UPLOAD_SOAP_ACTION, contentType);}
    		else if(requestType.equalsIgnoreCase("raw"))
    		{b = rawTransporter.call(data, compression, RAW_SOAP_ACTION, contentType);}
		} catch (IOException | XmlPullParserException e1) {
			System.out.println("Error contacting WS server " + errorCounter);
			errorCounter++;
			Toolkit.getDefaultToolkit().beep();
			//e1.printStackTrace();
		}
    	System.out.println("PROXY: Response Data Size: " + b.length);
    	
    	errorCounter = 0;
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