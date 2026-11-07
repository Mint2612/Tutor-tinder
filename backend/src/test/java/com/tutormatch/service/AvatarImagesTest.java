package com.tutormatch.service;

import static org.junit.jupiter.api.Assertions.*;

import com.tutormatch.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AvatarImagesTest {
  static byte[] encode(BufferedImage image, String format) throws Exception {
    var out = new ByteArrayOutputStream();
    ImageIO.write(image, format, out);
    return out.toByteArray();
  }

  static BufferedImage decode(byte[] bytes) throws Exception {
    return ImageIO.read(new ByteArrayInputStream(bytes));
  }

  @Test
  void largePictureIsScaledDownKeepingItsProportionsAndBecomesJpeg() throws Exception {
    byte[] result =
        AvatarImages.normalize(encode(new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB), "png"));
    assertEquals((byte) 0xFF, result[0]);
    assertEquals((byte) 0xD8, result[1]);
    var image = decode(result);
    assertEquals(512, image.getWidth());
    assertEquals(307, image.getHeight());
  }

  @Test
  void smallPictureIsNeverEnlarged() throws Exception {
    var image =
        decode(AvatarImages.normalize(encode(new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB), "png")));
    assertEquals(100, image.getWidth());
    assertEquals(80, image.getHeight());
  }

  @Test
  void transparencyIsFlattenedOntoWhite() throws Exception {
    var transparent = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
    var image = decode(AvatarImages.normalize(encode(transparent, "png")));
    int pixel = image.getRGB(20, 20) & 0xFFFFFF;
    // JPEG is lossy, so allow a tiny deviation from pure white.
    for (int shift = 0; shift <= 16; shift += 8) assertTrue(((pixel >> shift) & 0xFF) >= 250);
  }

  @Test
  void anythingThatIsNotARealJpegOrPngIsRefused() throws Exception {
    for (byte[] bad :
        new byte[][] {
          "GIF89a....".getBytes(),
          "plain text, not an image".getBytes(),
          new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3},
          java.util.Arrays.copyOf(encode(new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB), "png"), 30),
          new byte[0]
        }) {
      assertEquals(400, assertThrows(BusinessException.class, () -> AvatarImages.normalize(bad)).status());
    }
  }

  @Test
  void hugeRastersAndOversizedFilesAreRefusedBeforeDecoding() throws Exception {
    // 30 megapixels of one colour compress to a few KB, the classic decompression bomb.
    byte[] bomb = encode(new BufferedImage(6000, 5000, BufferedImage.TYPE_BYTE_GRAY), "png");
    assertTrue(bomb.length < AvatarImages.MAX_BYTES);
    assertEquals(400, assertThrows(BusinessException.class, () -> AvatarImages.normalize(bomb)).status());
    byte[] big = new byte[AvatarImages.MAX_BYTES + 1];
    big[0] = (byte) 0xFF;
    big[1] = (byte) 0xD8;
    big[2] = (byte) 0xFF;
    assertEquals(400, assertThrows(BusinessException.class, () -> AvatarImages.normalize(big)).status());
  }
}
