package com.example.asm_final.chat;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.example.mentor.chat.TutorAttachment;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/**
 * App-private copies: a picker URI is never the persisted attachment, and names never become paths.
 */
public final class LocalChatFiles {
  public static final class FileFailure extends RuntimeException {
    public final boolean tooLarge;

    public FileFailure(boolean tooLarge) {
      super(tooLarge ? "FILE_TOO_LARGE" : "FILE_UNREADABLE");
      this.tooLarge = tooLarge;
    }
  }

  private final Context context;

  public LocalChatFiles(Context context) {
    this.context = context.getApplicationContext();
  }

  private File root(int owner) {
    if (owner <= 0) throw new IllegalArgumentException("Missing attachment owner");
    return new File(context.getFilesDir(), "general_chat/" + owner);
  }

  public File resolve(int owner, ChatRecords.FileItem item) {
    if (item == null || !item.storageKey.matches("[0-9a-fA-F-]{36}\\.bin"))
      throw new IllegalArgumentException("Invalid stored attachment key");
    try {
      File base = root(owner).getCanonicalFile(),
          file = new File(base, item.storageKey).getCanonicalFile();
      if (!file.getParentFile().equals(base))
        throw new IllegalArgumentException("Attachment escaped account root");
      return file;
    } catch (IOException invalid) {
      throw new FileFailure(false);
    }
  }

  private byte[] read(InputStream input) throws IOException {
    if (input == null) throw new IOException("Missing attachment stream");
    try (InputStream source = input;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = source.read(buffer)) != -1) {
        if (bytes.size() + count > TutorAttachment.MAX_BYTES) throw new FileFailure(true);
        bytes.write(buffer, 0, count);
      }
      if (bytes.size() == 0) throw new FileFailure(false);
      return bytes.toByteArray();
    }
  }

  private String mime(byte[] bytes) {
    String mime;
    if (bytes.length >= 3
        && (bytes[0] & 255) == 255
        && (bytes[1] & 255) == 216
        && (bytes[2] & 255) == 255) mime = "image/jpeg";
    else if (bytes.length >= 8
        && bytes[0] == (byte) 137
        && bytes[1] == 80
        && bytes[2] == 78
        && bytes[3] == 71
        && bytes[4] == 13
        && bytes[5] == 10
        && bytes[6] == 26
        && bytes[7] == 10) mime = "image/png";
    else if (bytes.length >= 12
        && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
        && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) mime = "image/webp";
    else if (bytes.length >= 5
        && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
      mime = "application/pdf";
    else {
      try {
        String text =
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        for (int i = 0; i < text.length(); i++)
          if (Character.isISOControl(text.charAt(i)) && "\n\r\t".indexOf(text.charAt(i)) < 0)
            throw new FileFailure(false);
        return "text/plain";
      } catch (CharacterCodingException invalid) {
        throw new FileFailure(false);
      }
    }
    if (mime.startsWith("image/")) {
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
      if (bounds.outWidth <= 0
          || bounds.outHeight <= 0
          || (long) bounds.outWidth * bounds.outHeight > 60000000L) throw new FileFailure(false);
    }
    return mime;
  }

  private ChatRecords.FileItem save(int owner, String name, byte[] bytes) {
    String mime = mime(bytes), id = UUID.randomUUID().toString(), key = id + ".bin";
    String label = (name == null ? "attachment" : name).replaceAll("[\\p{Cntrl}]", "").trim();
    if (label.isEmpty()) label = "attachment";
    label = label.substring(0, Math.min(120, label.length()));
    ChatRecords.FileItem item = new ChatRecords.FileItem(id, label, mime, key, bytes.length);
    File directory = root(owner);
    if (!directory.isDirectory() && !directory.mkdirs()) throw new FileFailure(false);
    File target = resolve(owner, item);
    try (FileOutputStream out = new FileOutputStream(target)) {
      out.write(bytes);
    } catch (IOException error) {
      target.delete();
      throw new FileFailure(false);
    }
    return item;
  }

  public ChatRecords.FileItem importUri(int owner, Uri uri) {
    String name = "attachment";
    try {
      try (Cursor c =
          context
              .getContentResolver()
              .query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
        if (c != null && c.moveToFirst() && !c.isNull(0)) name = c.getString(0);
      }
      return save(owner, name, read(context.getContentResolver().openInputStream(uri)));
    } catch (IOException | SecurityException invalid) {
      throw new FileFailure(false);
    }
  }

  public ChatRecords.FileItem importCamera(int owner, File source) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(source.getPath(), options);
    if (options.outWidth <= 0 || options.outHeight <= 0) throw new FileFailure(false);
    int sample = 1;
    while (Math.max(options.outWidth, options.outHeight) / sample > 2560) sample *= 2;
    options.inJustDecodeBounds = false;
    options.inSampleSize = sample;
    Bitmap original = BitmapFactory.decodeFile(source.getPath(), options);
    if (original == null) throw new FileFailure(false);
    Bitmap rotated = original;
    try {
      ExifInterface exif = new ExifInterface(source.getPath());
      int orientation =
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      Matrix matrix = new Matrix();
      if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90);
      else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180);
      else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270);
      else if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) matrix.postScale(-1, 1);
      else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) matrix.postScale(1, -1);
      rotated =
          Bitmap.createBitmap(
              original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      if (!rotated.compress(Bitmap.CompressFormat.JPEG, 85, bytes)
          || bytes.size() > TutorAttachment.MAX_BYTES) throw new FileFailure(true);
      return save(owner, "camera-" + System.currentTimeMillis() + ".jpg", bytes.toByteArray());
    } catch (IOException invalid) {
      throw new FileFailure(false);
    } finally {
      if (rotated != original) rotated.recycle();
      original.recycle();
    }
  }

  public TutorAttachment payload(int owner, ChatRecords.FileItem file) {
    File source = resolve(owner, file);
    if (source.length() != file.size) throw new FileFailure(false);
    try {
      return new TutorAttachment(file.name, file.mime, read(new FileInputStream(source)));
    } catch (IOException invalid) {
      throw new FileFailure(false);
    }
  }

  public void delete(int owner, ChatRecords.FileItem file) {
    File source = resolve(owner, file);
    if (source.exists()) source.delete();
  }
}
