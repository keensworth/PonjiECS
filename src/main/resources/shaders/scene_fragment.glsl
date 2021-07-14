#version 330

in vec3 outColor;
in vec3 surfacePos;
in vec2 outTexCoords;
flat in vec3 outNormal;
out vec4 fragColor;

uniform vec3 cameraPos;
uniform vec3 mousePos;
uniform vec3[15] pointLights;
uniform vec3[15] spotLights;

uniform vec3 diffuseColor;
uniform vec3 specularColor;

uniform sampler2D texture;

uniform int textured;

vec3 CalculateLight(vec3 lightPos, bool isPointLight){
    //light attributes
    vec3 lightDir = normalize(vec3(0, 0, -1));
    vec3 lightColor = vec3(1, 1, 1);
    vec3 camera = cameraPos;
    vec3 surfaceToCamera = camera-surfacePos;
    float ambientStrength = 0.0;
    float coneAngle = 30;
    float lightAttenuation = 1.0f;
    vec3 surfaceToLight = normalize(lightPos - surfacePos);
    float distanceToLight = length(lightPos - surfacePos);
    float attenuation = 1.0 / (1.0 + lightAttenuation*pow(distanceToLight/100,2));

    float intensity = 1;

    if (isPointLight){
        //cone restrictions (affects attenuation)
        float outerAngle = 30;
        float innerAngle = 12.5;
        float lightToSurfaceAngle = degrees(acos(dot(-surfaceToLight, normalize(lightDir))));
        float epsilon = innerAngle-outerAngle;
        intensity = clamp((lightToSurfaceAngle - outerAngle)/epsilon, 0.0, 1.0);
        if (lightToSurfaceAngle > coneAngle){
            attenuation = 0.0;
        }
    }

    float diffuseCoefficient = 0;
    vec3 diffuse = vec3(0,0,0);

    if (textured==1){
        //diffuse
        diffuseCoefficient = max(0.0, dot(outNormal, surfaceToLight));
        diffuse = diffuseCoefficient * ((texture2D(texture, outTexCoords).rgb) * lightColor) * intensity;
    } else {
        //diffuse
        diffuseCoefficient = max(0.0, dot(outNormal, surfaceToLight));
        diffuse = diffuseCoefficient * ((outColor + diffuseColor) * lightColor) * intensity;
    }

    //specular
    float specularCoefficient = 0.0;
    if (diffuseCoefficient > 0.0){
        specularCoefficient = pow(max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, outNormal))), 1);
    }
    vec3 specular = specularCoefficient * ((outColor + (specularColor/2)) * lightColor) * intensity;

    //linear color (color before gamma correction)
    vec3 result = (attenuation*(diffuse));
    return result;
}

void main() {
    float gamma = 1.6;
    int i;
    vec3 result = vec3(0,0,0);

    for (i = 0; i < pointLights.length; i++){
        vec3 lightPos = pointLights[i];
        if(lightPos != vec3(0,0,0)){
            result += CalculateLight(lightPos, true);
        }
    }

    for (i = 0; i < spotLights.length; i++){
        vec3 lightPos = spotLights[i];
        if(lightPos != vec3(0,0,0)){
            result += CalculateLight(lightPos, false);
        }
    }

    result += CalculateLight(mousePos, false);
    result += 0.0f * outColor;
    result = pow(result, vec3(1.0 / gamma));
    fragColor = vec4(result,1f);
}