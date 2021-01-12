#version 330

in vec3 outColor;
in vec3 surfacePos;
flat in vec3 outNormal;
out vec4 fragColor;
uniform vec3 cameraPos;

void main()
{
    vec3 lightDir = normalize(vec3(0, 0, -1));
    //vec3 lightColor = vec3(255f/255, 230f/255, 146f/255);
    vec3 lightColor = vec3(1, 1, 1);
    vec3 lightPos = vec3(cameraPos.x,cameraPos.y,cameraPos.z);
    vec3 camera = cameraPos;
    vec3 surfaceToCamera = camera-surfacePos;

    float ambientStrength = 0.1;
    float coneAngle = 30;
    float lightAttenuation = 1.0f;
    vec3 surfaceToLight = normalize(lightPos - surfacePos);
    float distanceToLight = length(lightPos - surfacePos);
    float attenuation = 1.0 / (1.0 + lightAttenuation*pow(distanceToLight/400,2));

    //cone restrictions (affects attenuation)
    float outerAngle = 25;
    float innerAngle = 12.5;
    float lightToSurfaceAngle = degrees(acos(dot(-surfaceToLight, normalize(lightDir))));
    float epsilon = innerAngle-outerAngle;
    float intensity = clamp((lightToSurfaceAngle - outerAngle)/epsilon,0.0,1.0);
    //-----REMOVE TO CREATE POINT LIGHT-----//

    if (lightToSurfaceAngle > coneAngle){
        attenuation = 0.0;
    }


    //ambient
    vec3 ambient = ambientStrength * (outColor * lightColor);

    //diffuse
    float diffuseCoefficient = max(0.0, dot(outNormal, -surfaceToLight));
    vec3 diffuse = diffuseCoefficient * (outColor * lightColor) * intensity;

    //specular

    float specularCoefficient = 0.0;
    if (diffuseCoefficient > 0.0){
        specularCoefficient = pow(max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, outNormal))), 0.5);
    }
    vec3 specular = specularCoefficient * (outColor * lightColor)*intensity;


    //linear color (color before gamma correction)
    vec3 result = (ambient + attenuation*(diffuse));
    fragColor = vec4(result,1.0f);
}