public class Actor {
    public Mesh model;
    public Texture texture;
    public double[] location;
    public double[] rotation;
    public double[] scale;

    public Actor(Mesh m)
    {
        model = m;
        location = new double[] {0, 0, 0};
        rotation = new double[] {0, 0, 0};
        scale = new double[] {1, 1, 1};
    }
}
