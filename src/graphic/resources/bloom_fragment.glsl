#version 330 core
out vec4 FragColor;

uniform sampler2D image;

uniform bool horizontal;
uniform float weight[5] = float[] (0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main(){
    vec3 result = texture(image, gl_FragCoord.xy / vec2(450,900)).rgb * weight[0]; // current fragment's contribution
    if(horizontal)
    {
        for(int i = 1; i < 5; ++i)
        {
            result += texture2D(image, (gl_FragCoord.xy + vec2(i, 0.0)) / vec2(450,900)).rgb * weight[i];
            result += texture2D(image, (gl_FragCoord.xy - vec2(i, 0.0)) / vec2(450,900)).rgb * weight[i];
        }
    }
    else
    {
        for(int i = 1; i < 5; ++i)
        {
            result += texture2D(image, (gl_FragCoord.xy + vec2(0.0, i)) / vec2(450,900)).rgb * weight[i];
            result += texture2D(image, (gl_FragCoord.xy - vec2(0.0, i)) / vec2(450,900)).rgb * weight[i];
        }
    }
    FragColor = vec4(result, 1.0);
}
