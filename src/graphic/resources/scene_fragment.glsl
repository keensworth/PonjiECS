#version 330

in vec3 outColor;
flat in vec3 outNormal;
out vec4 fragColor;

void main()
{
    vec3 lightDir = normalize(vec3(0,0,-1));
    vec3 lightColor = vec3(1,1,1);
    float ambientStrength = 0.1;
    vec3 ambient = ambientStrength * lightColor;
    float diff = max(dot(outNormal, lightDir)*0.5 + 0.5, 0.0);
    vec3 diffuse = diff * lightColor;
    vec3 result = (diffuse + ambient) * outColor;

    fragColor = vec4(result,1.0f);
    //fragColor = vec4(outColor*outNormal,1.0f);
}