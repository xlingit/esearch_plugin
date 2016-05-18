package org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.readonlyrest.ConfigurationHelper;
import org.elasticsearch.plugin.readonlyrest.acl.RequestContext;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.Rule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.RuleExitResult;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.RuleNotConfiguredException;
import org.elasticsearch.rest.action.readonlyrest.acl.RuleConfigurationError;

/**
 * Created by sscarduzio on 13/02/2016.
 */
public class UriReRule extends Rule {

  private Pattern uri_re = null;

  public UriReRule(Settings s) throws RuleNotConfiguredException {
    super(s);

    String tmp = s.get(KEY);
    if (!ConfigurationHelper.isNullOrEmpty(tmp)) {
      try{
        uri_re = Pattern.compile(tmp.trim());
      }
      catch (PatternSyntaxException e) {
        throw new RuleConfigurationError("invalid 'uri_re' regexp", e);
      }

    }
    else {
      throw new RuleNotConfiguredException();
    }
  }

  @Override
  public RuleExitResult match(RequestContext rc) {
    if (uri_re == null) {
      return NO_MATCH;
    }
    return uri_re.matcher(rc.getRequest().uri()).find() ? MATCH : NO_MATCH;
  }
}
