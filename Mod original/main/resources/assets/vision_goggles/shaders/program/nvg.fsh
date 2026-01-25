#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;

float noise(vec2 co) {
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    
    // Ojo de Pez
    vec2 center = vec2(0.5, 0.5);
    vec2 distVec = uv - center;
    float dist = length(distVec);
    uv = center + distVec * (dist * dist * 0.45 + 0.82);

    float wear = 0.0;
    float globalAlpha = 1.0;
    if (battery < 0.15) {
        wear = (0.15 - battery) / 0.15;
        uv.x += sin(uv.y * 20.0 + time * 10.0) * 0.005 * pow(wear, 2.0);
        
        // PARPADEO AJUSTADO AL 6%
        if (battery < 0.06) {
            float death = (0.06 - battery) / 0.06;
            if (fract(time * (10.0 + death * 30.0)) < (death * 0.8)) {
                globalAlpha = 0.0;
            }
        }
    }

    vec4 baseColor = texture2D(DiffuseSampler, uv);
    float lum = dot(baseColor.rgb, vec3(0.3, 0.59, 0.11));
    
    float brightness = 1.6;
    if (battery < 0.05) brightness *= (battery / 0.05);
    vec3 visionColor = vec3(lum * 0.05, lum * 1.0, lum * 0.05) * brightness;
    
    visionColor += (noise(uv + fract(time * 0.01)) - 0.5) * (0.08 + pow(wear, 1.5) * 1.5);
    visionColor -= sin(uv.y * 800.0) * 0.04;
    visionColor *= (1.0 - smoothstep(0.4, 0.7, dist));

    gl_FragColor = vec4(visionColor * globalAlpha, 1.0);
}