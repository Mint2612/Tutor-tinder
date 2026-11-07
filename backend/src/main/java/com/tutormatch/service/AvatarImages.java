package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import com.tutormatch.exception.BusinessException;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

/**
 * Turns an untrusted upload into a small, metadata-free JPEG. The type is decided by the file's
 * leading bytes, never by the client's Content-Type; re-encoding drops EXIF (including GPS) and
 * anything hidden after the image data. The browser already bakes EXIF rotation into the pixels
 * before uploading, so orientation tags are not interpreted here.
 */
final class AvatarImages {
  static final int MAX_BYTES = 2 * 1024 * 1024;
  static final int MAX_SIDE = 512;
  /** Refuse to decode pictures that would need a huge raster (decompression bombs). */
  static final long MAX_PIXELS = 24_000_000L;

  private static final float QUALITY = 0.85f;

  private AvatarImages() {}

  static byte[] normalize(byte[] bytes) {
    require(bytes != null && bytes.length > 0, 400, "Chưa chọn ảnh");
    require(bytes.length <= MAX_BYTES, 400, "Ảnh tối đa 2 MB");
    require(isJpeg(bytes) || isPng(bytes), 400, "Ảnh đại diện chỉ nhận JPEG hoặc PNG hợp lệ");
    try (var in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(in);
      require(readers.hasNext(), 400, "Không đọc được ảnh");
      var reader = readers.next();
      try {
        reader.setInput(in, true, true);
        long width = reader.getWidth(0), height = reader.getHeight(0);
        require(
            width > 0 && height > 0 && width * height <= MAX_PIXELS,
            400,
            "Ảnh quá lớn (tối đa 24 megapixel)");
        return encode(scale(reader.read(0)));
      } finally {
        reader.dispose();
      }
    } catch (BusinessException e) {
      throw e;
    } catch (IOException | RuntimeException e) {
      throw new BusinessException(400, "Không đọc được ảnh");
    }
  }

  private static boolean isJpeg(byte[] b) {
    return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
  }

  private static boolean isPng(byte[] b) {
    return b.length > 8
        && (b[0] & 0xFF) == 0x89
        && b[1] == 'P'
        && b[2] == 'N'
        && b[3] == 'G'
        && b[4] == 0x0D
        && b[5] == 0x0A
        && b[6] == 0x1A
        && b[7] == 0x0A;
  }

  /** Fits the picture inside MAX_SIDE (never enlarging) on a white background (no alpha). */
  private static BufferedImage scale(BufferedImage source) {
    double ratio =
        Math.min(1.0, (double) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
    int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
    int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
    var out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var g = out.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setColor(java.awt.Color.WHITE);
      g.fillRect(0, 0, width, height);
      g.drawImage(source, 0, 0, width, height, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  private static byte[] encode(BufferedImage image) throws IOException {
    var writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) throw new IOException("No JPEG writer");
    var writer = writers.next();
    var out = new ByteArrayOutputStream();
    try (var stream = ImageIO.createImageOutputStream(out)) {
      var params = writer.getDefaultWriteParam();
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(QUALITY);
      writer.setOutput(stream);
      writer.write(null, new IIOImage(image, null, null), params);
    } finally {
      writer.dispose();
    }
    return out.toByteArray();
  }
}
