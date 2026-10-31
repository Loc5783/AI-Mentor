package com.example.asm_final.supabase;
/** Tracks one heartbeat; unrelated replies cannot keep a stale connection alive. */
public final class RealtimeHeartbeat {
 private int sequence=1;
 private String pending;
 public synchronized String next() throws java.io.IOException {
  if(pending!=null) throw new java.io.IOException("Heartbeat not acknowledged");
  pending=Integer.toString(++sequence); return pending;
 }
 public synchronized void acknowledge(String ref) {
  if(ref.equals(pending)) pending=null;
 }
}
