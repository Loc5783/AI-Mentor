package com.example.asm_final.accounts;

public final class UserProfile {
  public final String fullName, address, phone, email, avatar, bio;

  public UserProfile(String fullName, String address, String phone, String email, String avatar) {
    this(fullName, address, phone, email, avatar, "");
  }

  public UserProfile(
      String fullName, String address, String phone, String email, String avatar, String bio) {
    this.fullName = fullName;
    this.address = address;
    this.phone = phone;
    this.email = email;
    this.avatar = avatar;
    this.bio = bio == null ? "" : bio.trim();
  }
}
