#version 330

layout (location=0) in vec3 position;
layout (location=1) in vec3 normal;
out vec3 outColor;
flat out vec3 outNormal;
out vec3 surfacePos;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform vec3 inColor;

void main()
{
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
    surfacePos = vec3(modelMatrix * vec4(position,1.0f));
    //gl_Position = vec4(position, 1.0);
    outColor = inColor;
    outNormal = normal;
}