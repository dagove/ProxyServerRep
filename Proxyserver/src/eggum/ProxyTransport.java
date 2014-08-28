package eggum;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.transport.ServiceConnection;
import org.ksoap2.transport.ServiceConnectionSE;
import org.ksoap2.transport.Transport;
import org.xmlpull.v1.XmlPullParserException;

import eggum.ExiJava;

public class ProxyTransport extends Transport
{
  ExiJava exi = null;
	
  public ProxyTransport(String url)
  {
    super(null, url);
  }

  public ProxyTransport(Proxy proxy, String url)
  {
    super(proxy, url);
  }

  public ProxyTransport(String url, int timeout)
  {
    super(url, timeout);
  }

  public ProxyTransport(Proxy proxy, String url, int timeout) {
    super(proxy, url, timeout);
  }

  public ProxyTransport(String url, int timeout, int contentLength)
  {
    super(url, timeout);
  }

  public ProxyTransport(Proxy proxy, String url, int timeout, int contentLength) {
    super(proxy, url, timeout);
  }
  
  public void call(String soapAction, SoapEnvelope envelope)
    throws IOException, XmlPullParserException
  {
    call(soapAction, envelope, null);
  }

  public List call(String soapAction, SoapEnvelope envelope, List headers) throws IOException, XmlPullParserException
  {
    return call(soapAction, envelope, headers, null);
  }
  
  public byte[] call(byte[] data, String compression, String soapAction, String contentType) throws IOException, XmlPullParserException
  {
	  boolean gzipData = false;
	  boolean exiData = false;
	  List headers = null;
	  File outputFile = null;

	  if (soapAction == null) {
	      soapAction = "\"\"";
	    }	  
	  	if(compression.equalsIgnoreCase("gzip")) {gzipData = true;}
	  	else if(compression.equalsIgnoreCase("exi")) {exiData = true;}

	    byte[] requestData = null;

	    if(exiData)
	    {
	    	if(exi == null) {exi= new ExiJava();}   	
	    	try {
				requestData = exi.decompressByteArray(data);
			} catch (EfficientXMLException e) {
				e.printStackTrace();
			}
	    }	    
	    else if(gzipData)
	    {
	    	InputStream is = new ByteArrayInputStream(data);	    	
	    	GZIPInputStream gis = new GZIPInputStream(is);	    	
	    	requestData = convertInputStreamToByteArray(gis);	    	
	    }
	    else
	    {
	    	requestData = data;
	    }

	    this.requestDump = (this.debug ? new String(requestData) : null);
	    this.responseDump = null;

	    ServiceConnection connection = getServiceConnection();
	    connection.setRequestProperty("User-Agent", "ksoap2-android/2.6.0+");
	    connection.setRequestProperty("Content-Type", contentType);
	    
	    if(("text/xml;charset=utf-8").equals(contentType))
	    {
	    	connection.setRequestProperty("SOAPAction", soapAction);
	    }	    

	    connection.setRequestProperty("Connection", "close");
	    connection.setRequestProperty("Accept-Encoding", "gzip");
	    connection.setRequestProperty("Content-Length", "" + 179);

	    connection.setRequestMethod("POST");
	    OutputStream os = connection.openOutputStream();
	    
		os.write(requestData, 0, requestData.length);
	    os.flush();
	    os.close();
	    
	    requestData = null;
	    
	    List retHeaders = null;
	    byte[] buf = null;
	    int contentLength = 8192;
	    boolean gZippedContent = false;
	    InputStream is;
	    try { int status = connection.getResponseCode();
	      if (status != 200) {
	        throw new IOException("HTTP request failed, HTTP status: " + status);
	      }

	      retHeaders = connection.getResponseProperties();
	      for (int i = 0; i < retHeaders.size(); i++) {
	        HeaderProperty hp = (HeaderProperty)retHeaders.get(i);

	        if (null != hp.getKey())
	        {
	          if ((hp.getKey().equalsIgnoreCase("content-length")) && 
	            (hp.getValue() != null)) {
	            try {
	              contentLength = Integer.parseInt(hp.getValue());
	            } catch (NumberFormatException nfe) {
	              System.out.println(nfe);
	              contentLength = 8192;
	            }
	          }
	          else{
	        	  //System.out.println(" no contentLength " + hp.getKey() );
	          }
	          if ((hp.getKey().equalsIgnoreCase("Content-Encoding")) && (hp.getValue().equalsIgnoreCase("gzip")))
	          {
	            gZippedContent = true;
	            break;
	          }
	        }
	      }
	      //InputStream is;
	      if (gZippedContent) {
	        is = getUnZippedInputStream(new BufferedInputStream(connection.openInputStream(), contentLength));
	      }
	      else
	        is = new BufferedInputStream(connection.openInputStream(), contentLength);
	    }
	    catch (IOException e)	
	    {
	      if (gZippedContent) {
	        is = getUnZippedInputStream(new BufferedInputStream(connection.getErrorStream(), contentLength));
	      }
	      else {
	        is = new BufferedInputStream(connection.getErrorStream(), contentLength);
	      }

	      if ((this.debug) && (is != null))
	      {
	        readDebug(is, contentLength, outputFile);
	      }

	      connection.disconnect();
	      throw e;
	    }

	    if (this.debug) {
	      is = readDebug(is, contentLength, outputFile);
	    }

	    os = null;
	    buf = null;
	    
		byte[] buff = new byte[1024];
	    ByteArrayOutputStream out = new ByteArrayOutputStream(8192);	    
	    int n = 0;
	    while ((n = is.read(buff)) >= 0) {
	        out.write(buff, 0, n);
	    }
	    is.close();
	    out.close();
	    
	    byte[] b = out.toByteArray();
	    
	    if(gzipData)
	    {
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try{
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(b);
            gzipOutputStream.close();
        } catch(IOException e){
            throw new RuntimeException(e);
        }
        b = byteArrayOutputStream.toByteArray();

	    }
	    else if(exiData)
	    {
	    	byte[] b2 = null;
	    	try {
				b2 = exi.compressByteArray(b);
			} catch (EfficientXMLException e) {
				e.printStackTrace();
			}
	    	b = b2;
	    }
	    
	    return b;
  }

