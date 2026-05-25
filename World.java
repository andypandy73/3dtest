import java.util.ArrayList;
import java.util.List;


public class World {
    private static final double[] LIGHT_DIR = Utils.normalize(new double[]{0.4, 0.7, 1.0});

    public List<Actor> actors;
    public double[] cameraLocation;
    public double[] cameraRotation;
    public double[] cameraTarget;
    public double viewAspect = 1.0;

    public World() {
        actors = new ArrayList<>();
        cameraLocation = new double[] { 0, 0, -100 };
        cameraTarget   = new double[] { 0, 0, 200 };
        cameraRotation = new double[] { 0.0, 1.0, 0.0 };
    }

    public void Render(FrameBuffer fb) {
        double[][] projection = Utils.createMatrix_Projection(50.0, viewAspect, 0.5, 200.0);
        double[][] camera     = Utils.createMatrix_Lookat(cameraLocation, cameraTarget, cameraRotation);

        // Sort front-to-back so closer actors fill the z-buffer first
        double ex = cameraLocation[0], ey = cameraLocation[1], ez = cameraLocation[2];
        actors.sort((a, b) -> {
            double da = (a.location[0]-ex)*(a.location[0]-ex) + (a.location[1]-ey)*(a.location[1]-ey) + (a.location[2]-ez)*(a.location[2]-ez);
            double db = (b.location[0]-ex)*(b.location[0]-ex) + (b.location[1]-ey)*(b.location[1]-ey) + (b.location[2]-ez)*(b.location[2]-ez);
            return Double.compare(da, db);
        });

        actors.parallelStream().forEach(a -> {
            double[][] scale    = new double[4][4];
            double[][] rotation = new double[4][4];
            double[][] trans    = new double[4][4];
            double[][] tmp1     = new double[4][4];
            double[][] tmp2     = new double[4][4];
            double[][] matMV    = new double[4][4];

            Utils.createMatrix_Scaler(scale, a.scale[0], a.scale[1], a.scale[2]);
            Utils.createMatrix_CombinedRotator(rotation, a.rotation[0], a.rotation[1], a.rotation[2]);
            Utils.createMatrix_Translator(trans, a.location[0], a.location[1], a.location[2]);
            Utils.multiplyMatrices(tmp1,  scale,    rotation);
            Utils.multiplyMatrices(tmp2,  tmp1,     trans);
            Utils.multiplyMatrices(matMV, tmp2,     camera);

            Mesh m = a.model;
            m.applyModelView(matMV, LIGHT_DIR);
            m.applyProjection(projection);
            double[] ld = LIGHT_DIR;
            double ltx = camera[0][0]*ld[0] + camera[1][0]*ld[1] + camera[2][0]*ld[2];
            double lty = camera[0][1]*ld[0] + camera[1][1]*ld[1] + camera[2][1]*ld[2];
            double ltz = camera[0][2]*ld[0] + camera[1][2]*ld[1] + camera[2][2]*ld[2];
            m.Render(fb, a.texture, (float)ltx, (float)lty, (float)ltz);
        });
    }
}
