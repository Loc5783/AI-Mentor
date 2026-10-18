package com.example.mentor.chat;

/** UI calls this contract on a worker; remote implementations may replace direct Gemini later. */
public interface TutorGateway {
  boolean configured();

  TutorReply reply(TutorRequest request) throws TutorFailure;
}
