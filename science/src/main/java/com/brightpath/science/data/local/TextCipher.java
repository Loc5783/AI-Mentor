package com.brightpath.science.data.local;

public interface TextCipher {
    String encrypt(String plainText);
    String decrypt(String storedText);
}
