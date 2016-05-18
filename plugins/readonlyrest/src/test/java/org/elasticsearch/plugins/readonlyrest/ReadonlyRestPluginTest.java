package org.elasticsearch.plugins.readonlyrest;

import java.util.Collection;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.plugin.readonlyrest.ReadonlyRestPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Test;

public class ReadonlyRestPluginTest extends ESIntegTestCase {
  @Override
  protected Collection<Class<? extends Plugin>> nodePlugins() {
    return pluginList(ReadonlyRestPlugin.class);
  }
  
  @Test
  public void testHealth(){
	  ClusterHealthStatus chs =ensureGreen("music");  
	  System.out.println("1231231");
	  System.out.println(chs.GREEN);
  }

}