#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;
uniform float focus;
uniform float Interference;

float noise(vec2 co) {
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    vec2 center = vec2(0.5, 0.5);
    vec2 distVec = uv - center;
    float dist = length(distVec);
    uv = center + distVec * (dist * dist * 0.45 + 0.82);

    // Zoom Tightening
    if (focus > 0.0) {
        uv = mix(uv, center + (uv - center) * 0.98, focus);
    }
    
    // Thermal Interference (e.g., in Nether) - Wobbly distortion MUST happen before sampling!
    if (Interference > 0.0) {
        uv.x += sin(uv.y * 50.0 + time * 10.0) * 0.008 * Interference;
        uv.y += cos(uv.x * 50.0 + time * 12.0) * 0.008 * Interference;
    }

    float wear = 0.0;
    float globalAlpha = 1.0;
    if (battery < 0.15) {
        wear = (0.15 - battery) / 0.15;
        uv.x += sin(uv.y * 20.0 + time * 10.0) * 0.005 * pow(wear, 2.0);
        
        // IMPROVED CHAOTIC FLICKER AT < 7%
        if (battery < 0.07) {
            float death = (0.07 - battery) / 0.07;
            float pulse = sin(time * (10.0 + death * 30.0));
            if (pulse > (0.8 + (1.0 - death) * 0.5)) {
                globalAlpha = 0.1;
            }
            if (fract(time * 43.0) < (death * 0.1)) globalAlpha = 1.5;
        }
    }

    vec4 baseColor = texture2D(DiffuseSampler, uv);
    
    // Slight pixelation effect when zooming in thermal
    if (focus > 0.5) {
        float pixels = 400.0 - (focus - 0.5) * 200.0;
        uv = floor(uv * pixels) / pixels;
        baseColor = texture2D(DiffuseSampler, uv);
    }

    float lum = dot(baseColor.rgb, vec3(0.3, 0.59, 0.11));
    
    vec3 coldBackground = vec3(lum * 0.0, lum * 0.15, lum * 0.45);
    float heatSignal = 0.0;
    
    // Detect Cyan (Living Entities) - More Strict
    // min(G, B) - R must be high AND R must be low to avoid bright white/sand
    float cyanBias = min(baseColor.g, baseColor.b) - baseColor.r;

    if (cyanBias > 0.5 && baseColor.r < 0.6) {
        heatSignal = 1.0;
    } 
    // Ignore Pure White and very bright yellowish blocks (Sand at day)
    else if (baseColor.r > 0.8 && baseColor.g > 0.8) {
        heatSignal = 0.0;
    }
    // Fallback for natural heat (Lava, Fire) -> High lum + Warm tint (R > B)
    else if (lum > 0.85 && baseColor.r > baseColor.b) {
        heatSignal = (lum - 0.85) * 4.0;
    }
    
    vec3 heatColor = vec3(1.0, 0.4, 0.0) * 2.0;
    vec3 visionColor = mix(coldBackground, heatColor, clamp(heatSignal, 0.0, 1.0)) * 1.6;
    visionColor = max(visionColor, vec3(0.0, 0.05, 0.15));
    
    // Noise and Scanlines increase with focus
    float noiseIntensity = 0.08 + pow(wear, 1.5) * 1.5 + focus * 0.04;
    visionColor *= (0.8 + 0.2 * noise(uv * 100.0 + time));
    
    // Thermal Interference (Noise and Color shift)
    if (Interference > 0.0) {
        float interferenceNoise = noise(uv * 200.0 + time * 15.0) * Interference;
        visionColor += vec3(interferenceNoise * 1.5, interferenceNoise * 0.5, 0.0);
        // Make the screen pulse with a reddish tint
        float pulse = (sin(time * 8.0) * 0.5 + 0.5) * Interference;
        visionColor = mix(visionColor, vec3(1.0, 0.2, 0.0), pulse * 0.3);
    }

    visionColor -= sin(uv.y * 600.0) * 0.03;
    
    visionColor *= (1.0 - smoothstep(0.45 - focus * 0.1, 0.7 - focus * 0.15, dist));
    
    gl_FragColor = vec4(visionColor * globalAlpha, 1.0);
}
