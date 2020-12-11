#version 330 core
out vec4 FragColor;

uniform sampler2D scene;
uniform sampler2D bloomBlur;
//uniform float exposure = 2;

void main()
{
    const float gamma = 2.2;
    const float exposure = 1;
    vec3 hdrColor = texture(scene, gl_FragCoord.xy / vec2(450,900)).rgb;
    vec3 bloomColor = texture(bloomBlur, gl_FragCoord.xy / vec2(450,900)).rgb;
    hdrColor += bloomColor; // additive blending
    // tone mapping
    vec3 result = vec3(1.0) - exp(-hdrColor * exposure);
    // also gamma correct while we're at it
    result = pow(result, vec3(1.0 / gamma));
    FragColor = vec4(result, 1.0);
}