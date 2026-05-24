public class Vertex {
    double x, y, z;
    double x_model, y_model, z_model;

    double u, v;
    double u_model, v_model;

    // Clip-space w before perspective divide, needed for perspective-correct UV
    double wClip = 1.0;

    Vertex() {}

    Vertex(double x, double y, double z) {
        this.x = x_model = x;
        this.y = y_model = y;
        this.z = z_model = z;
    }

    void applyModelView(double[][] m) {
        double ox = m[0][0]*x_model + m[1][0]*y_model + m[2][0]*z_model + m[3][0];
        double oy = m[0][1]*x_model + m[1][1]*y_model + m[2][1]*z_model + m[3][1];
        double oz = m[0][2]*x_model + m[1][2]*y_model + m[2][2]*z_model + m[3][2];
        double ow = m[0][3]*x_model + m[1][3]*y_model + m[2][3]*z_model + m[3][3];
        x = ox / ow; y = oy / ow; z = oz / ow;
    }

    void applyProjection(double[][] m) {
        double ox = m[0][0]*x + m[1][0]*y + m[2][0]*z + m[3][0];
        double oy = m[0][1]*x + m[1][1]*y + m[2][1]*z + m[3][1];
        double oz = m[0][2]*x + m[1][2]*y + m[2][2]*z + m[3][2];
        wClip      = m[0][3]*x + m[1][3]*y + m[2][3]*z + m[3][3];
        x = ox / wClip; y = oy / wClip; z = oz / wClip;
    }

    double[] asArray() {
        return new double[]{x, y, z};
    }
}
