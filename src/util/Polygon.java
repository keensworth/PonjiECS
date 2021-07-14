package util;

public class Polygon {
    public float[] xPoints;
    public float[] yPoints;
    public int sides;

    public float maxX;
    public float maxY;
    public float minX;
    public float minY;

    public void Polygon(float[] xPoints, float[] yPoints, int sides) {
        this.xPoints = xPoints;
        this.yPoints = yPoints;
        this.sides = sides;

        float[] xBounds = getBounds(xPoints);
        float[] yBounds = getBounds(yPoints);

        this.maxX = xBounds[1];
        this.maxY = yBounds[1];
        this.minX = xBounds[0];
        this.minY = yBounds[0];
    }

    private float[] getBounds(float[] values){
        float[] bounds = {0,0};
        for (int i = 0; i < values.length; i++){
            if (i==0){
                bounds[0] = values[i];
                bounds[1] = values[i];
            }
            if (values[i]<bounds[0])
                bounds[0] = values[i];
            if (values[i]>bounds[1])
                bounds[1] = values[i];
        }
        return bounds;
    }
}
