package com.example.asm_final.competition;

public interface PublicProfileRepository {
  PublicLearner load(int owner, String profileId) throws Exception;

  byte[] avatar(int owner, String profileId) throws Exception;
}
