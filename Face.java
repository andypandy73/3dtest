import static java.lang.Math.*;

public class Face {
    // Direct vertex references — no List overhead
    Vertex v0, v1, v2;

    double[][] normals_model = new double[3][3];
    double[][] normals     = new double[3][3];
    double[]   brightness  = new double[3];

    Face(Vertex a, Vertex b, Vertex c) {
        v0 = a; v1 = b; v2 = c;
    }

    void Render(FrameBuffer fb, Texture texture) {
        int[]   buffer = fb.pixels;
        float[] zArr   = fb.depth;
        int bufWidth  = fb.width;
        int bufHeight = fb.height;

        // Backface culling
        double faceNz = (normals[0][2] + normals[1][2] + normals[2][2]) / 3.0;
        if (faceNz <= 0) return;

        int x0 = (int)((v0.x + 1) / 2 * bufWidth);
        int y0 = (int)((1 - v0.y) / 2 * bufHeight);
        int x1 = (int)((v1.x + 1) / 2 * bufWidth);
        int y1 = (int)((1 - v1.y) / 2 * bufHeight);
        int x2 = (int)((v2.x + 1) / 2 * bufWidth);
        int y2 = (int)((1 - v2.y) / 2 * bufHeight);

        // Depth cull
        double z0d = v0.z, z1d = v1.z, z2d = v2.z;
        if (z0d < -1 || z0d > 1 || z1d < -1 || z1d > 1 || z2d < -1 || z2d > 1) return;

        // Frustum cull
        if (x0 < 0 && x1 < 0 && x2 < 0) return;
        if (x0 >= bufWidth  && x1 >= bufWidth  && x2 >= bufWidth)  return;
        if (y0 < 0 && y1 < 0 && y2 < 0) return;
        if (y0 >= bufHeight && y1 >= bufHeight && y2 >= bufHeight) return;

        // Convert inner-loop values to float
        float z0 = (float)z0d, z1 = (float)z1d, z2 = (float)z2d;
        float b0 = (float)brightness[0], b1 = (float)brightness[1], b2 = (float)brightness[2];

        // UV seam correction (done in double for precision, then converted)
        double tu0 = v0.u, tu1 = v1.u, tu2 = v2.u;
        double tv0 = v0.v, tv1 = v1.v, tv2 = v2.v;
        if (abs(tu1 - tu0) > 0.5) tu1 += (tu1 < tu0) ? 1.0 : -1.0;
        if (abs(tu2 - tu0) > 0.5) tu2 += (tu2 < tu0) ? 1.0 : -1.0;

        float iw0 = (float)(1.0 / v0.wClip);
        float iw1 = (float)(1.0 / v1.wClip);
        float iw2 = (float)(1.0 / v2.wClip);
        float tu0w = (float)(tu0 * iw0), tu1w = (float)(tu1 * iw1), tu2w = (float)(tu2 * iw2);
        float tv0w = (float)(tv0 * iw0), tv1w = (float)(tv1 * iw1), tv2w = (float)(tv2 * iw2);

        int minX = max(0, min(x0, min(x1, x2)));
        int maxX = min(bufWidth - 1, max(x0, max(x1, x2)));
        int minY = max(0, min(y0, min(y1, y2)));
        int maxY = min(bufHeight - 1, max(y0, max(y1, y2)));

        int dx0 = x1 - x0, dy0 = y1 - y0;
        int dx1 = x2 - x1, dy1 = y2 - y1;
        int dx2 = x0 - x2, dy2 = y0 - y2;

        int startCross0 = dx0 * (minY - y0) - dy0 * (minX - x0);
        int startCross1 = dx1 * (minY - y1) - dy1 * (minX - x1);
        int startCross2 = dx2 * (minY - y2) - dy2 * (minX - x2);

        float totalCross = startCross0 + startCross1 + startCross2;
        if (totalCross == 0) return;
        float invTotal = 1.0f / totalCross;

        // Texture null check hoisted outside the pixel loops
        if (texture != null) {
            for (int y = minY; y <= maxY; y++) {
                int rowCross0 = startCross0, rowCross1 = startCross1, rowCross2 = startCross2;
                int rowOffset = y * bufWidth;
                for (int x = minX; x <= maxX; x++) {
                    if ((rowCross0 >= 0 && rowCross1 >= 0 && rowCross2 >= 0) ||
                            (rowCross0 <= 0 && rowCross1 <= 0 && rowCross2 <= 0)) {
                        float zPixel = (rowCross1 * z0 + rowCross2 * z1 + rowCross0 * z2) * invTotal;
                        int idx = rowOffset + x;
                        if (zPixel < zArr[idx]) {
                            zArr[idx] = zPixel;
                            float brt = max(0.0f, min(1.0f, (rowCross1 * b0 + rowCross2 * b1 + rowCross0 * b2) * invTotal));
                            float wDenom = rowCross1 * iw0 + rowCross2 * iw1 + rowCross0 * iw2;
                            float invW = 1.0f / wDenom;
                            float tu = (rowCross1 * tu0w + rowCross2 * tu1w + rowCross0 * tu2w) * invW;
                            float tv = (rowCross1 * tv0w + rowCross2 * tv1w + rowCross0 * tv2w) * invW;
                            int texColor = texture.sample(tu, tv);
                            int ri = (int)(((texColor >> 16) & 0xFF) * brt);
                            int gi = (int)(((texColor >>  8) & 0xFF) * brt);
                            int bi = (int)(( texColor        & 0xFF) * brt);
                            buffer[idx] = (255 << 24) | (ri << 16) | (gi << 8) | bi;
                        }
                    }
                    rowCross0 -= dy0; rowCross1 -= dy1; rowCross2 -= dy2;
                }
                startCross0 += dx0; startCross1 += dx1; startCross2 += dx2;
            }
        } else {
            for (int y = minY; y <= maxY; y++) {
                int rowCross0 = startCross0, rowCross1 = startCross1, rowCross2 = startCross2;
                int rowOffset = y * bufWidth;
                for (int x = minX; x <= maxX; x++) {
                    if ((rowCross0 >= 0 && rowCross1 >= 0 && rowCross2 >= 0) ||
                            (rowCross0 <= 0 && rowCross1 <= 0 && rowCross2 <= 0)) {
                        float zPixel = (rowCross1 * z0 + rowCross2 * z1 + rowCross0 * z2) * invTotal;
                        int idx = rowOffset + x;
                        if (zPixel < zArr[idx]) {
                            zArr[idx] = zPixel;
                            float brt = max(0.0f, min(1.0f, (rowCross1 * b0 + rowCross2 * b1 + rowCross0 * b2) * invTotal));
                            int grey = (int)(brt * 220);
                            buffer[idx] = (255 << 24) | (grey << 16) | (grey << 8) | grey;
                        }
                    }
                    rowCross0 -= dy0; rowCross1 -= dy1; rowCross2 -= dy2;
                }
                startCross0 += dx0; startCross1 += dx1; startCross2 += dx2;
            }
        }

    }
}
