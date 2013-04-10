//<pre>

package render;

import java.awt.*;
import java.nio.*;
import java.awt.image.*;
import java.net.*;
//import javax.imageio.*;
import java.io.*;

/**
 * Texture handling
 */

public class Texture
{

  public int[]             texels;
  public int               height, width;
  private String           name;
  private ImagePyramid     pyramid            = null;
  public static boolean    useMIP             = true;
  private boolean          mip;
  public int               textname           = 0;
  public ByteBuffer        bytebuff;
  public int[]             packedPixels;
  public double            uclamp             = 1;
  public double            vclamp             = 1;

  public boolean           animated           = false;

  public Texture(String file, String name) throws IOException
  {
    this.name = name;
    readPixels(TextureReader.readImage(file));
    pyramid = new ImagePyramid(new ImageBuffer(packedPixels, width, height));
    mip = true;
  }

  public ImagePyramid getPyramid() {
     return pyramid;
  }

  public Texture(URL fileURL, String name) throws IOException
  {
    this.name = name;
    BufferedImage image = TextureReader.readImage(fileURL);
    if (image != null)
       readPixels(image);
  }

  public Texture(ByteBuffer buff, int h, int w, String name)
  {
    bytebuff = buff;
    height = h;
    width = w;
    mip = false;
  }

  /**
   * Constructor using an array of pixels
   * 
   * @param pix
   *          array of pixel colors using packed ints
   * @param h
   *          height of the image
   * @param w
   *          width of the image
   * @param name
   *          texture name
   * @param mip
   *          flag determining whether to use the multi image pyramid (MIP).
   */
  public Texture(int[] pix, int h, int w, String name, boolean mip)
  {
    texels = pix;
    height = h;
    width = w;
    pyramid = new ImagePyramid(new ImageBuffer(pix, w, h));
    this.mip = mip;
  }

  private static URL getResource(final String filename)
  {
    // Try to load resource from jar
    URL url = ClassLoader.getSystemResource(filename);
    // If not found in jar, then load from disk
    if (url == null)
    {
      try
      {
        url = new URL("file", "localhost", filename);
      }
      catch (Exception urlException )
      {
      } // ignore
    }
    return url;
  }

  private void readPixels(BufferedImage img)
  {
    height = img.getHeight();
    width = img.getWidth();
    packedPixels = new int[width * height];

    PixelGrabber pixelgrabber = new PixelGrabber(
      img, 0, 0, img.getWidth(), img.getHeight(), packedPixels, 0, img.getWidth());
    try {
      pixelgrabber.grabPixels();
    }
    catch (InterruptedException e ) {
      throw new RuntimeException();
    }

    //I am only doing RGB here, we may want transparent textures, so I'll have to add ARGB
    //Look at DiscTexture for inspiration

    ByteBuffer unpackedPixels = ByteBuffer.allocateDirect(packedPixels.length * 4);

    int powerof2 = 64;
    while (powerof2 < Math.max(width, height))
       powerof2 *= 2;

    texels = new int[powerof2 * powerof2];

    for (int row = img.getHeight() - 1; row >= 0; row--)
       for (int col = 0; col < img.getWidth(); col++) {
          int packedPixel = packedPixels[row * img.getWidth() + col];
          texels[row * powerof2 + col] = packedPixel;
          unpackedPixels.put((byte) (packedPixel >> 24 & 0xff));
          unpackedPixels.put((byte) (packedPixel >> 16 & 0xff));
          unpackedPixels.put((byte) (packedPixel >>  8 & 0xff));
          unpackedPixels.put((byte) (packedPixel       & 0xff));
       }

    //see if we need to pad
    if (powerof2 != width && powerof2 != height) {
      uclamp = (double)width  / powerof2;
      vclamp = (double)height / powerof2;

      int packed[] = new int[powerof2 * powerof2];
      ByteBuffer unpacked = ByteBuffer.allocateDirect(packed.length * 4);
      for (int x = 0; x < powerof2; x++)
        for (int y = 0; y < powerof2; y++)
          if (x < width && y < height)
            packed[x + y * powerof2] = packedPixels[x + y * width];
          else if (x >= width)
            packed[x + y * powerof2] = packed[width - 1 + y * powerof2];
          else if (y >= height)
            packed[x + y * powerof2] = packed[x + (height - 1) * powerof2];

      for (int row = powerof2 - 1; row >= 0; row--)
        for (int col = 0; col < powerof2; col++) {
          int packedPixel = packed[row * powerof2 + col];
          unpacked.put((byte) (packedPixel >> 24 & 0xff));
          unpacked.put((byte) (packedPixel >> 16 & 0xff));
          unpacked.put((byte) (packedPixel >>  8 & 0xff));
          unpacked.put((byte) (packedPixel       & 0xff));
        }

      packedPixels = packed;
      unpackedPixels = unpacked;
    }

    //texels = packedPixels;
    height = powerof2;
    width = powerof2;
    bytebuff = unpackedPixels;

  }

  /**
   * Constructor using an image object
   * 
   * @param src
   *          Image object
   * @param component
   *          the component that the image is associated with
   * @param name
   *          texture name
   * @param mip
   *          flag determining whether to use the multi image pyramid (MIP).
   */
  public Texture(Image src, String name, Component component, boolean mip)
  {
    ImageBuffer buffer = new ImageBuffer(src, component);
    texels = buffer.pix;
    height = buffer.getHeight();
    width = buffer.getWidth();
    pyramid = new ImagePyramid(buffer);
    this.mip = mip;
  }

  public final int getWidth() {
     return width;
  }

  public final int getHeight() {
     return height;
  }

  public final void set(int col, int row, int value) {
     texels[col + width * row] = value;
  }

  public final int get(int col, int row) {
     return texels[col + width * row];
  }

  /**
   * Returns the integer value from the texture at [u, v ].
   * 
   * @param u
   *          horizontal component
   * @param v
   *          vertical component
   * @param dx
   *          size of the pixel ( for use with mip )
   * @param dx
   *          size of the pixel ( for use with mip )
   * @param mult
   *          the scale factor for dx and dy ( for use with mip )
   */
  public final int getTexel(double u, double v, int dx, int dy, int mult)
  {
    if (u >= 0 && u < 1 && v >= 0 && v < 1) {
      if (mip && useMIP) {
        double size = ((double)dx / mult) * ((double)dy / mult);
        return pyramid.get(u, v, size);
      }
      return texels[(int) (v * height) * width + (int) (u * width)];

    }
    return texels[0];
  }
}
