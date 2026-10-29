package com.example.asm_final.backend;

import static org.junit.Assert.*;

import org.junit.Test;

public final class BackendEndpointPolicyTest {
  @Test
  public void localHttpWorksOnlyForExplicitDebugHostsAndPorts() {
    for (String host : new String[] {"127.0.0.1", "10.0.2.2", "localhost"}) {
      assertTrue("Should allow local debug host " + host + " in debug", BackendEndpointPolicy.allowed("http://" + host + ":18080", true));
      assertFalse("Release must reject local HTTP host " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", false));
      assertFalse("HTTP host requires explicit port: " + host, BackendEndpointPolicy.allowed("http://" + host, true));
    }
  }

  @Test
  public void publicAndLanIpsRejectedWhenNotExpanded() {
    for (String host : new String[] {"8.8.8.8", "1.1.1.1", "10.24.33.105", "192.168.1.2", "172.16.0.1"}) {
      assertFalse("Must reject public or unexpanded LAN IP over HTTP: " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", true));
      assertFalse("Release must reject public/LAN IP over HTTP: " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", false));
    }
    for (String host : new String[] {"example.com", "10.0.2.2.example.com", "localhost.example.com"}) {
      assertFalse("Must reject public domain over HTTP: " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", true));
    }
  }

  @Test
  public void releaseAllowsValidHttpsWithoutCredentialsOrQuery() {
    assertTrue(BackendEndpointPolicy.allowed("https://api.school.example/v1", false));
    assertTrue(BackendEndpointPolicy.allowed("https://api.school.example:8443/v1", false));
    for (String invalid :
        new String[] {
          "",
          "   ",
          "broken",
          "https://user:pass@api.school.example",
          "http://user:pass@127.0.0.1:18080",
          "https://api.school.example?token=x",
          "http://127.0.0.1:18080?token=x",
          "https://api.school.example#x",
          "http://127.0.0.1:18080#x",
          "https://api.school.example:0",
          "https://api.school.example:99999",
          "http://10.0.2.2:65536",
          "http://:18080",
          "https:///path"
        }) {
      assertFalse("Should reject invalid endpoint: " + invalid, BackendEndpointPolicy.allowed(invalid, true));
      assertFalse("Should reject invalid endpoint in release: " + invalid, BackendEndpointPolicy.allowed(invalid, false));
    }
  }
}
