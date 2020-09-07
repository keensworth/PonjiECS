package util;

public class Geometry {
    public static float distanceFromPointToLine(int x1, int y1, int x2, int y2, float[] pt){
        float px = pt[0];
        float py = pt[1];

        float constant = (float) (dotProduct(x1,y1,x2,y2,px,py) / (Math.pow(x2-x1,2) + Math.pow(y2-y1,2)));

        float dx = (px-x1) - constant*(x2-x1);
        float dy = (py-y1) - constant*(y2-y1);

        return magnitude(dx,dy);
    }

    public static float distanceFromPointToPoint(float x1, float y1, float x2, float y2){
        return (float) Math.sqrt(Math.pow((x1-x2),2) + Math.pow((y1-y2),2));
    }

    public static float magnitude(float x1, float y1, float x2, float y2){
        return (float) Math.sqrt(Math.pow(x2-x1,2) + Math.pow(y2-y1,2));
    }

    public static float magnitude(float x, float y){
        return (float) Math.sqrt(Math.pow(x,2) + Math.pow(y,2));
    }

    public static float dotProduct(float x1, float y1, float x2, float y2, float px, float py){
        float v1X = x2 - x1;
        float v1Y = y2 - y1;
        float v2X = px - x1;
        float v2Y = py - y1;

        return ((v1X*v2X) + (v1Y*v2Y));
    }

    public static float dotProduct(float v1X, float v1Y, float v2X, float v2Y){
        return ((v1X*v2X) + (v1Y*v2Y));
    }

    public static float[] norm(float x1, float y1, float x2, float y2){
        float mag = magnitude(x1,y1,x2,y2);

        float xComp = x2-x1;
        float yComp = y2-y1;

        float[] normal = new float[2];
        normal[0] = xComp / mag;
        normal[1] = yComp / mag;

        return normal;
    }
}
