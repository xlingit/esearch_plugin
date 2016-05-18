package org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl;

import java.util.Arrays;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.readonlyrest.acl.RequestContext;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.MatcherWithWildcards;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.Rule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.RuleExitResult;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.RuleNotConfiguredException;

/**
 * Created by sscarduzio on 20/02/2016.
 */
public class IndicesRule extends Rule {

  private final static ESLogger logger = Loggers.getLogger(IndicesRule.class);

  protected MatcherWithWildcards m;

  public IndicesRule(Settings s) throws RuleNotConfiguredException {
    super(s);
    m = new MatcherWithWildcards(s, KEY);
  }

  @Override
  public RuleExitResult match(RequestContext rc) {
    if(m.match(rc.getIndices())){
      return MATCH;
    }
    logger.debug("This request uses the indices '" + Arrays.toString(rc.getIndices()) + "' and none of them is on the list.");
    return NO_MATCH;
  }

}
