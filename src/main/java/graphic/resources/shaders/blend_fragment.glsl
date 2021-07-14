#version 330 core
out vec4 FragColor;

uniform sampler2D scene;
uniform sampler2D bloomBlur;
//uniform float exposure = 2;

void main()
{
    const float gamma = 2.2;
    const float exposure = 1;
    vec4 hdrColor = texture(scene, gl_FragCoord.xy / vec2(450,900)).rgba;
    vec4 bloomColor = texture(bloomBlur, gl_FragCoord.xy / vec2(450,900)).rgba;
    hdrColor += bloomColor; // additive blending
    // tone mapping
    vec4 result = vec4(1.0) - exp(-hdrColor * exposure);
    // gamma correction
    result = pow(result, vec4(1.0 / gamma));
    FragColor = result;
}