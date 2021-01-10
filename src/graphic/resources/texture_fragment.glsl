#version 330 core
out vec4 FragColor;
in vec4 outColor;

uniform sampler2D scene;

void main()
{
    vec4 hdrColor = texture(scene, gl_FragCoord.xy / vec2(450,900)).rgba;
    FragColor = hdrColor;
}