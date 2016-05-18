package org.elasticsearch.plugin.readonlyrest.acl.blocks;

import java.util.Set;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.readonlyrest.acl.RequestContext;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.Rule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.RuleExitResult;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.RuleNotConfiguredException;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.ActionsRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.ApiKeysRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.AuthKeyRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.HostsRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.IndicesRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.KibanaAccessRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.MaxBodyLengthRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.MethodsRule;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.rules.impl.UriReRule;
import org.elasticsearch.rest.action.readonlyrest.acl.RuleConfigurationError;

import com.google.common.collect.Sets;

/**
 * Created by sscarduzio on 13/02/2016.
 */
public class Block {
  private ESLogger logger;

  private final String name;
  private final Policy policy;
  private boolean authHeaderAccepted = false;
  private Set<Rule> conditionsToCheck = Sets.newHashSet();

  public Block(Settings s, ESLogger logger) {
    this.name = s.get("name");
    String sPolicy = s.get("type");
    this.logger = logger;
    if (sPolicy == null) {
      throw new RuleConfigurationError("The field \"type\" is mandatory and should be either of " + Block.Policy.valuesString() + ". If this field is correct, check the YAML indentation is correct.", null);
    }


    policy = Block.Policy.valueOf(sPolicy.toUpperCase());

    // Won't add the condition if its configuration is not found
    try {
      conditionsToCheck.add(new HostsRule(s));
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new ApiKeysRule(s));
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new AuthKeyRule(s));
      authHeaderAccepted = true;
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new UriReRule(s));
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new MaxBodyLengthRule(s));
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new MethodsRule(s));
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new KibanaAccessRule(s));
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new IndicesRule(s));
    } catch (RuleNotConfiguredException e) {
    }
    try {
      conditionsToCheck.add(new ActionsRule(s));
    } catch (RuleNotConfiguredException e) {
    }
  }

  public String getName() {
    return name;
  }

  public Policy getPolicy() {
    return policy;
  }

  public boolean isAuthHeaderAccepted() {
    return authHeaderAccepted;
  }

  public enum Policy {
    ALLOW, FORBID;

    public static String valuesString() {
      StringBuilder sb = new StringBuilder();
      for (Policy v : values()) {
        sb.append(v.toString()).append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
    }
  }

  /**
   * Check all the conditions of this rule and return a rule exit result
   *
   */

  public BlockExitResult check(RequestContext rc) {
    boolean match = true;

    for (Rule condition : conditionsToCheck) {
      // Exit at the first rule that matches the request
      RuleExitResult condExitResult = condition.match(rc);
      // a block matches if ALL rules match
      match &= condExitResult.isMatch();
    }
    if (match) {
      logger.debug("matched " + this);
      return new BlockExitResult(this, true);
    }
    logger.debug("[" + name + "] request matches no rules, forbidden by default: req: " + rc);

    return BlockExitResult.NO_MATCH;
  }

  @Override
  public String toString() {
    return "readonlyrest Rules Block :: { name: '" + name + "', policy: " + policy + "}";
  }
}
