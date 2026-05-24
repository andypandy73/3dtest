public class Utils {



    static double[][] multiplyMatrices(double[][] a, double[][] b) {
        double[][] result = new double[4][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = a[i][0] * b[0][j] +
                        a[i][1] * b[1][j] +
                        a[i][2] * b[2][j] +
                        a[i][3] * b[3][j];
            }
        }
        return result;
    }

    static double[][] createMatrix_CombinedRotator(double angleX, double angleY, double angleZ) {
        // Java automatically initializes all elements to 0.0
        double[][] m = new double[4][4];

        double radX = Math.toRadians(angleX);
        double radY = Math.toRadians(angleY);
        double radZ = Math.toRadians(angleZ);

        double cx = Math.cos(radX);
        double sx = Math.sin(radX);
        double cy = Math.cos(radY);
        double sy = Math.sin(radY);
        double cz = Math.cos(radZ);
        double sz = Math.sin(radZ);

        // Row 0
        m[0][0] = cy * cz;
        m[0][1] = cy * sz;
        m[0][2] = -sy;

        // Row 1
        m[1][0] = sx * sy * cz - cx * sz;
        m[1][1] = sx * sy * sz + cx * cz;
        m[1][2] = sx * cy;

        // Row 2
        m[2][0] = cx * sy * cz + sx * sz;
        m[2][1] = cx * sy * sz - sx * cz;
        m[2][2] = cx * cy;

        // Row 3 (Homogeneous coordinate)
        m[3][3] = 1.0;

        return m;
    }


    static double[][] createMatrix_YaxisRotator (double angle) {

        double m[][] = new double[4][4];

        double cosAngle = Math.cos(angle * Math.PI / 180.0);
        double sinAngle = Math.sin(angle * Math.PI / 180.0);

        m[0][0] = cosAngle;
        m[0][1] = 0.0;
        m[0][2] = -sinAngle;
        m[0][3] = 0;
        
        m[1][0] = 0.0;
        m[1][1] = 1.0;
        m[1][2] = 0.0;
        m[1][3] = 0.0;

        m[2][0] = sinAngle;
        m[2][1] = 0.0;
        m[2][2] = cosAngle;
        m[2][3] = 0.0;;

        m[3][0] = 0.0;
        m[3][1] = 0.0;
        m[3][2] = 0.0;
        m[3][3] = 1.0;

        return m;
    }
    
    static double[][] createMatrix_XaxisRotator(double angle) {

        double m[][] = new double[4][4];

        double cosAngle = Math.cos(angle * Math.PI / 180.0);
        double sinAngle = Math.sin(angle * Math.PI / 180.0);

        m[0][0] = 1.0;
        m[0][1] = 0.0;
        m[0][2] = 0.0;
        m[0][3] = 0.0;
        
        m[1][0] = 0.0;
        m[1][1] = cosAngle;
        m[1][2] = sinAngle;
        m[1][3] = 0.0;

        m[2][0] = 0.0;
        m[2][1] = -sinAngle;
        m[2][2] = cosAngle;
        m[2][3] = 0.0;

        m[3][0] = 0.0;
        m[3][1] = 0.0;
        m[3][2] = 0.0;
        m[3][3] = 1.0;

        return m;
    }
    
    static double[][] createMatrix_ZaxisRotator(double angle) {

        double m[][] = new double[4][4];

        double cosAngle = Math.cos(angle * Math.PI / 180.0);
        double sinAngle = Math.sin(angle * Math.PI / 180.0);

        m[0][0]  = cosAngle;
        m[0][1] = -sinAngle;
        m[0][2] = 0.0;
        m[0][3] = 0.0;
        
        m[1][0] = sinAngle;
        m[1][1] = cosAngle;
        m[1][2] = 0;
        m[1][3] = 0;

        m[2][0] = 0.0;
        m[2][1] = 0.0;
        m[2][2] = 1.0;
        m[2][3] = 0.0;

        m[3][0] = 0.0;
        m[3][1] = 0.0;
        m[3][2] = 0.0;
        m[3][3] = 1.0;

        return m;
    }
  


    public static double[][] createMatrix_Scaler(double sx, double sy, double sz) {
        double[][] m = new double[4][4];
        m[0][0] = sx;
        m[1][1] = sy;
        m[2][2] = sz;
        m[3][3] = 1.0;
        return m;
    }

    public static double[][] createMatrix_Translator(double tx, double ty, double tz)
    {   
        double m[][] = new double[4][4];

        m[0][0] = 1.0;
        m[1][0] = 0.0;
        m[2][0] = 0.0;
        m[3][0] = tx;
        
        m[0][1] = 0.0;
        m[1][1] = 1.0;
        m[2][1] = 0.0;
        m[3][1] = ty;

        m[0][2] = 0.0;
        m[1][2] = 0.0;
        m[2][2] = 1.0;
        m[3][2] = tz;

        m[0][3] = 0.0;
        m[1][3] = 0.0;
        m[2][3] = 0.0;
        m[3][3] = 1.0;

        return m;
    }

    public static double[][] createMatrix_Projection(double fov,  double aspect, double n, double f)
    {

        
        //double yScale = (1.0 / Math.tan(Math.toRadians(fov / 2.0)));
        double yScale = 1.0  /  (Math.tan((float) fov / 2.0 * Math.PI / 180.0));
        double xScale = yScale / aspect;
        double frustumLength = f - n;

        double[][] matrix = new double[4][4];


        matrix[0][0] = xScale;
        matrix[1][1] = yScale;
        matrix[2][2] = -((f + n) / frustumLength);
        matrix[2][3] = -((2.0 * n * f) / frustumLength); // Fixed index: Row 2, Col 3
        matrix[3][2] = -1.0;                            // Fixed index: Row 3, Col 2
        matrix[3][3] = 0.0;

        return matrix;
    }

    public static double[][] createMatrix_Lookat(double[] eye, double[] target, double[] up) {

        double[] z = normalize(subtract(eye, target));
        double[] x = normalize(cross(up, z));
        double[] y = cross(z, x);

        // Row-vector convention (v*M): basis vectors go in columns, translation in last row.
        double m[][] = new double[4][4];

        m[0][0] = x[0];  m[0][1] = y[0];  m[0][2] = z[0];  m[0][3] = 0;
        m[1][0] = x[1];  m[1][1] = y[1];  m[1][2] = z[1];  m[1][3] = 0;
        m[2][0] = x[2];  m[2][1] = y[2];  m[2][2] = z[2];  m[2][3] = 0;
        m[3][0] = -dot(x, eye);
        m[3][1] = -dot(y, eye);
        m[3][2] = -dot(z, eye);
        m[3][3] = 1;

        return m;
    }

    public static double[] multiply(double[] v, double[][] m) {

        double v_t[] = new double[4];

        v_t[0] = m[0][0] * v[0] + m[1][0] * v[1] + m[2][0] * v[2] + m[3][0] * v[3];
        v_t[1] = m[0][1] * v[0] + m[1][1] * v[1] + m[2][1] * v[2] + m[3][1] * v[3];
        v_t[2] = m[0][2] * v[0] + m[1][2] * v[1] + m[2][2] * v[2] + m[3][2] * v[3];
        v_t[3] = m[0][3] * v[0] + m[1][3] * v[1] + m[2][3] * v[2] + m[3][3] * v[3];

        return v_t;
    }

    
     public static double[] subtract(double[] a, double[] b) {
         return new double[] {
             a[0] - b[0],
             a[1] - b[1],
             a[2] - b[2]
         };
     }


    public static double[] cross(double[] a, double[] b) {
        return new double[] {
            a[1]*b[2] - a[2]*b[1],
            a[2]*b[0] - a[0]*b[2],
            a[0]*b[1] - a[1]*b[0]               
        };
    }

    public static double dot(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    public static double[] normalize(double[] v) {
        double length = (double)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return new double[] {
            v[0]/length,
            v[1]/length,
            v[2]/length
        };
    }

    public static double[] normal(double a[], double b[], double c[])
    {
        return normalize(cross(subtract(b, a), subtract(c,a)));
    }

    // In-place variants — write into dst instead of allocating. Used by World's ThreadLocal scratch.
    static void multiplyMatrices(double[][] dst, double[][] a, double[][] b) {
        for (int i = 0; i < 4; i++) {
            dst[i][0] = a[i][0]*b[0][0] + a[i][1]*b[1][0] + a[i][2]*b[2][0] + a[i][3]*b[3][0];
            dst[i][1] = a[i][0]*b[0][1] + a[i][1]*b[1][1] + a[i][2]*b[2][1] + a[i][3]*b[3][1];
            dst[i][2] = a[i][0]*b[0][2] + a[i][1]*b[1][2] + a[i][2]*b[2][2] + a[i][3]*b[3][2];
            dst[i][3] = a[i][0]*b[0][3] + a[i][1]*b[1][3] + a[i][2]*b[2][3] + a[i][3]*b[3][3];
        }
    }

    static void createMatrix_Scaler(double[][] m, double sx, double sy, double sz) {
        m[0][0]=sx; m[0][1]=0;  m[0][2]=0;  m[0][3]=0;
        m[1][0]=0;  m[1][1]=sy; m[1][2]=0;  m[1][3]=0;
        m[2][0]=0;  m[2][1]=0;  m[2][2]=sz; m[2][3]=0;
        m[3][0]=0;  m[3][1]=0;  m[3][2]=0;  m[3][3]=1;
    }

    static void createMatrix_CombinedRotator(double[][] m, double angleX, double angleY, double angleZ) {
        double cx = Math.cos(Math.toRadians(angleX)), sx = Math.sin(Math.toRadians(angleX));
        double cy = Math.cos(Math.toRadians(angleY)), sy = Math.sin(Math.toRadians(angleY));
        double cz = Math.cos(Math.toRadians(angleZ)), sz = Math.sin(Math.toRadians(angleZ));
        m[0][0]=cy*cz;            m[0][1]=cy*sz;            m[0][2]=-sy;     m[0][3]=0;
        m[1][0]=sx*sy*cz-cx*sz;  m[1][1]=sx*sy*sz+cx*cz;  m[1][2]=sx*cy;   m[1][3]=0;
        m[2][0]=cx*sy*cz+sx*sz;  m[2][1]=cx*sy*sz-sx*cz;  m[2][2]=cx*cy;   m[2][3]=0;
        m[3][0]=0;                m[3][1]=0;                m[3][2]=0;        m[3][3]=1;
    }

    static void createMatrix_Translator(double[][] m, double tx, double ty, double tz) {
        m[0][0]=1; m[0][1]=0; m[0][2]=0; m[0][3]=0;
        m[1][0]=0; m[1][1]=1; m[1][2]=0; m[1][3]=0;
        m[2][0]=0; m[2][1]=0; m[2][2]=1; m[2][3]=0;
        m[3][0]=tx; m[3][1]=ty; m[3][2]=tz; m[3][3]=1;
    }

}
