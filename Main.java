import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;


public class Main implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private static World myWorld;
    private static final int NUM_SHIPS = 50;
    private static Actor[]   shipGroup  = new Actor[NUM_SHIPS];
    private static double[]  shipAngle  = new double[NUM_SHIPS];
    private static double[]  shipRadius = new double[NUM_SHIPS];
    private static double[]  shipSpeed  = new double[NUM_SHIPS];
    private static double[]  shipTilt   = new double[NUM_SHIPS];
    private static final double[] WORLD_CENTER = {0, 0, 20};

    private static final int WIDTH  = 2000;
    private static final int HEIGHT = 2000;
    private static FrameBuffer fb;
    private static Canvas canvas;

    private int lastMouseX, lastMouseY;
    private static final double MOUSE_SENSITIVITY = 0.003;
    private static final double SCROLL_SPEED = 2.0;

    private static Actor cubeActor;

    // ── Mode switch ──────────────────────────────────────────────────────────
    private static final boolean SINGLE_SHIP_MODE = true; // true = single rotating ship, false = fleet orbit
    private static final int    VIEWER_TEXTURE    = 0;    // 0=Blue 1=Red 2=Green 3=Orange 4=Purple 5=Yellow
    private static double viewerDist = 15.0; // camera distance in single-ship mode

    private static long triAccum = 0;
    private static long lastPrintNanos = 0;
    private static long lastFrameNanos = 0;
    private static int framesOnTime = 0;
    private static int framesLate = 0;

    public static void main(String[] args) {
        myWorld = new World();

        fb = new FrameBuffer(WIDTH, HEIGHT);

        Texture[] textures = {
            Texture.loadFromFile("src/textures/StarSparrow_Blue.png"),
            Texture.loadFromFile("src/textures/StarSparrow_Red.png"),
            Texture.loadFromFile("src/textures/StarSparrow_Green.png"),
            Texture.loadFromFile("src/textures/StarSparrow_Orange.png"),
            Texture.loadFromFile("src/textures/StarSparrow_Purple.png"),
            Texture.loadFromFile("src/textures/StarSparrow_Yellow.png"),
        };
        Texture shipNormal    = Texture.loadFromFile("src/textures/StarSparrow_Normal.png");
        Texture shipMetallic  = Texture.loadFromFile("src/textures/StarSparrow_Metallic.png");
        Texture shipRoughness = Texture.loadFromFile("src/textures/StarSparrow_Roughness.png");

        Mesh shipMesh = new Mesh("src/models/ship2.csv");

        if (SINGLE_SHIP_MODE) {
            // Single ship viewer — sits at origin, mouse rotates it, scroll zooms
            Actor viewer = new Actor(shipMesh.deepCopy());
            viewer.texture      = textures[VIEWER_TEXTURE % textures.length];
            viewer.normalMap    = shipNormal;
            viewer.metallicMap  = shipMetallic;
            viewer.roughnessMap = shipRoughness;
            myWorld.actors.add(viewer);
            myWorld.cameraLocation = new double[]{0, 3, -viewerDist};
            myWorld.cameraTarget   = new double[]{0, 0, 0};
            myWorld.cameraRotation = new double[]{0, 1, 0};
        } else {
            myWorld.cameraLocation = new double[]{0, 30, -30};
            myWorld.cameraTarget   = new double[]{0,  0,  20};
            myWorld.cameraRotation = new double[]{0,  1,   0};
            for (int i = 0; i < NUM_SHIPS; i++) {
                shipGroup[i] = new Actor(shipMesh.deepCopy());
                shipGroup[i].scale[0] = 1;
                shipGroup[i].scale[1] = 1;
                shipGroup[i].scale[2] = 1;
                shipGroup[i].texture      = textures[i % textures.length];
                shipGroup[i].normalMap    = shipNormal;
                shipGroup[i].metallicMap  = shipMetallic;
                shipGroup[i].roughnessMap = shipRoughness;
                shipRadius[i] = 15 + Math.random() * 12;
                shipSpeed[i]  = (0.425 + Math.random() * 0.10) * (Math.random() < 0.5 ? 1 : -1);
                shipAngle[i]  = i * (360.0 / NUM_SHIPS) + Math.random() * 20;
                shipTilt[i]   = (Math.random() - 0.5) * 60;
                myWorld.actors.add(shipGroup[i]);
            }
        }

        cubeActor = new Actor(new Mesh("src/models/cube3.csv"));
        cubeActor.location[0] = WORLD_CENTER[0];
        cubeActor.location[1] = WORLD_CENTER[1];
        cubeActor.location[2] = WORLD_CENTER[2];
        cubeActor.scale[0] = 8; cubeActor.scale[1] = 8; cubeActor.scale[2] = 8;
        cubeActor.texture      = Texture.generateGiftWrap();
        cubeActor.metallicMap  = Texture.solidGrey(220);
        cubeActor.roughnessMap = Texture.solidGrey(35);
        // myWorld.actors.add(cubeActor);

        canvas = new Canvas();
        canvas.setBackground(Color.BLACK);

        Main handler = new Main();
        canvas.addKeyListener(handler);
        canvas.addMouseListener(handler);
        canvas.addMouseMotionListener(handler);
        canvas.addMouseWheelListener(handler);

        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = canvas.getWidth(), h = canvas.getHeight();
                if (h > 0) myWorld.viewAspect = (double) w / h;
            }
        });

        JFrame frame = new JFrame("3D Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas);
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        canvas.createBufferStrategy(2);
        canvas.requestFocusInWindow();
        myWorld.viewAspect = 1280.0 / 720.0;

        BufferStrategy bs = canvas.getBufferStrategy();
        Thread renderThread = new Thread(() -> {
            while (true) {
                renderFrame();
                do {
                    Graphics g = bs.getDrawGraphics();
                    g.drawImage(fb.image, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
                    g.dispose();
                } while (bs.contentsRestored());
                bs.show();
                Toolkit.getDefaultToolkit().sync();
            }
        });
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private static void renderFrame() {
        final long frameStart = System.nanoTime();
        final double deltaTime = lastFrameNanos == 0 ? 0 : (frameStart - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = frameStart;

        if (!SINGLE_SHIP_MODE) for (int i = 0; i < NUM_SHIPS; i++) {
            shipAngle[i] += shipSpeed[i] * deltaTime * 60;
            double a     = Math.toRadians(shipAngle[i]);
            double aPrev = a - Math.toRadians(shipSpeed[i]);
            double tlt   = Math.toRadians(shipTilt[i]);
            double r  = shipRadius[i] * (1.0 + 0.08 * Math.sin(a     * 2.1 + i * 1.3) + 0.04 * Math.sin(a     * 3.7 + i * 2.1));
            double rp = shipRadius[i] * (1.0 + 0.08 * Math.sin(aPrev * 2.1 + i * 1.3) + 0.04 * Math.sin(aPrev * 3.7 + i * 2.1));
            double yW  = shipRadius[i] * 0.10 * Math.sin(a     * 1.6 + i * 0.9);
            double yWp = shipRadius[i] * 0.10 * Math.sin(aPrev * 1.6 + i * 0.9);
            double px  = WORLD_CENTER[0] + r  * Math.cos(a);
            double py  = WORLD_CENTER[1] + r  * Math.sin(a) * Math.sin(tlt) + yW;
            double pz  = WORLD_CENTER[2] + r  * Math.sin(a) * Math.cos(tlt);
            double ppx = WORLD_CENTER[0] + rp * Math.cos(aPrev);
            double ppy = WORLD_CENTER[1] + rp * Math.sin(aPrev) * Math.sin(tlt) + yWp;
            double ppz = WORLD_CENTER[2] + rp * Math.sin(aPrev) * Math.cos(tlt);
            shipGroup[i].location[0] = px;
            shipGroup[i].location[1] = py;
            shipGroup[i].location[2] = pz;
            double vx = -(px - ppx), vy = -(py - ppy), vz = -(pz - ppz);
            double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz);
            if (vlen > 1e-10) { vx /= vlen; vy /= vlen; vz /= vlen; }
            double dX = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -vy))));
            double dY = Math.toDegrees(Math.atan2(vx, vz));
            shipGroup[i].rotation[0] = 90 + dX;
            shipGroup[i].rotation[1] = dY;
            shipGroup[i].rotation[2] = 0;
        }

        cubeActor.rotation[0] += 23.0 * deltaTime;
        cubeActor.rotation[1] += 37.0 * deltaTime;

        fb.clear();
        myWorld.Render(fb);

        long facesThisFrame = 0;
        for (Actor a : myWorld.actors) facesThisFrame += a.model.faces.size();
        triAccum += facesThisFrame;
        long now = System.nanoTime();
        long frameUs = (now - frameStart) / 1000;
        if (frameUs <= 16_666) framesOnTime++; else framesLate++;
        if (lastPrintNanos == 0) lastPrintNanos = now;
        if (now - lastPrintNanos >= 1_000_000_000L) {
            double elapsed = (now - lastPrintNanos) / 1e9;
            System.out.printf("Tri/sec: %,d  On-time: %d  Late: %d%n",
                (long)(triAccum / elapsed), framesOnTime, framesLate);
            triAccum = 0;
            framesOnTime = 0;
            framesLate = 0;
            lastPrintNanos = now;
        }
    }

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {
        int s = e.getKeyCode();
        double delta = 0.1;

        switch (s) {
            case 33: myWorld.cameraTarget[1] += 1.0; break;
            case 34: myWorld.cameraTarget[1] -= 1.0; break;
            case 38: myWorld.cameraLocation[2]+=(delta*10); break;
            case 40: myWorld.cameraLocation[2]-=(delta*10); break;
            case 37: myWorld.cameraLocation[0]-=delta; break;
            case 39: myWorld.cameraLocation[0]+=delta; break;
            case 49: myWorld.actors.get(0).rotation[0]+=5; break;
            case 50: myWorld.actors.get(0).rotation[0]-=5; break;
            case 51: myWorld.actors.get(0).rotation[1]+=5; break;
            case 52: myWorld.actors.get(0).rotation[1]-=5; break;
            case 53: myWorld.actors.get(0).rotation[2]+=5; break;
            case 54: myWorld.actors.get(0).rotation[2]-=5; break;
            case 55: myWorld.actors.get(0).location[2]+=0.5; break;
            case 56: myWorld.actors.get(0).location[2]-=0.5; break;

            case 'Q':
            case 'A': {
                double yaw = (s == 'Q') ? delta : -delta; yaw=yaw/5;
                double dx = myWorld.cameraTarget[0] - myWorld.cameraLocation[0];
                double dz = myWorld.cameraTarget[2] - myWorld.cameraLocation[2];
                double cy = Math.cos(yaw), sy = Math.sin(yaw);
                myWorld.cameraTarget[0] = myWorld.cameraLocation[0] + dx*cy - dz*sy;
                myWorld.cameraTarget[2] = myWorld.cameraLocation[2] + dx*sy + dz*cy;
                break;
            }
            case 'W':
            case 'S': {
                double pitch = (s == 'W') ? -delta : delta; pitch = pitch/5;
                double[] fwd = Utils.normalize(Utils.subtract(myWorld.cameraTarget, myWorld.cameraLocation));
                double[] right = Utils.normalize(Utils.cross(myWorld.cameraRotation, fwd));
                double[] up = Utils.cross(fwd, right);
                double dist = Math.sqrt(
                    Math.pow(myWorld.cameraTarget[0]-myWorld.cameraLocation[0],2) +
                    Math.pow(myWorld.cameraTarget[1]-myWorld.cameraLocation[1],2) +
                    Math.pow(myWorld.cameraTarget[2]-myWorld.cameraLocation[2],2));
                double cp = Math.cos(pitch), sp = Math.sin(pitch);
                double[] newFwd = {
                    fwd[0]*cp + up[0]*sp,
                    fwd[1]*cp + up[1]*sp,
                    fwd[2]*cp + up[2]*sp
                };
                myWorld.cameraTarget[0] = myWorld.cameraLocation[0] + newFwd[0]*dist;
                myWorld.cameraTarget[1] = myWorld.cameraLocation[1] + newFwd[1]*dist;
                myWorld.cameraTarget[2] = myWorld.cameraLocation[2] + newFwd[2]*dist;
                break;
            }
            case 'E':
            case 'D': {
                double roll = (s == 'E') ? delta : -delta; roll=roll/5;
                double[] fwd = Utils.normalize(Utils.subtract(myWorld.cameraTarget, myWorld.cameraLocation));
                double[] up = myWorld.cameraRotation;
                double[] right = Utils.normalize(Utils.cross(up, fwd));
                double cr = Math.cos(roll), sr = Math.sin(roll);
                myWorld.cameraRotation = new double[] {
                    up[0]*cr + right[0]*sr,
                    up[1]*cr + right[1]*sr,
                    up[2]*cr + right[2]*sr
                };
                break;
            }
            case 'R': myWorld.cameraTarget[0]+=(delta); break;
            case 'F': myWorld.cameraTarget[0]-=(delta); break;
            case 'T': myWorld.cameraTarget[1]+=(delta); break;
            case 'G': myWorld.cameraTarget[1]-=(delta); break;
            case 'Y': myWorld.cameraTarget[2]+=(delta); break;
            case 'H': myWorld.cameraTarget[2]-=(delta); break;

            case 'P':
                myWorld.cameraLocation[0]=0;
                myWorld.cameraLocation[1]=0;
                myWorld.cameraLocation[2]=0;
                myWorld.cameraRotation[0]=0;
                myWorld.cameraRotation[1]=1;
                myWorld.cameraRotation[2]=0;
                myWorld.cameraTarget[0]=0;
                myWorld.cameraTarget[1]=0;
                myWorld.cameraTarget[2]=100;
                break;

            case 'M': {
                Actor spawned = new Actor(shipGroup[0].model);
                spawned.location[0] = myWorld.cameraLocation[0];
                spawned.location[1] = myWorld.cameraLocation[1];
                spawned.location[2] = myWorld.cameraLocation[2];
                myWorld.actors.add(spawned);
                break;
            }
        }

        System.out.println(
            String.format("Loc (%.1f,%.1f,%.1f) Rot (%.1f, %.1f, %.1f), Target (%.1f, %.1f, %.1f), ActorRot (%.1f, %.1f, %.1f)",
            Double.valueOf(myWorld.cameraLocation[0]),
            Double.valueOf(myWorld.cameraLocation[1]),
            Double.valueOf(myWorld.cameraLocation[2]),
            Double.valueOf(myWorld.cameraRotation[0]),
            Double.valueOf(myWorld.cameraRotation[1]),
            Double.valueOf(myWorld.cameraRotation[2]),
            Double.valueOf(myWorld.cameraTarget[0]),
            Double.valueOf(myWorld.cameraTarget[1]),
            Double.valueOf(myWorld.cameraTarget[2]),
            Double.valueOf(myWorld.actors.get(0).rotation[2]),
            Double.valueOf(myWorld.actors.get(0).rotation[1]),
            Double.valueOf(myWorld.actors.get(0).rotation[0])
            ));
    }

    public void keyReleased(KeyEvent e) {}

    public void mousePressed(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    public void mouseDragged(MouseEvent e) {
        int dx = e.getX() - lastMouseX;
        int dy = e.getY() - lastMouseY;
        lastMouseX = e.getX();
        lastMouseY = e.getY();

        if (SINGLE_SHIP_MODE) {
            Actor ship = myWorld.actors.get(0);
            ship.rotation[1] += dx * MOUSE_SENSITIVITY * (180.0 / Math.PI);
            ship.rotation[0] -= dy * MOUSE_SENSITIVITY * (180.0 / Math.PI);
            return;
        }

        double yaw = -dx * MOUSE_SENSITIVITY;
        double tdx = myWorld.cameraTarget[0] - myWorld.cameraLocation[0];
        double tdz = myWorld.cameraTarget[2] - myWorld.cameraLocation[2];
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        myWorld.cameraTarget[0] = myWorld.cameraLocation[0] + tdx*cy - tdz*sy;
        myWorld.cameraTarget[2] = myWorld.cameraLocation[2] + tdx*sy + tdz*cy;

        double pitch = dy * MOUSE_SENSITIVITY;
        double[] fwd   = Utils.normalize(Utils.subtract(myWorld.cameraTarget, myWorld.cameraLocation));
        double[] right = Utils.normalize(Utils.cross(myWorld.cameraRotation, fwd));
        double[] up    = Utils.cross(fwd, right);
        double dist = Math.sqrt(
            Math.pow(myWorld.cameraTarget[0] - myWorld.cameraLocation[0], 2) +
            Math.pow(myWorld.cameraTarget[1] - myWorld.cameraLocation[1], 2) +
            Math.pow(myWorld.cameraTarget[2] - myWorld.cameraLocation[2], 2));
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double[] newFwd = {
            fwd[0]*cp + up[0]*sp,
            fwd[1]*cp + up[1]*sp,
            fwd[2]*cp + up[2]*sp
        };
        myWorld.cameraTarget[0] = myWorld.cameraLocation[0] + newFwd[0]*dist;
        myWorld.cameraTarget[1] = myWorld.cameraLocation[1] + newFwd[1]*dist;
        myWorld.cameraTarget[2] = myWorld.cameraLocation[2] + newFwd[2]*dist;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        double scroll = e.getWheelRotation() * SCROLL_SPEED;
        if (SINGLE_SHIP_MODE) {
            viewerDist = Math.max(3.0, viewerDist + scroll);
            double[] fwd = Utils.normalize(Utils.subtract(myWorld.cameraTarget, myWorld.cameraLocation));
            myWorld.cameraLocation[0] = myWorld.cameraTarget[0] - fwd[0] * viewerDist;
            myWorld.cameraLocation[1] = myWorld.cameraTarget[1] - fwd[1] * viewerDist;
            myWorld.cameraLocation[2] = myWorld.cameraTarget[2] - fwd[2] * viewerDist;
            return;
        }
        double[] fwd = Utils.normalize(Utils.subtract(myWorld.cameraTarget, myWorld.cameraLocation));
        myWorld.cameraLocation[0] += fwd[0] * scroll;
        myWorld.cameraLocation[1] += fwd[1] * scroll;
        myWorld.cameraLocation[2] += fwd[2] * scroll;
        myWorld.cameraTarget[0]   += fwd[0] * scroll;
        myWorld.cameraTarget[1]   += fwd[1] * scroll;
        myWorld.cameraTarget[2]   += fwd[2] * scroll;
    }

    public void mouseReleased(MouseEvent e) {}
    public void mouseClicked(MouseEvent e)  {}
    public void mouseEntered(MouseEvent e)  {}
    public void mouseExited(MouseEvent e)   {}
    public void mouseMoved(MouseEvent e)    {}
}
