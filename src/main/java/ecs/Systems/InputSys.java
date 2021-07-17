package ecs.Systems;

import ecs.Components.*;
import ecs.Entity;
import ecs.System;
import static org.lwjgl.glfw.GLFW.*;

import graphic.Window;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import util.ComponentMask;
import util.ETree.EntNode;

public class InputSys extends System {
    private Input input;
    private Window window;
    private Position position;
    private Entity camera;
    private GLFWCursorPosCallback cursor_position_callback;
    private GLFWMouseButtonCallback mouse_button_callback;
    private GLFWKeyCallback key_press_callback;

    private boolean holding = false;
    private int holdKey = 0;


    public InputSys() {
        super(Input.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        java.lang.System.out.println("Updating InputSys");
        updateValues(components, entityTree);

        //Check for input
        glfwPollEvents();
        updateKeyPress(-1,-1);

        return null;
    }

    private void updateCursorPos(double xpos, double ypos){
        input.setMousePos(new int[]{(int)xpos, (int)ypos});
        java.lang.System.out.println(xpos + " " + ypos);
    }

    private void updateMouseClick(int button, int action){
        if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {   //left click, set clicked
            input.setClicked();
        }
    }

    private void updateKeyPress(int button, int action){
        if (action == GLFW_RELEASE){
            holding = false;
        }
        if (holding){
            Vector3f camPos = position.get(camera);
            if (holdKey == GLFW_KEY_LEFT_SHIFT){
                position.set(camera, new Vector3f(camPos.x, camPos.y, camPos.z-2));
            } else if (holdKey == GLFW_KEY_SPACE){
                position.set(camera, new Vector3f(camPos.x, camPos.y, camPos.z+2));
            } else if (holdKey == GLFW_KEY_A){
                position.set(camera, new Vector3f(camPos.x-2, camPos.y, camPos.z));
            } else if (holdKey == GLFW_KEY_D){
                position.set(camera, new Vector3f(camPos.x+2, camPos.y, camPos.z));
            } else if (holdKey == GLFW_KEY_W){
                position.set(camera, new Vector3f(camPos.x, camPos.y+2, camPos.z));
            } else if (holdKey == GLFW_KEY_S){
                position.set(camera, new Vector3f(camPos.x, camPos.y-2, camPos.z));
            }
        }
        if (button == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {  //escape, close window
            glfwSetWindowShouldClose(window.getWindowHandle(), true);
        } else if (action == GLFW_PRESS){
            holding = true;
            holdKey = button;
            Vector3f camPos = position.get(camera);
            if (button == GLFW_KEY_LEFT_SHIFT){
                position.set(camera, new Vector3f(camPos.x, camPos.y, camPos.z-4));
            } else if (button == GLFW_KEY_SPACE){
                position.set(camera, new Vector3f(camPos.x, camPos.y, camPos.z+4));
            } else if (button == GLFW_KEY_A){
                position.set(camera, new Vector3f(camPos.x-4, camPos.y, camPos.z));
            } else if (button == GLFW_KEY_D){
                position.set(camera, new Vector3f(camPos.x+4, camPos.y, camPos.z));
            } else if (button == GLFW_KEY_W){
                position.set(camera, new Vector3f(camPos.x, camPos.y+4, camPos.z));
            } else if (button == GLFW_KEY_S){
                position.set(camera, new Vector3f(camPos.x, camPos.y-4, camPos.z));
            }
        }
    }

    private void updateValues(ComponentMask components, EntNode entityTree){
        if (window==null){
            window = ecs.getWindow();
            glfwSetCursorPosCallback(window.getWindowHandle(), cursor_position_callback = new GLFWCursorPosCallback() {
                @Override
                public void invoke(long window, double xpos, double ypos) {
                    updateCursorPos(xpos, ecs.height-ypos); //(0,0) - bottom left of window
                }
            });

            glfwSetMouseButtonCallback(window.getWindowHandle(), mouse_button_callback = new GLFWMouseButtonCallback(){
                @Override
                public void invoke(long window, int button, int action, int mods) {
                    updateMouseClick(button, action);
                }
            });

            glfwSetKeyCallback(window.getWindowHandle(), key_press_callback = new GLFWKeyCallback(){
                @Override
                public void invoke(long window, int key, int scancode, int action, int mods) {
                    updateKeyPress(key, action);
                }
            });
        }
        input = (Input) components.getComponent(Input.class);
        position = (Position) components.getComponent(Position.class);
        Entity[] cameras = getEntities(entityTree, new Class[]{Camera.class});
        camera = cameras[0];
    }

    @Override
    public void exit() {

    }
}
