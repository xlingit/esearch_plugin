package org.elasticsearch.rest.action.readonlyrest.acl;

public class RuleConfigurationError
  extends RuntimeException
{
  public RuleConfigurationError(String msg, Throwable cause)
  {
    super(msg, cause);
  }
}
