package com.example.historymodule.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Safely resizes images before sending them to AI vision. */
public final class HistoryImageReader {
    private static final int MAX_SIDE = 1280;
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    public ImageData readCompressed(Context context, Uri uri) throws Exception {
        if (context == null || uri == null) {
            throw new IllegalArgumentException("Chưa chọn ảnh.");
        }

        ContentResolver resolver = context.getApplicationContext().getContentResolver();
        String mimeType = resolver.getType(uri);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "image/jpeg";
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) throw new IllegalArgumentException("Không thể mở ảnh.");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IllegalArgumentException("Tệp đã chọn không phải ảnh hợp lệ.");
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE * 2);
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap;
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) throw new IllegalArgumentException("Không thể mở lại ảnh.");
            bitmap = BitmapFactory.decodeStream(input, null, decode);
        } catch (OutOfMemoryError error) {
            throw new IllegalArgumentException("Ảnh quá lớn. Hãy chọn ảnh nhỏ hơn.");
        }
        if (bitmap == null) throw new IllegalArgumentException("Ảnh không hợp lệ.");

        return compressBitmap(bitmap);
    }

    public ImageData fromBitmap(Bitmap bitmap) throws Exception {
        if (bitmap == null) throw new IllegalArgumentException("Không nhận được ảnh từ camera.");
        return compressBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false));
    }

    private ImageData compressBitmap(Bitmap decoded) throws Exception {
        Bitmap scaled = decoded;
        int maxSide = Math.max(decoded.getWidth(), decoded.getHeight());
        if (maxSide > MAX_SIDE) {
            float scale = MAX_SIDE / (float) maxSide;
            scaled = Bitmap.createScaledBitmap(
                    decoded,
                    Math.max(1, Math.round(decoded.getWidth() * scale)),
                    Math.max(1, Math.round(decoded.getHeight() * scale)),
                    true
            );
            if (scaled != decoded) decoded.recycle();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int quality = 84;
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output);
            while (output.size() > MAX_BYTES && quality > 52) {
                output.reset();
                quality -= 8;
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, output);
            }
            if (output.size() > MAX_BYTES) {
                throw new IllegalArgumentException("Ảnh vẫn quá lớn sau khi nén.");
            }
            return new ImageData(output.toByteArray(), "image/jpeg");
        } finally {
            scaled.recycle();
        }
    }

    private int calculateSampleSize(int width, int height, int target) {
        int sample = 1;
        int max = Math.max(width, height);
        while (max / sample > target && sample < 16) sample *= 2;
        return sample;
    }

    public static final class ImageData {
        private final byte[] bytes;
        private final String mimeType;

        public ImageData(byte[] bytes, String mimeType) {
            this.bytes = bytes == null ? new byte[0] : bytes;
            this.mimeType = mimeType == null ? "image/jpeg" : mimeType;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}
