import java.util.HashMap;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Mesh {
    List<Face> faces;
    List<Vertex> vertexList = new ArrayList<>();

    Mesh() {
        faces = new ArrayList<>();
    }

    Mesh(String filename) {
        faces = new ArrayList<>();
        HashMap<String, Vertex> posMap = new HashMap<>();
        HashMap<Vertex, List<Face>> vertexFaces = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                int numVertex = Integer.parseInt(cols[0]);
                Vertex[] v = new Vertex[numVertex];
                for (int i = 0; i < numVertex; i++) {
                    double vx = Double.parseDouble(cols[i*3+1].trim());
                    double vy = Double.parseDouble(cols[i*3+2].trim());
                    double vz = Double.parseDouble(cols[i*3+3].trim());
                    String key = vx + "," + vy + "," + vz;
                    Vertex shared = posMap.get(key);
                    if (shared == null) {
                        shared = new Vertex(vx, vy, vz);
                        posMap.put(key, shared);
                        vertexList.add(shared);
                    }
                    v[i] = shared;
                }
                Face f = new Face(v[0], v[1], v[2]);
                faces.add(f);
                vertexFaces.computeIfAbsent(f.v0, k -> new ArrayList<>()).add(f);
                vertexFaces.computeIfAbsent(f.v1, k -> new ArrayList<>()).add(f);
                vertexFaces.computeIfAbsent(f.v2, k -> new ArrayList<>()).add(f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        computeVertexNormals(vertexFaces);
        computeSphericalUVs();
        //computeCubeUVs();
        //computePlanarUVs();
    }

    private void computeVertexNormals(HashMap<Vertex, List<Face>> vertexFaces) {
        HashMap<Face, double[]> faceNormals = new HashMap<>();
        for (Face f : faces) {
            faceNormals.put(f, Utils.normal(f.v0.asArray(), f.v1.asArray(), f.v2.asArray()));
        }

        double creaseThreshold = Math.cos(Math.toRadians(45));
        for (Face f : faces) {
            double[] thisFn = faceNormals.get(f);
            if (thisFn == null) continue;
            Vertex[] verts = {f.v0, f.v1, f.v2};
            for (int i = 0; i < 3; i++) {
                double[] acc = new double[3];
                for (Face neighbour : vertexFaces.get(verts[i])) {
                    double[] n = faceNormals.get(neighbour);
                    if (n != null && Utils.dot(thisFn, n) > creaseThreshold) {
                        acc[0] += n[0]; acc[1] += n[1]; acc[2] += n[2];
                    }
                }
                double[] norm = Utils.normalize(acc);
                f.normals[i][0] = f.normals_model[i][0] = norm[0];
                f.normals[i][1] = f.normals_model[i][1] = norm[1];
                f.normals[i][2] = f.normals_model[i][2] = norm[2];
            }
        }
    }

    public void Render(FrameBuffer fb, Texture texture, Texture normalMap, float lx, float ly, float lz) {
        faces.stream().forEach(f -> f.Render(fb, texture, normalMap, lx, ly, lz));
    }

    private void computePlanarUVs() {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Vertex vtx : vertexList) {
            minX = Math.min(minX, vtx.x_model); maxX = Math.max(maxX, vtx.x_model);
            minY = Math.min(minY, vtx.y_model); maxY = Math.max(maxY, vtx.y_model);
        }
        double rangeX = maxX - minX, rangeY = maxY - minY;
        for (Vertex vtx : vertexList) {
            vtx.u = vtx.u_model = (vtx.x_model - minX) / rangeX;
            vtx.v = vtx.v_model = (vtx.y_model - minY) / rangeY;
            vtx.txm = 1; vtx.tym = 0; vtx.tzm = 0;
        }
    }

    private void computeCubeUVs() {
        for (Vertex vtx : vertexList) {
            double len = Math.sqrt(vtx.x_model*vtx.x_model + vtx.y_model*vtx.y_model + vtx.z_model*vtx.z_model);
            if (len < 1e-10) { vtx.u = vtx.u_model = 0; vtx.v = vtx.v_model = 0.5; continue; }
            double nx = vtx.x_model / len, ny = vtx.y_model / len, nz = vtx.z_model / len;
            double ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
            if (ax >= ay && ax >= az) {
                vtx.u = vtx.u_model = (nz / ax + 1) / 2;
                vtx.v = vtx.v_model = (ny / ax + 1) / 2;
                vtx.txm = 0; vtx.tym = 0; vtx.tzm = Math.signum(nx);
            } else if (ay >= ax && ay >= az) {
                vtx.u = vtx.u_model = (nx / ay + 1) / 2;
                vtx.v = vtx.v_model = (nz / ay + 1) / 2;
                vtx.txm = Math.signum(ny); vtx.tym = 0; vtx.tzm = 0;
            } else {
                vtx.u = vtx.u_model = (nx / az + 1) / 2;
                vtx.v = vtx.v_model = (ny / az + 1) / 2;
                vtx.txm = Math.signum(nz); vtx.tym = 0; vtx.tzm = 0;
            }
        }
    }

    private void computeSphericalUVs() {
        for (Vertex vtx : vertexList) {
            double len = Math.sqrt(vtx.x_model*vtx.x_model + vtx.y_model*vtx.y_model + vtx.z_model*vtx.z_model);
            if (len < 1e-10) { vtx.u = vtx.u_model = 0; vtx.v = vtx.v_model = 0.5; continue; }
            double nx = vtx.x_model / len, ny = vtx.y_model / len, nz = vtx.z_model / len;
            vtx.u = vtx.u_model = 0.5 + Math.atan2(nx, nz) / (2 * Math.PI);
            vtx.v = vtx.v_model = 0.5 - Math.asin(Math.max(-1.0, Math.min(1.0, ny))) / Math.PI;
            double rxz = Math.sqrt(nx*nx + nz*nz);
            if (rxz > 1e-10) { vtx.txm = nz/rxz; vtx.tym = 0; vtx.tzm = -nx/rxz; }
            else              { vtx.txm = 1;       vtx.tym = 0; vtx.tzm = 0; }
        }
    }

    public Mesh deepCopy() {
        Mesh copy = new Mesh();
        HashMap<Vertex, Vertex> vMap = new HashMap<>();
        for (Vertex s : vertexList) {
            Vertex d = new Vertex();
            d.x = s.x; d.y = s.y; d.z = s.z;
            d.x_model = s.x_model; d.y_model = s.y_model; d.z_model = s.z_model;
            d.u = s.u; d.v = s.v; d.u_model = s.u_model; d.v_model = s.v_model;
            d.wClip = s.wClip;
            d.txm = s.txm; d.tym = s.tym; d.tzm = s.tzm;
            copy.vertexList.add(d);
            vMap.put(s, d);
        }
        for (Face f : faces) {
            Face cf = new Face(vMap.get(f.v0), vMap.get(f.v1), vMap.get(f.v2));
            for (int i = 0; i < 3; i++) {
                cf.normals[i][0]       = f.normals[i][0];       cf.normals[i][1]       = f.normals[i][1];       cf.normals[i][2]       = f.normals[i][2];
                cf.normals_model[i][0] = f.normals_model[i][0]; cf.normals_model[i][1] = f.normals_model[i][1]; cf.normals_model[i][2] = f.normals_model[i][2];
                cf.tangents[i][0]      = f.tangents[i][0];      cf.tangents[i][1]      = f.tangents[i][1];      cf.tangents[i][2]      = f.tangents[i][2];
            }
            copy.faces.add(cf);
        }
        return copy;
    }

    // Transforms positions, rotates normals and tangents, computes brightness and face view dir.
    public void applyModelView(double[][] t, double[] light) {
        for (Vertex v : vertexList) v.applyModelView(t);
        for (Face f : faces) {
            Vertex[] cv = {f.v0, f.v1, f.v2};
            for (int i = 0; i < 3; i++) {
                double[] nb = f.normals_model[i];
                double onx = t[0][0]*nb[0] + t[1][0]*nb[1] + t[2][0]*nb[2];
                double ony = t[0][1]*nb[0] + t[1][1]*nb[1] + t[2][1]*nb[2];
                double onz = t[0][2]*nb[0] + t[1][2]*nb[1] + t[2][2]*nb[2];
                double len = Math.sqrt(onx*onx + ony*ony + onz*onz);
                if (len > 1e-10) {
                    f.normals[i][0] = onx /= len;
                    f.normals[i][1] = ony /= len;
                    f.normals[i][2] = onz /= len;
                    double diffuse = Math.max(0.0, light[0]*onx + light[1]*ony + light[2]*onz);
                    f.brightness[i] = 0.08 + diffuse * 0.92;
                }
                Vertex vt = cv[i];
                double ttx = t[0][0]*vt.txm + t[1][0]*vt.tym + t[2][0]*vt.tzm;
                double tty = t[0][1]*vt.txm + t[1][1]*vt.tym + t[2][1]*vt.tzm;
                double ttz = t[0][2]*vt.txm + t[1][2]*vt.tym + t[2][2]*vt.tzm;
                double tlen = Math.sqrt(ttx*ttx + tty*tty + ttz*ttz);
                if (tlen > 1e-10) {
                    f.tangents[i][0] = (float)(ttx / tlen);
                    f.tangents[i][1] = (float)(tty / tlen);
                    f.tangents[i][2] = (float)(ttz / tlen);
                }
            }
            // Per-face view direction: centroid → camera (camera is at origin in view space)
            double cx = -(cv[0].xv + cv[1].xv + cv[2].xv) / 3.0;
            double cy = -(cv[0].yv + cv[1].yv + cv[2].yv) / 3.0;
            double cz = -(cv[0].zv + cv[1].zv + cv[2].zv) / 3.0;
            double vlen = Math.sqrt(cx*cx + cy*cy + cz*cz);
            if (vlen > 1e-10) {
                f.fvx = (float)(cx / vlen);
                f.fvy = (float)(cy / vlen);
                f.fvz = (float)(cz / vlen);
            }
        }
    }

    public void applyProjection(double[][] t) {
        for (Vertex v : vertexList) v.applyProjection(t);
    }

    public static Mesh createCube() {
        Mesh m = new Mesh();
        Vertex v0 = new Vertex(-1,-1,-1), v1 = new Vertex( 1,-1,-1);
        Vertex v2 = new Vertex( 1, 1,-1), v3 = new Vertex(-1, 1,-1);
        Vertex v4 = new Vertex(-1,-1, 1), v5 = new Vertex( 1,-1, 1);
        Vertex v6 = new Vertex( 1, 1, 1), v7 = new Vertex(-1, 1, 1);
        Vertex[] verts = {v0,v1,v2,v3,v4,v5,v6,v7};
        for (Vertex v : verts) m.vertexList.add(v);

        int[][] tris = {
            {0,2,1}, {0,3,2},  // front  (z=-1)
            {4,5,6}, {4,6,7},  // back   (z=+1)
            {0,4,7}, {0,7,3},  // left   (x=-1)
            {1,2,6}, {1,6,5},  // right  (x=+1)
            {0,1,5}, {0,5,4},  // bottom (y=-1)
            {3,7,6}, {3,6,2},  // top    (y=+1)
        };
        HashMap<Vertex, List<Face>> vertexFaces = new HashMap<>();
        for (int[] tri : tris) {
            Face f = new Face(verts[tri[0]], verts[tri[1]], verts[tri[2]]);
            m.faces.add(f);
            for (int idx : tri)
                vertexFaces.computeIfAbsent(verts[idx], k -> new ArrayList<>()).add(f);
        }
        m.computeVertexNormals(vertexFaces);
        m.computeCubeUVs();
        return m;
    }

}
