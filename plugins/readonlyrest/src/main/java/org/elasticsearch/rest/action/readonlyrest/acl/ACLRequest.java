package org.elasticsearch.rest.action.readonlyrest.acl;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.http.netty.NettyHttpChannel;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.jboss.netty.channel.socket.SocketChannel;

public class ACLRequest
{
  private static final Pattern localhostRe = Pattern.compile("^(127(\\.\\d+){1,3}|[0:]+1)$");
  private static final String LOCALHOST = "127.0.0.1";
  private static ESLogger logger;
  private String address;
  private String uri;
  private Integer bodyLength;
  private RestRequest.Method method;
  
  public String toString()
  {
    return this.method + " " + this.uri + " len: " + this.bodyLength + " originator address: " + this.address;
  }
  
  public String getAddress()
  {
    return this.address;
  }
  
  public String getUri()
  {
    return this.uri;
  }
  
  public Integer getBodyLength()
  {
    return this.bodyLength;
  }
  
  public ACLRequest(RestRequest request, RestChannel channel)
  {
    this(request.uri(), getAddress(channel), Integer.valueOf(request.content().length()), request.method());
    String content = request.content().toUtf8();
  }
  
  public ACLRequest(String uri, String address, Integer bodyLength, RestRequest.Method method)
  {
    this.uri = uri;
    this.address = address;
    this.bodyLength = bodyLength;
    this.method = method;
  }
  
  static String getAddress(RestChannel channel)
  {
    String remoteHost = null;
    try
    {
      NettyHttpChannel obj = (NettyHttpChannel)channel;
      
      Field f = obj.getClass().getDeclaredField("channel");
      f.setAccessible(true);
      SocketChannel sc = (SocketChannel)f.get(obj);
      InetSocketAddress remoteHostAddr = sc.getRemoteAddress();
      remoteHost = remoteHostAddr.getAddress().getHostAddress();
      if (localhostRe.matcher(remoteHost).find()) {
        remoteHost = "127.0.0.1";
      }
    }
    catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException e)
    {
      e.printStackTrace();
      logger.error("error checking the host", e, new Object[0]);
      return null;
    }
    return remoteHost;
  }
  
  public RestRequest.Method getMethod()
  {
    return this.method;
  }
}
