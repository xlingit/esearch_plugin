package org.elasticsearch.plugin.readonlyrest;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;

public class ReadonlyRestPlugin
  extends Plugin
{
  public String name()
  {
    return "readonlyrest";
  }
  
  public String description()
  {
    return "Reject attempts to change data, so we can expose this REST API to clients";
  }
  
  public void onModule(RestModule module)
  {
    module.addRestAction(ReadonlyRestAction.class);
  }
}
