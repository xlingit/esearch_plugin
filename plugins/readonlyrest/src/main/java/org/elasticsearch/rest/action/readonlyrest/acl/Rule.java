package org.elasticsearch.rest.action.readonlyrest.acl;

import java.util.List;
import java.util.regex.Pattern;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.readonlyrest.ConfigurationHelper;

import com.google.common.collect.Lists;

public class Rule
{
  String name;
  Type type;
  Pattern uri_re;
  Integer maxBodyLenght;
  List<String> addresses;
  private List<RestRequest.Method> methods;
  String stringRepresentation;
  
  public static enum Type
  {
    ALLOW,  FORBID;
    
    private Type() {}
    
    public static String valuesString()
    {
      StringBuilder sb = new StringBuilder();
      for (Type v : values()) {
        sb.append(v.toString()).append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
    }
  }
  
  public Rule(String name, Type type, Pattern uri_re, Integer bodyLenght, List<String> addresses, List<RestRequest.Method> methods, String toString)
  {
    this.name = name;
    this.type = type;
    this.uri_re = uri_re;
    this.maxBodyLenght = bodyLenght;
    this.addresses = addresses;
    this.methods = methods;
    this.stringRepresentation = toString;
  }
  
  public static Rule build(Settings s)
  {
    List<String> hosts = null;
    String[] a = s.getAsArray("hosts");
    if ((a != null) && (a.length > 0))
    {
      hosts = Lists.newArrayList();
      for (int i = 0; i < a.length; i++) {
        if (!ConfigurationHelper.isNullOrEmpty(a[i])) {
          hosts.add(a[i].trim());
        }
      }
    }
    a = s.getAsArray("methods");
    List<RestRequest.Method> methods = null;
    if ((a != null) && (a.length > 0)) {
      try
      {
        for (String string : a)
        {
          RestRequest.Method m = RestRequest.Method.valueOf(string.trim().toUpperCase());
          if (methods == null) {
            methods = Lists.newArrayList();
          }
          methods.add(m);
        }
      }
      catch (Throwable t)
      {
        throw new RuleConfigurationError("Invalid HTTP method found in configuration " + a, t);
      }
    }
    Pattern uri_re = null;
    String tmp = s.get("uri_re");
    if (!ConfigurationHelper.isNullOrEmpty(tmp)) {
      uri_re = Pattern.compile(tmp.trim());
    }
    String name = s.get("name");
    
    String sType = s.get("type");
    if (sType == null) {
      throw new RuleConfigurationError("The field \"type\" is mandatory and should be either of " + Type.valuesString() + ". If this field is correct, check the YAML indentation is correct.", null);
    }
    Type type = Type.valueOf(sType.toUpperCase());
    Integer maxBodyLength = s.getAsInt("maxBodyLength", null);
    if ((!ConfigurationHelper.isNullOrEmpty(name)) && (type != null) && ((uri_re != null) || (maxBodyLength != null) || (hosts != null) || (methods != null))) {
      return new Rule(name.trim(), type, uri_re, maxBodyLength, hosts, methods, s.toDelimitedString(' '));
    }
    throw new RuleConfigurationError("insufficient or invalid configuration for rule: '" + name + "'", null);
  }
  
  public String toString()
  {
    return this.stringRepresentation;
  }
  
  public boolean matchesAddress(String address)
  {
    if (this.addresses == null) {
      return true;
    }
    return this.addresses.contains(address);
  }
  
  public boolean matchesMaxBodyLength(Integer len)
  {
    if (this.maxBodyLenght == null) {
      return true;
    }
    return len.intValue() <= this.maxBodyLenght.intValue();
  }
  
  public boolean matchesUriRe(String uri)
  {
    if (this.uri_re == null) {
      return true;
    }
    return this.uri_re.matcher(uri).find();
  }
  
  public boolean mathesMethods(RestRequest.Method method)
  {
    if (this.methods == null) {
      return true;
    }
    return this.methods.contains(method);
  }
}
