#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform float battery;
uniform vec3 colorFilter;
uniform float focus;
uniform float Glare;
uniform float u_DSPActive;
uniform float AspectRatio;

float hashNoise(vec2 p, float t) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z + fract(t * 13.37));
}

void main() {
    vec2 uv = texCoord;
    
    // Ojo de Pez & Optical distance with Aspect Ratio correction
    vec2 center = vec2(0.5, 0.5);
    vec2 aspectVec = vec2((uv.x - 0.5) * AspectRatio, uv.y - 0.5);
    float dist = length(aspectVec);
    vec2 distVec = uv - center;
    uv = center + distVec * (dist * dist * 0.45 + 0.82);

    // Zoom-dependent distortions
    if (focus > 0.0) {
        // Slight chromatic aberration on edges when focused
        float ab = focus * 0.003 * dist;
        float r = texture(DiffuseSampler, uv + vec2(ab, 0.0)).r;
        float g = texture(DiffuseSampler, uv).g;
        float b = texture(DiffuseSampler, uv - vec2(ab, 0.0)).b;
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

    vec4 baseColor = texture(DiffuseSampler, uv);
    
    // Apply chromatic aberration if zooming
    if (focus > 0.0) {
        float ab = focus * 0.005 * dist;
        baseColor.r = texture(DiffuseSampler, uv + vec2(ab, 0.0)).r;
        baseColor.b = texture(DiffuseSampler, uv - vec2(ab, 0.0)).b;
    }

    float lum = dot(baseColor.rgb, vec3(0.3, 0.59, 0.11));
    
    // 1. Optical Tube Halation / Bloom around intense light sources (torches, fire, lava)
    float bloom = 0.0;
    vec2 bStep = vec2(0.0035, 0.0035);
    float s1 = dot(texture(DiffuseSampler, uv + vec2( bStep.x,  0.0)).rgb,     vec3(0.3, 0.59, 0.11));
    float s2 = dot(texture(DiffuseSampler, uv - vec2( bStep.x,  0.0)).rgb,     vec3(0.3, 0.59, 0.11));
    float s3 = dot(texture(DiffuseSampler, uv + vec2( 0.0,      bStep.y)).rgb, vec3(0.3, 0.59, 0.11));
    float s4 = dot(texture(DiffuseSampler, uv - vec2( 0.0,      bStep.y)).rgb, vec3(0.3, 0.59, 0.11));
    float avgLum = (s1 + s2 + s3 + s4) * 0.25;
    if (avgLum > 0.60) {
        bloom = smoothstep(0.60, 1.0, avgLum) * 0.40;
    }

    float brightness = 1.6 + focus * 0.4; // Slightly brighter when zoomed
    
    vec3 visionColor = vec3(lum * colorFilter.r, lum * colorFilter.g, lum * colorFilter.b) * brightness;
    // Inject halation phosphor bloom
    visionColor += vec3(bloom * colorFilter.r * 1.5, bloom * colorFilter.g * 1.5, bloom * colorFilter.b * 1.5);
    
    // 2. Multiband Scintillation: High-frequency quantum grain + low-frequency microchannel plate mottling
    float scintillation = mix(1.65, 0.35, clamp(lum * 2.5, 0.0, 1.0));
    float noiseIntensity = (0.07 + pow(wear, 1.5) * 1.5 + focus * 0.04) * scintillation;
    
    // DSP Denoising: digital DSP filter reduces analog grain by 75%
    if (u_DSPActive > 0.5) {
        noiseIntensity *= 0.25;
    }
    float fineGrain = hashNoise(uv * 850.0, time) - 0.5;
    float plateMottle = (hashNoise(uv * 120.0, time * 0.05) - 0.5) * 0.35;
    visionColor += (fineGrain + plateMottle) * noiseIntensity;
    
    // Scanlines (subtler in DSP mode)
    float scanlineStrength = (u_DSPActive > 0.5) ? 0.012 : (0.035 + focus * 0.02);
    visionColor -= sin(uv.y * 800.0) * scanlineStrength;
    
    // 3. Glare / Auto-Gating effect
    if (Glare > 0.0) {
        if (u_DSPActive > 0.5) {
            // Smart DSP Auto-Gating: suppress flashbang overexposure and protect sensor
            float gatingProtection = clamp(1.0 - Glare * 0.65, 0.35, 1.0);
            float microFlicker = 1.0 - (hashNoise(vec2(time * 60.0, 0.0), time) * 0.08 * Glare);
            visionColor = clamp(visionColor, 0.0, 1.2) * gatingProtection * microFlicker;
            visionColor += vec3(Glare * 0.25);
        } else {
            // Analog Mode: Classic harsh cathode tube blowout
            float glareAmount = Glare * 2.0;
            visionColor += vec3(glareAmount);
            float maxCol = max(visionColor.r, max(visionColor.g, visionColor.b));
            visionColor = mix(visionColor, vec3(maxCol), Glare);
        }
    }

    // 4. Optical S-Curve Tone Compression (Prevents hard ugly digital clipping)
    visionColor = visionColor / (1.0 + visionColor * 0.25);

    // 5. Dual-stage Military Ocular Vignette
    float ocularVignette = 1.0 - smoothstep(0.42 - focus * 0.08, 0.69 - focus * 0.14, dist);
    visionColor *= ocularVignette;

    fragColor = vec4(visionColor * globalAlpha, 1.0);
}
