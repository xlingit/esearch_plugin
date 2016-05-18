package org.elasticsearch.rest.action.readonlyrest;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;

public class ConfigurationHelper
{
  private static final String ES_YAML_CONF_PREFIX = "readonlyrest.";
  private static final String K_RESP_REQ_FORBIDDEN = "response_if_req_forbidden";
  public final boolean enabled;
  public final String forbiddenResponse;
  
  public ConfigurationHelper(Settings settings, ESLogger logger)
  {
    Settings s = settings.getByPrefix("readonlyrest.");
    if (!s.getAsBoolean("enable", Boolean.valueOf(false)).booleanValue())
    {
      logger.info("Readonly Rest plugin is installed, but not enabled", new Object[0]);
      this.enabled = false;
    }
    else
    {
      this.enabled = true;
    }
    String t = s.get("response_if_req_forbidden");
    if (t != null) {
      t.trim();
    }
    if (isNullOrEmpty(t)) {
      this.forbiddenResponse = null;
    } else {
      this.forbiddenResponse = t;
    }
  }
  
  public static boolean isNullOrEmpty(String s)
  {
    return (s == null) || (s.trim().length() == 0);
  }
}
