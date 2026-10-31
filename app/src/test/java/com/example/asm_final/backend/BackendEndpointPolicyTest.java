package com.example.asm_final.backend;

import static org.junit.Assert.*;

import org.junit.Test;

public final class BackendEndpointPolicyTest {
  @Test
  public void localHttpWorksOnlyForExplicitDebugHostsAndPorts() {
    for (String host : new String[] {"127.0.0.1", "10.0.2.2", "localhost", "192.168.1.2", "10.24.33.105", "172.16.0.1"}) {
      assertTrue("Should allow local debug host " + host + " in debug", BackendEndpointPolicy.allowed("http://" + host + ":18080", true));
      assertFalse("Release must reject local HTTP host " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", false));
      assertFalse("HTTP host requires explicit port: " + host, BackendEndpointPolicy.allowed("http://" + host, true));
    }
  }

  @Test
  public void publicIpsAndDomainsRejectedOverHttp() {
    for (String host : new String[] {"8.8.8.8", "1.1.1.1"}) {
      assertFalse("Must reject public IP over HTTP: " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", true));
      assertFalse("Release must reject public IP over HTTP: " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", false));
    }
    for (String host : new String[] {"example.com", "10.0.2.2.example.com", "localhost.example.com"}) {
      assertFalse("Must reject public domain over HTTP: " + host, BackendEndpointPolicy.allowed("http://" + host + ":18080", true));
    }
  }

  @Test
  public void releaseAllowsValidHttpsWithoutCredentialsOrQuery() {
    assertTrue(BackendEndpointPolicy.allowed("https://api.school.example/v1", false));
    for (String invalid :
        new String[] {
          "",
          "broken",
          "https://user:pass@api.school.example",
          "https://api.school.example?token=x",
          "https://api.school.example#x",
          "https://api.school.example:0",
          "https://api.school.example:99999",
          "http://10.0.2.2:65536"
        }) assertFalse(invalid, BackendEndpointPolicy.allowed(invalid, true));
  }
}
