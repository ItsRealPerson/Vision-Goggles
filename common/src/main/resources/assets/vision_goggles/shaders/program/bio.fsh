#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;
uniform float focus;

float noise(vec2 co) {
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    
    // Zoom effect: Tighten the view slightly
    if (focus > 0.0) {
        vec2 center = vec2(0.5, 0.5);
        uv = mix(uv, center + (uv - center) * 0.95, focus);
    }

    // Digital Scanline (moving down)
    float scanline = sin(uv.y * 800.0 - time * 10.0) * 0.05;
    
    // Scanner bar (bright line moving down)
    float scanBar = 0.0;
    float barPos = fract(time * 0.2);
    if (abs(uv.y - barPos) < 0.02) {
        scanBar = 0.2;
    }

    vec4 baseColor = texture2D(DiffuseSampler, uv);
    
    // High Contrast Look
    float gray = dot(baseColor.rgb, vec3(0.299, 0.587, 0.114));
    
    // High-tech amber look
    vec3 techColor = vec3(gray * 1.5, gray * 0.9, gray * 0.2); 
    
    // Add grid overlay
    float grid = 0.0;
    if (mod(uv.x * 50.0, 1.0) < 0.05 || mod(uv.y * 30.0, 1.0) < 0.05) {
        grid = 0.1;
    }

    vec3 finalColor = techColor + scanline + scanBar + grid;

    // Vignette that tightens with focus
    vec2 distVec = uv - vec2(0.5, 0.5);
    float dist = length(distVec);
    float vignette = 1.0 - smoothstep(0.4 - focus * 0.1, 0.8 - focus * 0.2, dist);
    finalColor *= vignette;

    // Battery failure effect
    float globalAlpha = 1.0;
    if (battery < 0.15) {
        if (battery < 0.07) {
            float death = (0.07 - battery) / 0.07;
            float pulse = sin(time * (10.0 + death * 30.0));
            if (pulse > (0.8 + (1.0 - death) * 0.5)) {
                globalAlpha = 0.1;
            }
            if (fract(time * 43.0) < (death * 0.1)) globalAlpha = 1.5;
        }
    }

    gl_FragColor = vec4(finalColor * globalAlpha, 1.0);
}
