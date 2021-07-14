#version 330

layout (location=0) in vec3 position;
out vec4 outColor;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform vec4 inColor;

void main()
{
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
    //gl_Position = vec4(position, 1.0);
    outColor = inColor;
}