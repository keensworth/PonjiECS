package graphic;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Test {
    public static void main(String[] args) {
        Window window = new Window("test", 500, 500, true);
        window.init();

        Renderer renderer = new Renderer();
        renderer.init(window);

        //aspectRatio = (float)window.getWidth() / window.getHeight();
        //Matrix4f projectionMatrix = new Matrix4f().perspective(60, 0.5f, -0.01f, -1000);

        Matrix4f modelMatrix = new Matrix4f().identity().translate(0, 0, -50).scale(1);

        Matrix4f projectionMatrix = new Matrix4f().perspective(60, 0.5f, -0.01f, -1000);

        Matrix4f viewMatrix = new Matrix4f().identity();
        viewMatrix.rotate(0, new Vector3f(1, 0, 0))
                .rotate(0, new Vector3f(0, 1, 0))
                .rotate(0, new Vector3f(0, 0, 1));
        viewMatrix.translate(0, 0, 0);

        float distance = -50;

        Vector4f left = new Vector4f(-1,0,distance,1);
        Vector4f right = new Vector4f(1,0,distance,1);
        Vector4f upper = new Vector4f(0,1,distance,1);
        Vector4f lower = new Vector4f(0,-1,distance,1);

        Matrix4f temp = (viewMatrix.invert()).mul(projectionMatrix.invert());
        Vector4f answer = left.mul(temp);

        System.out.println(" ");
        System.out.println(answer.toString());


        /*
        System.out.println("Projection: ");
        System.out.println(projectionMatrix.toString());
        System.out.println(" ");
        System.out.println("Model: ");
        System.out.println(modelMatrix.toString());
        System.out.println(" ");
        System.out.println("View: ");
        System.out.println(viewMatrix.toString());

         */


        while(true){
            window.update();
        }
    }
}
