package com.example.asm_final.accounts;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.Test;
import java.util.EnumMap;
import java.util.Map;
import static org.junit.Assert.*;

public final class QrCodeEncoderTest {

  @Test
  public void testQrCodeEncodingAndDecodingMatchToken() throws Exception {
    String token = "qr_9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1";
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    hints.put(EncodeHintType.MARGIN, 2);

    QRCodeWriter writer = new QRCodeWriter();
    BitMatrix matrix = writer.encode(token, BarcodeFormat.QR_CODE, 250, 250, hints);
    assertNotNull(matrix);

    LuminanceSource source = new BitMatrixLuminanceSource(matrix);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    Result result = new MultiFormatReader().decode(bitmap);

    assertNotNull(result);
    assertEquals(token, result.getText());
  }

  @Test
  public void testLongTokenEncodingAndDecoding() throws Exception {
    String longToken = "qr_very_long_invitation_token_string_with_uuid_and_random_padding_1234567890abcdef";
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    hints.put(EncodeHintType.MARGIN, 4);

    QRCodeWriter writer = new QRCodeWriter();
    BitMatrix matrix = writer.encode(longToken, BarcodeFormat.QR_CODE, 300, 300, hints);
    assertNotNull(matrix);

    LuminanceSource source = new BitMatrixLuminanceSource(matrix);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    Result result = new MultiFormatReader().decode(bitmap);

    assertEquals(longToken, result.getText());
  }

  private static final class BitMatrixLuminanceSource extends LuminanceSource {
    private final BitMatrix clientMatrix;

    protected BitMatrixLuminanceSource(BitMatrix matrix) {
      super(matrix.getWidth(), matrix.getHeight());
      this.clientMatrix = matrix;
    }

    @Override
    public byte[] getRow(int y, byte[] row) {
      int width = getWidth();
      if (row == null || row.length < width) {
        row = new byte[width];
      }
      for (int x = 0; x < width; x++) {
        row[x] = (byte) (clientMatrix.get(x, y) ? 0 : 255);
      }
      return row;
    }

    @Override
    public byte[] getMatrix() {
      int width = getWidth();
      int height = getHeight();
      byte[] matrix = new byte[width * height];
      for (int y = 0; y < height; y++) {
        System.arraycopy(getRow(y, null), 0, matrix, y * width, width);
      }
      return matrix;
    }

    @Override
    public boolean isCropSupported() {
      return false;
    }

    @Override
    public boolean isRotateSupported() {
      return false;
    }
  }
}
