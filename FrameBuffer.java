import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;

public class FrameBuffer {
    public final int width;
    public final int height;
    public final int[]   pixels;
    public final float[] depth;
    public final BufferedImage image;

    public FrameBuffer(int width, int height) {
        this.width  = width;
        this.height = height;
        this.image  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        this.depth  = new float[width * height];
    }

    public void clear() {
        IntStream.range(0, pixels.length).parallel().forEach(i -> {
            pixels[i] = 0xFF000000;
            depth[i] = Float.MAX_VALUE;
        });
    }
}
