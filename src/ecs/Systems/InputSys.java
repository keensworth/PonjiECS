package ecs.Systems;

import ecs.Components.*;
import ecs.System;
import static org.lwjgl.glfw.GLFW.*;

import graphic.Window;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import util.ComponentMask;
import util.ETree.EntNode;

public class InputSys extends System {
    private Input input;
    private Window window;
    private GLFWCursorPosCallback cursor_position_callback;
    private GLFWMouseButtonCallback mouse_button_callback;


    public InputSys() {
        super(Input.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask componentMask, boolean entityChange) {
        java.lang.System.out.println("Updating InputSys");
        updateValues(componentMask);

        //Check for input
        glfwPollEvents();

        return null;
    }

    private void updateCursorPos(double xpos, double ypos){
        input.setMousePos(new int[]{(int)xpos, (int)ypos});
        java.lang.System.out.println(xpos + " " + ypos);
    }

    private void updateMouseClick(int button, int action){
        if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {   //left click, set clicked
            input.setClicked();
        } else if (button == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {  //escape, close window
            glfwSetWindowShouldClose(window.getWindowHandle(), true);
        }
    }

    private void updateValues(ComponentMask componentMask){
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
        }
        input = (Input) componentMask.getComponent(Input.class);
    }

    @Override
    public void exit() {

    }
}
