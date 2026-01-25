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
    
    vec3 coldBackground = vec3(lum * 0.0, lum * 0.15, lum * 0.45);
    float heatSignal = 0.0;
    if (baseColor.r > 0.9 && baseColor.g > 0.9 && baseColor.b > 0.9) {
        heatSignal = 1.0;
    } else if (lum > 0.85) {
        heatSignal = (lum - 0.85) * 4.0;
    }
    
    vec3 heatColor = vec3(1.0, 0.4, 0.0) * 2.0;
    vec3 visionColor = mix(coldBackground, heatColor, clamp(heatSignal, 0.0, 1.0)) * 1.6;
    visionColor = max(visionColor, vec3(0.0, 0.05, 0.15));

    if (battery < 0.05) visionColor *= (battery / 0.05);
    
    visionColor += (noise(uv + fract(time * 0.01)) - 0.5) * (0.08 + pow(wear, 1.5) * 1.5);
    visionColor -= sin(uv.y * 800.0) * 0.04;
    visionColor *= (1.0 - smoothstep(0.4, 0.7, dist));

    gl_FragColor = vec4(visionColor * globalAlpha, 1.0);
}