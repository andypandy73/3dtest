import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Texture {
    final int width, height;
    final int[] pixels; // packed as (r<<16|g<<8|b), each component 0-255

    Texture(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    // Nearest-neighbour sample; u,v wrap in [0,1]
    int sample(double u, double v) {
        u -= Math.floor(u);
        v -= Math.floor(v);
        int px = Math.min((int)(u * width),  width  - 1);
        int py = Math.min((int)(v * height), height - 1);
        return pixels[py * width + px];
    }

    static Texture loadFromFile(String path) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            int W = img.getWidth(), H = img.getHeight();
            Texture tex = new Texture(W, H);
            for (int py = 0; py < H; py++) {
                for (int px = 0; px < W; px++) {
                    int argb = img.getRGB(px, py);
                    tex.pixels[py * W + px] = argb & 0x00FFFFFF; // strip alpha, keep r<<16|g<<8|b
                }
            }
            return tex;
        } catch (IOException e) {
            System.err.println("Failed to load texture: " + path);
            return null;
        }
    }

    static Texture generateFur() {
        int W = 512, H = 512;
        Texture tex = new Texture(W, H);

        for (int py = 0; py < H; py++) {
            for (int px = 0; px < W; px++) {
                double u = (double)px / W;
                double v = (double)py / H;

                // Low-frequency waviness shifts strand positions horizontally
                double wave = valueNoise(u * 3,  v * 8,  0) * 0.025
                            + valueNoise(u * 8,  v * 20, 1) * 0.012;

                // Which strand, and distance to its centre
                double fiberPos = (u + wave) * 100.0;
                double dist = Math.abs(fiberPos - Math.round(fiberPos)); // 0 = centre, 0.5 = between

                // Strand width tapers toward the tip (v=1)
                double strandW = 0.20 - v * 0.10;
                double onStrand = Math.max(0.0, 1.0 - dist / strandW);
                onStrand *= onStrand; // quadratic falloff for soft edges

                // Fine detail noise
                double detail = valueNoise(u * 40, v * 80, 2) * 0.10;

                // Brightness: lighter on-strand, darker between, tips darker
                double bright = 0.42 + onStrand * 0.38 + detail;
                bright *= (1.0 - v * 0.30); // tip darkening

                // Warm orange-brown monkey palette
                double r = Math.min(1.0, Math.max(0.0, bright * 0.85));
                double g = Math.min(1.0, Math.max(0.0, bright * 0.55));
                double b = Math.min(1.0, Math.max(0.0, bright * 0.22));

                tex.pixels[py * W + px] = ((int)(r*255) << 16) | ((int)(g*255) << 8) | (int)(b*255);
            }
        }
        return tex;
    }

    static Texture generateTron() {
        int W = 512, H = 512;
        Texture tex = new Texture(W, H);

        int    gridCount = 220;  // crosshatch lines per UV tile
        double lineHalf  = 0.28; // half-width of each line as a fraction of one cell

        // Dark blue background (visible but not distracting)
        double bgR = 0.00, bgG = 0.02, bgB = 0.18;

        for (int py = 0; py < H; py++) {
            for (int px = 0; px < W; px++) {
                double u = (double)px / W;
                double v = (double)py / H;

                // Position within each grid cell, [0,1]
                double cu = (u * gridCount) % 1.0;
                double cv = (v * gridCount) % 1.0;

                // Distance to nearest horizontal or vertical grid line (0 = on the line)
                double du   = Math.min(cu, 1.0 - cu);
                double dv   = Math.min(cv, 1.0 - cv);
                double dist = Math.min(du, dv);

                // Sharp bright core
                double core = Math.max(0.0, 1.0 - dist / lineHalf);
                core *= core;

                // Wider, dimmer bloom halo for the neon glow look
                double bloom = Math.max(0.0, 1.0 - dist / (lineHalf * 4.5));
                bloom *= bloom * 0.28;

                double intensity = Math.min(1.0, core + bloom);

                // Blend neon line over dark blue background
                double r = bgR;
                double g = lerp(bgG, 0.72, intensity);
                double b = lerp(bgB, 1.00, intensity);

                tex.pixels[py * W + px] = ((int)(r*255) << 16) | ((int)(g*255) << 8) | (int)(b*255);
            }
        }
        return tex;
    }

    static Texture generateGiftWrap() {
        int W = 512, H = 512;
        Texture tex = new Texture(W, H);

        // Five festive stripe colours cycling diagonally
        int[][] palette = {
            {210, 20,  40},  // red
            { 20, 155, 55},  // green
            { 20,  55, 210}, // blue
            {210, 155, 15},  // gold
            {160,  20, 185}, // purple
        };

        for (int py = 0; py < H; py++) {
            for (int px = 0; px < W; px++) {
                double u = (double)px / W;
                double v = (double)py / H;

                // Diagonal stripes — cycle through palette
                double d = ((u + v) * palette.length) % palette.length;
                if (d < 0) d += palette.length;
                int[] col = palette[(int)d];
                double r = col[0] / 255.0;
                double g = col[1] / 255.0;
                double b = col[2] / 255.0;

                // Gold ribbon bands: evenly spaced horizontal and vertical lines
                double rh = Math.abs(((v * 4 + 0.5) % 1.0) - 0.5) * 2; // 0 at ribbon centre
                double rv = Math.abs(((u * 4 + 0.5) % 1.0) - 0.5) * 2;
                double onRibbon = Math.min(1.0, Math.max(0.0, 1.0 - Math.min(rh, rv) * 10));
                r = lerp(r, 1.00, onRibbon);
                g = lerp(g, 0.82, onRibbon);
                b = lerp(b, 0.10, onRibbon);

                // Small white polka dots on non-ribbon areas
                if (onRibbon < 0.1) {
                    double su = (u * 10 + 0.5) % 1.0;
                    double sv = (v * 10 + 0.5) % 1.0;
                    double dotDist = Math.sqrt((su - 0.5)*(su - 0.5) + (sv - 0.5)*(sv - 0.5));
                    double dot = Math.max(0.0, 1.0 - dotDist / 0.14);
                    dot *= dot;
                    r = Math.min(1.0, r + dot * 0.6);
                    g = Math.min(1.0, g + dot * 0.6);
                    b = Math.min(1.0, b + dot * 0.6);
                }

                tex.pixels[py * W + px] = ((int)(r*255) << 16) | ((int)(g*255) << 8) | (int)(b*255);
            }
        }
        return tex;
    }

    // Elevation-mapped terrain texture: v=0 is deep water, v≈0.28 is shoreline, v=1 is hilltop.
    // u tiles laterally for surface detail; v is clamped [0,1] so no wrapping issue.
    static Texture generateTerrain() {
        int W = 512, H = 512;
        Texture tex = new Texture(W, H);
        for (int py = 0; py < H; py++) {
            for (int px = 0; px < W; px++) {
                double u = (double)px / W;
                double v = (double)py / H; // 0=deep valley, 1=hill peak

                double waterLine = 0.28;
                double shoreEnd  = 0.36;
                double r, g, b;

                if (v < waterLine) {
                    double t = v / waterLine; // 0=deep, 1=shore
                    double glint = valueNoise(u * 8, t * 5, 50) * 0.07
                                 + valueNoise(u * 20, t * 14, 51) * 0.03;
                    r = lerp(0.02, 0.20, t) + glint * 0.04;
                    g = lerp(0.10, 0.45, t) + glint * 0.09;
                    b = lerp(0.50, 0.72, t) + glint * 0.14;
                } else if (v < shoreEnd) {
                    double t = (v - waterLine) / (shoreEnd - waterLine);
                    r = lerp(0.74, 0.38, t);
                    g = lerp(0.64, 0.54, t);
                    b = lerp(0.34, 0.17, t);
                } else {
                    double macro = valueNoise(u * 3, v * 6, 52);
                    double med   = valueNoise(u * 11, v * 18, 53) * 0.55
                                 + valueNoise(u * 22, v * 35, 54) * 0.28;
                    double fine  = valueNoise(u * 50, v * 75, 55) * 0.12;
                    double bright = 0.22 + (macro * 0.3 + med + fine) * 0.32;
                    r = bright * 0.29;
                    g = bright * 0.87;
                    b = bright * 0.13;
                }

                tex.pixels[py * W + px] = ((int)(Math.min(1,Math.max(0,r))*255) << 16)
                                        | ((int)(Math.min(1,Math.max(0,g))*255) << 8)
                                        |  (int)(Math.min(1,Math.max(0,b))*255);
            }
        }
        return tex;
    }

    static Texture generateMetallic() {
        int W = 512, H = 512;
        Texture tex = new Texture(W, H);

        int   panelsU = 6, panelsV = 8;   // grid divisions
        double seam   = 0.04;             // seam width as fraction of a cell
        double r, g, b;

        for (int py = 0; py < H; py++) {
            for (int px = 0; px < W; px++) {
                double u = (double)px / W;
                double v = (double)py / H;

                // Panel cell coordinates
                double cu = (u * panelsU) % 1.0;
                double cv = (v * panelsV) % 1.0;
                int    pu = (int)(u * panelsU);
                int    pv = (int)(v * panelsV);

                // Seam: proximity to panel edge
                double edgeU = Math.min(cu, 1.0 - cu);
                double edgeV = Math.min(cv, 1.0 - cv);
                boolean onSeam = edgeU < seam || edgeV < seam;

                // Metallic base — slight per-panel brightness variation + scratches
                double panelVar  = valueNoise(pu * 1.7, pv * 1.3, 70) * 0.12;
                double scratch   = valueNoise(u * 350, v * 2, 71) * 0.06
                                 + valueNoise(u * 600, v * 1, 72) * 0.03;
                double grain     = valueNoise(u * 80, v * 80, 73) * 0.03;
                double baseBright = 0.52 + panelVar + scratch + grain;

                // Default: steel grey
                r = baseBright * 0.78;
                g = baseBright * 0.80;
                b = baseBright * 0.82;

                // Yellow hazard stripes on select panels (every 3rd column, alternating rows)
                boolean stripePanel = (pu % 3 == 1) && (pv % 2 == 0);
                if (stripePanel && !onSeam) {
                    double diag = ((cu + cv) * 5.0) % 1.0;
                    boolean yellowStripe = diag < 0.45;
                    if (yellowStripe) {
                        r = 0.88 + scratch * 0.3;
                        g = 0.72 + scratch * 0.2;
                        b = 0.02;
                    } else {
                        // Dark stripe between yellow bands
                        r = 0.10 + scratch * 0.2;
                        g = 0.10 + scratch * 0.2;
                        b = 0.10 + scratch * 0.2;
                    }
                }

                // Red indicator panels (every 4th column, specific rows)
                boolean redPanel = (pu % 4 == 3) && (pv % 3 == 1);
                if (redPanel && !onSeam) {
                    // Small red rectangle in centre of panel
                    boolean inRect = cu > 0.2 && cu < 0.8 && cv > 0.25 && cv < 0.75;
                    if (inRect) {
                        r = 0.85 + scratch * 0.2;
                        g = 0.05;
                        b = 0.05;
                    }
                }

                // Seam lines: dark recessed groove
                if (onSeam) {
                    double s = baseBright * 0.30;
                    r = s; g = s; b = s;
                }

                tex.pixels[py * W + px] = ((int)(Math.min(1,Math.max(0,r))*255) << 16)
                                        | ((int)(Math.min(1,Math.max(0,g))*255) << 8)
                                        |  (int)(Math.min(1,Math.max(0,b))*255);
            }
        }
        return tex;
    }

    static Texture generateSpaceship() {
        int W = 2048, H = 2048;
        Texture tex = new Texture(W, H);

        for (int py = 0; py < H; py++) {
            for (int px = 0; px < W; px++) {
                double u = (double)px / W;
                double v = (double)py / H;

                // --- Panel grid for seam lines ---
                double panelU = (u * 14) % 1.0, panelV = (v * 20) % 1.0;
                double seamU  = Math.min(panelU, 1 - panelU);
                double seamV  = Math.min(panelV, 1 - panelV);
                boolean seam  = seamU < 0.03 || seamV < 0.03;

                double scratch = valueNoise(u * 400, v * 3, 80) * 0.05
                               + valueNoise(u * 700, v * 2, 81) * 0.025;
                double panelVar = valueNoise(u * 14, v * 20, 82) * 0.08;
                double base = 0.55 + panelVar + scratch;

                // --- Base hull: solid red ---
                double r = base * 0.95, g = base * 0.05, b = 0.02;

                // --- Cockpit (nose v=0.08-0.26, u=0.33-0.67): bright yellow glow ---
                double du = Math.abs(u - 0.5) / 0.17;
                double dv = Math.abs(v - 0.17) / 0.09;
                double cockpitDist = Math.sqrt(du*du + dv*dv);
                if (cockpitDist < 1.0 && v > 0.08 && v < 0.26) {
                    double glow = Math.pow(Math.max(0, 1.0 - cockpitDist), 1.5);
                    r = lerp(r, 1.00, glow * 0.95);
                    g = lerp(g, 0.85, glow * 0.95);
                    b = lerp(b, 0.02, glow * 0.95);
                }

                // --- Wing markings: yellow/red diagonal stripes at u≈0.25 and u≈0.75, v=0.38-0.56 ---
                double wingL = Math.abs(u - 0.25) / 0.11;
                double wingR = Math.abs(u - 0.75) / 0.11;
                boolean onWing = (wingL < 1.0 || wingR < 1.0) && v > 0.38 && v < 0.56;
                if (onWing && !seam) {
                    double diag = ((u + v) * 8.0) % 1.0;
                    if (diag < 0.5) {
                        r = 1.00; g = 0.82; b = 0.02; // yellow stripe
                    } else {
                        r = 0.75; g = 0.04; b = 0.02; // red stripe
                    }
                }

                // --- Yellow accent stripe along body sides (u≈0.12 and u≈0.88, v=0.2-0.72) ---
                boolean accentL = u > 0.09 && u < 0.15 && v > 0.20 && v < 0.72;
                boolean accentR = u > 0.85 && u < 0.91 && v > 0.20 && v < 0.72;
                if ((accentL || accentR) && !seam) {
                    r = 1.00; g = 0.78; b = 0.02;
                }

                // --- Bright blue centre body panels (u=0.35-0.65, v=0.30-0.55) ---
                boolean blueCentre = u > 0.35 && u < 0.65 && v > 0.30 && v < 0.55;
                if (blueCentre && !seam) {
                    double bVar = valueNoise(u * 30, v * 40, 90) * 0.10;
                    r = 0.04;
                    g = 0.30 + bVar;
                    b = 0.95 + bVar;
                }

                // --- Bright blue vent strips at wing roots (v=0.57-0.66) ---
                boolean blueVentL = u > 0.20 && u < 0.38 && v > 0.57 && v < 0.66;
                boolean blueVentR = u > 0.62 && u < 0.80 && v > 0.57 && v < 0.66;
                if ((blueVentL || blueVentR) && !seam) {
                    double bVar = valueNoise(u * 60, v * 20, 91) * 0.08;
                    r = 0.03;
                    g = 0.25 + bVar;
                    b = 1.00;
                }

                // --- Main thruster: bright yellow-white core fading to red (v=0.75-1.0) ---
                double tc = Math.abs(u - 0.5) / 0.12;
                double tv = (v - 0.78) / 0.18;
                double thrusterDist = Math.sqrt(tc*tc + tv*tv);
                if (v > 0.75 && thrusterDist < 1.0) {
                    double heat = Math.pow(Math.max(0, 1.0 - thrusterDist), 1.3);
                    r = lerp(r, 1.00, heat * 0.95);
                    g = lerp(g, 0.90, heat * 0.95);
                    b = lerp(b, 0.10, heat * 0.95);
                }

                // --- Secondary wing thrusters: yellow glow ---
                boolean thrL = u > 0.18 && u < 0.30 && v > 0.68 && v < 0.77;
                boolean thrR = u > 0.70 && u < 0.82 && v > 0.68 && v < 0.77;
                if (thrL || thrR) {
                    double cx = thrL ? 0.24 : 0.76;
                    double dist2 = Math.sqrt(Math.pow((u-cx)/0.06,2) + Math.pow((v-0.725)/0.04,2));
                    double heat2 = Math.max(0, 1.0 - dist2);
                    r = lerp(r, 1.00, heat2 * 0.90);
                    g = lerp(g, 0.80, heat2 * 0.90);
                    b = lerp(b, 0.05, heat2 * 0.90);
                }

                // --- Seam lines: medium grey grooves ---
                if (seam) {
                    r = 0.45; g = 0.43; b = 0.43;
                }

                tex.pixels[py * W + px] = ((int)(Math.min(1,Math.max(0,r))*255) << 16)
                                        | ((int)(Math.min(1,Math.max(0,g))*255) << 8)
                                        |  (int)(Math.min(1,Math.max(0,b))*255);
            }
        }
        return tex;
    }

    static Texture generateGrass() {
        int W = 512, H = 512;
        Texture tex = new Texture(W, H);
        for (int py = 0; py < H; py++) {
            for (int px = 0; px < W; px++) {
                double u = (double)px / W;
                double v = (double)py / H;

                double macro  = valueNoise(u * 3,  v * 3,  20);
                double medium = valueNoise(u * 11, v * 11, 21) * 0.6
                              + valueNoise(u * 23, v * 23, 22) * 0.3;
                double fine   = valueNoise(u * 55, v * 55, 23) * 0.15;

                // Vertical blade streaks
                double bladeCol   = (u * 64) % 1.0;
                double bladePhase = valueNoise(Math.floor(u * 64), 0, 24);
                double bladeDist  = Math.abs(bladeCol - 0.5);
                double blade = Math.max(0.0, 1.0 - bladeDist / (0.10 + bladePhase * 0.10));
                blade *= blade;
                double bladeRow = valueNoise(Math.floor(u * 64), v * 3, 25);
                blade *= (bladeRow > 0.45) ? (bladeRow - 0.45) / 0.55 : 0.0;

                double bright = 0.22 + (macro * 0.3 + medium + fine) * 0.32;

                double r = bright * 0.28 + blade * 0.08;
                double g = bright * 0.88 + blade * 0.20;
                double b = bright * 0.14;

                // Dry yellow patches
                double dry = Math.max(0.0, valueNoise(u * 4, v * 4, 26) - 0.72) * 3.5;
                r = lerp(r, 0.50, dry * 0.35);
                g = lerp(g, 0.48, dry * 0.20);
                b = lerp(b, 0.08, dry * 0.25);

                // Dark dirt spots
                double dirt = Math.max(0.0, valueNoise(u * 18, v * 18, 27) - 0.80) * 4.0;
                r = lerp(r, 0.25, dirt * 0.4);
                g = lerp(g, 0.18, dirt * 0.4);
                b = lerp(b, 0.08, dirt * 0.4);

                tex.pixels[py * W + px] = ((int)(Math.min(1,Math.max(0,r))*255) << 16)
                                        | ((int)(Math.min(1,Math.max(0,g))*255) << 8)
                                        |  (int)(Math.min(1,Math.max(0,b))*255);
            }
        }
        return tex;
    }

    // Smooth (Hermite) value noise
    private static double valueNoise(double x, double y, int seed) {
        int ix = (int)Math.floor(x), iy = (int)Math.floor(y);
        double fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2*fx); // smoothstep
        fy = fy * fy * (3 - 2*fy);
        double v00 = hash(ix,   iy,   seed), v10 = hash(ix+1, iy,   seed);
        double v01 = hash(ix,   iy+1, seed), v11 = hash(ix+1, iy+1, seed);
        return lerp(lerp(v00, v10, fx), lerp(v01, v11, fx), fy);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static double hash(int x, int y, int seed) {
        int h = seed * 1013 + x * 374761393 + y * 1103515245;
        h = ((h >> 13) ^ h) * 1664525 + 1013904223;
        return ((h & 0x7FFFFFFF) % 10000) / 10000.0;
    }
}
