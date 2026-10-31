package com.example.asm_final.backend;

import static org.junit.Assert.*;

import org.junit.Test;

public final class BackendEndpointPolicyTest {
  @Test
  public void localHttpWorksOnlyForExplicitDebugHostsAndPorts() {
    for (String host : new String[] {"127.0.0.1", "10.0.2.2"}) {
      assertTrue(BackendEndpointPolicy.allowed("http://" + host + ":8000", true));
      assertFalse(BackendEndpointPolicy.allowed("http://" + host + ":8000", false));
      assertFalse(BackendEndpointPolicy.allowed("http://" + host, true));
    }
    for (String host : new String[] {"example.com", "192.168.1.2", "10.0.2.2.example.com"})
      assertFalse(BackendEndpointPolicy.allowed("http://" + host + ":8000", true));
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
