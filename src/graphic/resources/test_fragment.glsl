#version 330 core
out vec4 FragColor;

uniform sampler2D scene;

void main()
{
    vec3 hdrColor = texture(scene, gl_FragCoord.xy / vec2(600,400)).rgb;
    FragColor = vec4(hdrColor, 1.0);
}