#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;
uniform vec3 colorFilter;
uniform float focus;

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

    // Zoom-dependent distortions
    if (focus > 0.0) {
        // Slight chromatic aberration on edges when focused
        float ab = focus * 0.003 * dist;
        float r = texture2D(DiffuseSampler, uv + vec2(ab, 0.0)).r;
        float g = texture2D(DiffuseSampler, uv).g;
        float b = texture2D(DiffuseSampler, uv - vec2(ab, 0.0)).b;
        // We'll apply this to the lum calculation later if needed, 
        // but for now let's just use it to shift the baseColor
    }

    float wear = 0.0;
    float globalAlpha = 1.0;
    if (battery < 0.15) {
        wear = (0.15 - battery) / 0.15;
        uv.x += sin(uv.y * 20.0 + time * 10.0) * 0.005 * pow(wear, 2.0);
        
        // IMPROVED CHAOTIC FLICKER AT < 7%
        if (battery < 0.07) {
            float death = (0.07 - battery) / 0.07;
            // Pulse speed increases slightly as battery dies
            float pulse = sin(time * (10.0 + death * 30.0));
            
            // Only flicker off when pulse is high. Threshold lowers as death increases.
            // 1.0 -> 0.0
            if (pulse > (0.8 + (1.0 - death) * 0.5)) {
                globalAlpha = 0.1; // Dim heavily but keep faint outline
            }
            // Add extra random micro-glitches (white noise flash)
            if (fract(time * 43.0) < (death * 0.1)) globalAlpha = 1.5; // Flash bright!
        }
    }

    vec4 baseColor = texture2D(DiffuseSampler, uv);
    
    // Apply chromatic aberration if zooming
    if (focus > 0.0) {
        float ab = focus * 0.005 * dist;
        baseColor.r = texture2D(DiffuseSampler, uv + vec2(ab, 0.0)).r;
        baseColor.b = texture2D(DiffuseSampler, uv - vec2(ab, 0.0)).b;
    }

    float lum = dot(baseColor.rgb, vec3(0.3, 0.59, 0.11));
    
    float brightness = 1.6 + focus * 0.4; // Slightly brighter when zoomed
    // Remove linear darkening, rely on flicker
    // if (battery < 0.05) brightness *= (battery / 0.05); 
    
    vec3 visionColor = vec3(lum * colorFilter.r, lum * colorFilter.g, lum * colorFilter.b) * brightness;
    
    // Increased noise when focused
    float noiseIntensity = 0.08 + pow(wear, 1.5) * 1.5 + focus * 0.05;
    visionColor += (noise(uv + fract(time * 0.01)) - 0.5) * noiseIntensity;
    
    // Scanlines
    visionColor -= sin(uv.y * 800.0) * (0.04 + focus * 0.02);
    
    // Tightening Vignette
    visionColor *= (1.0 - smoothstep(0.4 - focus * 0.1, 0.7 - focus * 0.15, dist));

    gl_FragColor = vec4(visionColor * globalAlpha, 1.0);
}
