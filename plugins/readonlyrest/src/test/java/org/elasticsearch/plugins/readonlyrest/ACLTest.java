package org.elasticsearch.plugins.readonlyrest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.Base64;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.netty.NettyHttpChannel;
import org.elasticsearch.http.netty.NettyHttpRequest;
import org.elasticsearch.http.netty.NettyHttpServerTransport;
import org.elasticsearch.plugin.readonlyrest.acl.ACL;
import org.elasticsearch.plugin.readonlyrest.acl.RequestContext;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.Block;
import org.elasticsearch.plugin.readonlyrest.acl.blocks.BlockExitResult;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestRequest.Method;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;

public class ACLTest {
  private static ACL acl;
  public static ACL mkACL(String fileName) {
    ACL _acl = null;
    try {
      byte[] encoded = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + fileName));
      String str = Charsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
      Settings s = Settings.builder().loadFromSource(str).build();
      _acl = new ACL(s);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return _acl;
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    acl = mkACL("/src/test/test_rules.yml");
  }

  public static RequestContext mockReq(String uri, String address, String apiKey, String authKey, Integer bodyLength, Method method, String xForwardedForHeader, final String[] _indices, String action) throws Throwable {
    RestRequest r = mock(RestRequest.class, RETURNS_DEEP_STUBS);
    when(r.method()).thenReturn(method);
    when(r.uri()).thenReturn(uri);
    when(r.getRemoteAddress()).thenReturn(new InetSocketAddress(address, 80));
    when(r.header("X-Forwarded-For")).thenReturn(xForwardedForHeader);
    when(r.header("X-Api-Key")).thenReturn(apiKey);
    when(r.header("Authorization")).thenReturn(authKey);
    when(r.content().length()).thenReturn(bodyLength);
    when(r.content().array()).thenReturn("test".getBytes());

    NettyHttpServerTransport nettyHttpServerTransport = mock(NettyHttpServerTransport.class);
    NettyHttpRequest nettyHttpRequest = mock(NettyHttpRequest.class);
    NettyHttpChannel c = new NettyHttpChannel(nettyHttpServerTransport, nettyHttpRequest, null, true);
    return new RequestContext(c, r, action, new ActionRequest() {
      private String[] indices = _indices == null ? new String[0] : _indices;

      @Override
      public ActionRequestValidationException validate() {
        return null;
      }

      public String[] indices() {
        return indices;
      }
    });
  }

  // Internal/External hosts
  @Test
  public final void testAcceptExternalGet() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.GET, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "8");
  }

  @Test
  public final void testAllowExternalOption() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.OPTIONS, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "8");
  }

  @Test
  public final void testNetMask() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "192.168.1.5", "", "", 0, Method.POST, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "4");
  }

  // Methods + hosts
  @Test
  public final void testRejectExternalPost() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertFalse(res.isMatch());
  }

  @Test
  public final void testAcceptInternalGet() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "127.0.0.1", "", "", 0, Method.GET, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "4");
  }

  @Test
  public final void testAcceptInternalHead() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "127.0.0.1", "", "", 0, Method.HEAD, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertEquals(res.getBlock().getName(), "4");
  }

  // Body length
  @Test
  public final void testRejectExternalGetWithBody() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 20, Method.GET, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertFalse(res.isMatch());
  }

  // URI REGEX
  @Test
  public final void testRejectExternalURIRegEx() throws Throwable {
    RequestContext rc = mockReq("/secret-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.GET, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.FORBID);
    assertEquals(res.getBlock().getName(), "5");
  }

  // API Keys
  @Test
  public final void testApiKey() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "1234567890", "", 0, Method.POST, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "3");
  }

  // HTTP Basic Auth
  @Test
  public final void testHttpBasicAuth() throws Throwable {
    String secret64 = Base64.encodeBytes("sales:p455wd".getBytes(Charsets.UTF_8));
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "Basic " + secret64, 0, Method.POST, null, null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "2");
  }

  @Test
  public final void testXforwardedForHeader() throws Throwable {
    RequestContext rc = mockReq("/index1/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, "9.9.9.9", null, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "1");
  }

  // index
  @Test
  public final void testIndexIsolation() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, new String[]{"public-idx"}, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "6");
  }

  @Test
  public final void testIndexWithWildcards() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, new String[]{"wildcard-123"}, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "9");
  }

  @Test
  public final void testIndexWithWildcardsExactMatch() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, new String[]{"wildcard-*"}, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "9");
  }

  @Test
  public final void testIndexWithPlus() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, new String[]{"+withplus"}, null);
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "10");
  }

  @Test
  public final void testIndexWithMinus() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, new String[]{"-withplus"}, null);
    BlockExitResult res = acl.check(rc);
    assertFalse(res.isMatch());
  }

  @Test
  public final void testAction() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, null, "action1");
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "11");
  }

  @Test
  public final void testActionWildcard() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, null, "action123asd");
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "12");
  }

  @Test
  public final void testActionWildcardExactMatch() throws Throwable {
    RequestContext rc = mockReq("/public-idx/_search?q=item.getName():fishingpole&size=200", "1.1.1.1", "", "", 0, Method.POST, null, null, "action*");
    BlockExitResult res = acl.check(rc);
    assertTrue(res.isMatch());
    assertTrue(res.getBlock().getPolicy() == Block.Policy.ALLOW);
    assertEquals(res.getBlock().getName(), "12");
  }

}