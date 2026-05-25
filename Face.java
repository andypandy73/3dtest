import static java.lang.Math.*;

public class Face {
    // Direct vertex references — no List overhead
    Vertex v0, v1, v2;

    double[][] normals_model = new double[3][3];
    double[][] normals     = new double[3][3];
    double[]   brightness  = new double[3];

    // View-space tangents per corner and per-face view direction (set each frame by Mesh)
    float[][] tangents = new float[3][3];
    float fvx, fvy, fvz;

    Face(Vertex a, Vertex b, Vertex c) {
        v0 = a; v1 = b; v2 = c;
    }

    void Render(FrameBuffer fb, Texture texture,
                Texture normalMap, Texture metallicMap, Texture roughnessMap,
                float lx, float ly, float lz) {
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

        float z0 = (float)z0d, z1 = (float)z1d, z2 = (float)z2d;
        float b0 = (float)brightness[0], b1 = (float)brightness[1], b2 = (float)brightness[2];

        // Per-corner normals
        float n0x = (float)normals[0][0], n0y = (float)normals[0][1], n0z = (float)normals[0][2];
        float n1x = (float)normals[1][0], n1y = (float)normals[1][1], n1z = (float)normals[1][2];
        float n2x = (float)normals[2][0], n2y = (float)normals[2][1], n2z = (float)normals[2][2];

        // Per-corner tangents
        float t0x = tangents[0][0], t0y = tangents[0][1], t0z = tangents[0][2];
        float t1x = tangents[1][0], t1y = tangents[1][1], t1z = tangents[1][2];
        float t2x = tangents[2][0], t2y = tangents[2][1], t2z = tangents[2][2];

        boolean hasPBR = (normalMap != null || metallicMap != null || roughnessMap != null);

        // UV seam correction
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
                            float wDenom = rowCross1 * iw0 + rowCross2 * iw1 + rowCross0 * iw2;
                            float invW = 1.0f / wDenom;
                            float tu = (rowCross1 * tu0w + rowCross2 * tu1w + rowCross0 * tu2w) * invW;
                            float tv = (rowCross1 * tv0w + rowCross2 * tv1w + rowCross0 * tv2w) * invW;
                            int texColor = texture.sample(tu, tv);

                            int ri, gi, bi;
                            if (hasPBR) {
                                // Interpolate normal
                                float nx = (rowCross1*n0x + rowCross2*n1x + rowCross0*n2x) * invTotal;
                                float ny = (rowCross1*n0y + rowCross2*n1y + rowCross0*n2y) * invTotal;
                                float nz = (rowCross1*n0z + rowCross2*n1z + rowCross0*n2z) * invTotal;
                                float nlen = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
                                if (nlen > 1e-6f) { nx /= nlen; ny /= nlen; nz /= nlen; }

                                // Interpolate tangent
                                float tax = (rowCross1*t0x + rowCross2*t1x + rowCross0*t2x) * invTotal;
                                float tay = (rowCross1*t0y + rowCross2*t1y + rowCross0*t2y) * invTotal;
                                float taz = (rowCross1*t0z + rowCross2*t1z + rowCross0*t2z) * invTotal;
                                float tlen = (float)Math.sqrt(tax*tax + tay*tay + taz*taz);
                                if (tlen > 1e-6f) { tax /= tlen; tay /= tlen; taz /= tlen; }

                                // Apply normal map via TBN
                                if (normalMap != null) {
                                    int nc = normalMap.sample(tu, tv);
                                    float tnx = ((nc >> 16) & 0xFF) / 127.5f - 1.0f;
                                    float tny = ((nc >>  8) & 0xFF) / 127.5f - 1.0f;
                                    float tnz = ( nc        & 0xFF) / 127.5f - 1.0f;
                                    // Bitangent = cross(n, t)
                                    float bx = ny*taz - nz*tay;
                                    float by = nz*tax - nx*taz;
                                    float bz = nx*tay - ny*tax;
                                    float wnx = tnx*tax + tny*bx + tnz*nx;
                                    float wny = tnx*tay + tny*by + tnz*ny;
                                    float wnz = tnx*taz + tny*bz + tnz*nz;
                                    float wlen = (float)Math.sqrt(wnx*wnx + wny*wny + wnz*wnz);
                                    if (wlen > 1e-6f) { nx = wnx/wlen; ny = wny/wlen; nz = wnz/wlen; }
                                }

                                float diffuse = max(0f, nx*lx + ny*ly + nz*lz);

                                float rough = roughnessMap != null
                                    ? ((roughnessMap.sample(tu, tv) >> 16) & 0xFF) / 255f : 0.5f;
                                float metal = metallicMap != null
                                    ? ((metallicMap.sample(tu, tv) >> 16) & 0xFF) / 255f : 0.0f;

                                // Blinn-Phong specular using per-face view direction
                                float hx = lx + fvx, hy = ly + fvy, hz = lz + fvz;
                                float hlen = (float)Math.sqrt(hx*hx + hy*hy + hz*hz);
                                if (hlen > 1e-6f) { hx /= hlen; hy /= hlen; hz /= hlen; }
                                float nDotH = max(0f, nx*hx + ny*hy + nz*hz);
                                float shininess = 2f + (1f - rough) * 30f;
                                float spec = (float)Math.pow(nDotH, shininess);

                                float ar = ((texColor >> 16) & 0xFF) / 255f;
                                float ag = ((texColor >>  8) & 0xFF) / 255f;
                                float ab = ( texColor        & 0xFF) / 255f;

                                float diffBrt = 0.24f + diffuse * 1.33f * (1f - metal * 0.7f);
                                // Specular tinted by albedo for metals, white for dielectrics
                                float specR = spec * (metal * ar + (1f - metal) * 0.04f) * 5f;
                                float specG = spec * (metal * ag + (1f - metal) * 0.04f) * 5f;
                                float specB = spec * (metal * ab + (1f - metal) * 0.04f) * 5f;

                                ri = min(255, (int)((ar * diffBrt + specR) * 255f));
                                gi = min(255, (int)((ag * diffBrt + specG) * 255f));
                                bi = min(255, (int)((ab * diffBrt + specB) * 255f));
                            } else {
                                float brt = max(0.0f, min(1.0f, (rowCross1*b0 + rowCross2*b1 + rowCross0*b2) * invTotal));
                                ri = (int)(((texColor >> 16) & 0xFF) * brt);
                                gi = (int)(((texColor >>  8) & 0xFF) * brt);
                                bi = (int)(( texColor        & 0xFF) * brt);
                            }
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
                            float brt = max(0.0f, min(1.0f, (rowCross1*b0 + rowCross2*b1 + rowCross0*b2) * invTotal));
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