  private InputStream readDebug(InputStream is, int contentLength, File outputFile)
    throws IOException
  {
    OutputStream bos;
    if (outputFile != null) {
      bos = new FileOutputStream(outputFile);
    }
    else {
      bos = new ByteArrayOutputStream(contentLength > 0 ? contentLength : 262144);
    }

    byte[] buf = new byte[256];
    while (true)
    {
      int rd = is.read(buf, 0, 256);
      if (rd == -1) {
        break;
      }
      bos.write(buf, 0, rd);
    }

    bos.flush();
    if ((bos instanceof ByteArrayOutputStream)) {
      buf = ((ByteArrayOutputStream)bos).toByteArray();
    }
    //OutputStream bos = null;
    this.responseDump = new String(buf);
    is.close();
    return new ByteArrayInputStream(buf);
  }

  private InputStream getUnZippedInputStream(InputStream inputStream)
    throws IOException
  {
    try
    {
      return (GZIPInputStream)inputStream; } catch (ClassCastException e) {
    }
    return new GZIPInputStream(inputStream);
  }

  public ServiceConnection getServiceConnection() throws IOException
  {
    return new ServiceConnectionSE(this.proxy, this.url, this.timeout);
  }
  
  static String convertStreamToString(java.io.InputStream is) {
      java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
      return s.hasNext() ? s.next() : "";
  }
  
  public byte[] convertInputStreamToByteArray(InputStream is) throws IOException
  {
	  byte[] buff = new byte[8192]; // 64*1024 or some size, can try out different sizes for performance
	  ByteArrayOutputStream out = new ByteArrayOutputStream(8192);	    
	  int n = 0;
	  while ((n = is.read(buff)) >= 0) {
		  out.write(buff, 0, n);
	  }
	  is.close();
	  out.close();
  
	  return out.toByteArray();
  }

@Override
public List call(String arg0, SoapEnvelope arg1, List arg2, File arg3)
		throws IOException, XmlPullParserException {
	// TODO Auto-generated method stub
	return null;
}
  
  
}