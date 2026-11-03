package com.example.asm_final.supabase;
import org.junit.Test;
import static org.junit.Assert.*;
public class RealtimeHeartbeatTest {
 @Test public void acknowledgedHeartbeatContinues() throws Exception {
  RealtimeHeartbeat h=new RealtimeHeartbeat();String first=h.next();h.acknowledge(first);
  assertNotEquals(first,h.next());
 }
 @Test(expected=java.io.IOException.class) public void unrelatedReplyDoesNotAcknowledge() throws Exception {
  RealtimeHeartbeat h=new RealtimeHeartbeat();h.next();h.acknowledge("1");h.next();
 }
 @Test(expected=java.io.IOException.class) public void missingReplyTimesOut() throws Exception {
  RealtimeHeartbeat h=new RealtimeHeartbeat();h.next();h.next();
 }
}
