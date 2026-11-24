#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalMask;
in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform float battery;
uniform float focus;
uniform float Interference;
uniform float u_DSPActive;
uniform float AspectRatio;
uniform float IrisActive;
uniform vec2 OutSize;

float hashNoise(vec2 p, float t) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z + fract(t * 13.37));
}

vec3 ironbow(float t) {
    t = clamp(t, 0.0, 1.0);
    vec3 c0 = vec3(0.01, 0.02, 0.08); // Dark navy
    vec3 c1 = vec3(0.28, 0.00, 0.55); // Purple
    vec3 c2 = vec3(0.85, 0.10, 0.15); // Red
    vec3 c3 = vec3(1.00, 0.55, 0.00); // Orange
    vec3 c4 = vec3(1.00, 0.95, 0.30); // Yellow
    vec3 c5 = vec3(1.00, 1.00, 1.00); // White

    if (t < 0.20) return mix(c0, c1, t / 0.20);
    if (t < 0.40) return mix(c1, c2, (t - 0.20) / 0.20);
    if (t < 0.70) return mix(c2, c3, (t - 0.40) / 0.30);
    if (t < 0.90) return mix(c3, c4, (t - 0.70) / 0.20);
    return mix(c4, c5, (t - 0.90) / 0.10);
}

float getLum(vec2 coord) {
    float worldLum = dot(texture(DiffuseSampler, coord).rgb, vec3(0.299, 0.587, 0.114));
    float entityLum = texture(ThermalMask, coord).a * 0.65;
    return max(worldLum, entityLum);
}

float sobelEdge(vec2 coord) {
    vec2 d = vec2(0.0012, 0.0012);
    float tl = getLum(coord + vec2(-d.x,  d.y));
    float t  = getLum(coord + vec2( 0.0,  d.y));
    float tr = getLum(coord + vec2( d.x,  d.y));
    float l  = getLum(coord + vec2(-d.x,  0.0));
    float r  = getLum(coord + vec2( d.x,  0.0));
    float bl = getLum(coord + vec2(-d.x, -d.y));
    float b  = getLum(coord + vec2( 0.0, -d.y));
    float br = getLum(coord + vec2( d.x, -d.y));

    float gx = -tl - 2.0 * l - bl + tr + 2.0 * r + br;
    float gy = -tl - 2.0 * t - tr + bl + 2.0 * b + br;
    return length(vec2(gx, gy));
}

void main() {
    vec2 uv = texCoord;
    vec2 center = vec2(0.5, 0.5);
    vec2 aspectVec = vec2((uv.x - 0.5) * AspectRatio, uv.y - 0.5);
    float dist = length(aspectVec);
    vec2 distVec = uv - center;
    uv = center + distVec * (dist * dist * 0.45 + 0.82);

    // Zoom Tightening
    if (focus > 0.0) {
        uv = mix(uv, center + (uv - center) * 0.98, focus);
    }

    // Thermal Interference (e.g., in Nether) - Wobbly distortion MUST happen before sampling!
    if (Interference > 0.0) {
        uv.x += sin(uv.y * 80.0 + time * 15.0) * 0.015 * Interference;
        uv.y += cos(uv.x * 80.0 + time * 18.0) * 0.015 * Interference;
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

    // Slight pixelation effect when zooming in thermal (shared for world & entity mask)
    if (focus > 0.5) {
        float pixels = 400.0 - (focus - 0.5) * 200.0;
        uv = floor(uv * pixels) / pixels;
    }

    vec4 baseColor = texture(DiffuseSampler, uv);
    if (IrisActive > 0.5) {
        // 3x3 smooth kernel on DiffuseSampler only to eliminate Iris/Photon TAA & dither grids without blurring ThermalMask
        vec2 tx = 1.0 / max(OutSize, vec2(1.0));
        vec3 c00 = texture(DiffuseSampler, uv + vec2(-tx.x, -tx.y)).rgb;
        vec3 c10 = texture(DiffuseSampler, uv + vec2(  0.0, -tx.y)).rgb;
        vec3 c20 = texture(DiffuseSampler, uv + vec2( tx.x, -tx.y)).rgb;
        vec3 c01 = texture(DiffuseSampler, uv + vec2(-tx.x,   0.0)).rgb;
        vec3 c21 = texture(DiffuseSampler, uv + vec2( tx.x,   0.0)).rgb;
        vec3 c02 = texture(DiffuseSampler, uv + vec2(-tx.x,  tx.y)).rgb;
        vec3 c12 = texture(DiffuseSampler, uv + vec2(  0.0,  tx.y)).rgb;
        vec3 c22 = texture(DiffuseSampler, uv + vec2( tx.x,  tx.y)).rgb;
        baseColor.rgb = baseColor.rgb * 0.25
                      + (c10 + c01 + c21 + c12) * 0.125
                      + (c00 + c20 + c02 + c22) * 0.0625;
    }
    vec4 entityMask = texture(ThermalMask, uv);

    float lum = dot(baseColor.rgb, vec3(0.3, 0.59, 0.11));
    if (IrisActive > 0.5) {
        // Soft HDR compression & shadow-lift gamma for shaderpack lighting
        float compressed = lum / (1.0 + lum * 0.45);
        lum = pow(clamp(compressed * 1.15, 0.0, 1.0), 0.78);
    }
    float heatSignal = 0.0;
    float coldSignal = 0.0;
    float cyanBias = min(baseColor.g, baseColor.b) - baseColor.r;

    // Ignore pure bright daylight sand/snow
    if (baseColor.r > 0.85 && baseColor.g > 0.85 && baseColor.b > 0.85) {
        heatSignal = 0.0;
    }
    // Natural thermal sources (Lava, Fire, Magma, Lit Furnaces, Campfires, Torches)
    else if (baseColor.r > baseColor.b * 1.35 && baseColor.r > 0.35 && lum > 0.30) {
        float chromaticHeat = (baseColor.r - baseColor.b * 1.35) * 1.8;
        float lumHeat = clamp((lum - 0.30) * 1.5, 0.0, 1.0);
        heatSignal = clamp(chromaticHeat * lumHeat * 2.2, 0.0, 1.0);
    }
    // Cryogenic cold sources: high blue dominance and moderate luminance (Ice, Packed Ice, Blue Ice, Powder Snow)
    else if (baseColor.b > baseColor.r * 1.45 && baseColor.b > 0.35 && cyanBias < 0.35) {
        coldSignal = clamp((baseColor.b - baseColor.r * 1.45) * 1.6, 0.0, 0.85);
    }

    // Suppress chromatic texture contrast on ambient non-thermal surfaces (e.g. zombie teal shirt vs green skin)
    // so cold/undead entities and blocks maintain a uniform ambient thermal signature while preserving 3D form
    if (heatSignal <= 0.01 && coldSignal <= 0.01) {
        float maxC = max(baseColor.r, max(baseColor.g, baseColor.b));
        float minC = min(baseColor.r, min(baseColor.g, baseColor.b));
        float sat = (maxC > 0.01) ? ((maxC - minC) / maxC) : 0.0;
        float structuralLum = mix(lum, maxC * 0.72, sat * 0.55);
        lum = mix(0.24, clamp(structuralLum, 0.0, 1.0), 0.32);
    }

    // World Thermal Ironbow calculation
    float baseTemp = lum * 0.35;
    float finalTemp = clamp(baseTemp + clamp(heatSignal, 0.0, 1.0) * 0.75 - coldSignal * 0.50, 0.0, 1.0);
    vec3 visionColor = ironbow(finalTemp);

    // Subtle thermal radiation bleed around natural fire/lava blocks
    if (heatSignal > 0.35) {
        vec3 heatBloom = vec3(1.0, 0.45, 0.05) * (heatSignal - 0.35) * 0.16;
        visionColor += heatBloom;
    }

    // Dedicated entity thermal mask compositing (isolated from terrain/water RGB contamination)
    if (entityMask.a > 0.02) {
        float maxRgb = max(max(entityMask.r, entityMask.g), entityMask.b);
        vec3 normHue = (maxRgb > 0.01) ? (entityMask.rgb / maxRgb) : vec3(0.0);
        vec3 entityCol;

        // Default cyan sentinel (#00FFFF) or critical Lua targets -> smooth Ironbow orange..gold..white-hot
        if (min(normHue.g, normHue.b) - normHue.r > 0.45) {
            float entityTemp = clamp(entityMask.a, 0.0, 1.0);
            entityCol = ironbow(entityTemp);
        }
        // Warm FLIR Ironbow tones (#FF0000..#FF8800..#FFBB00..#FFFF00) -> stable hue-tiered heat
        else if (normHue.r > 0.65 && normHue.b < 0.35) {
            float hueTier = mix(0.56, 0.94, clamp(normHue.g, 0.0, 1.0));
            float entityTemp = clamp(hueTier * entityMask.a, 0.0, 0.98);
            entityCol = ironbow(entityTemp);
        }
        // Custom Lua tint colors (#00FF44, #FF00FF, #FFFFFF, etc.)
        else {
            entityCol = entityMask.rgb * (0.35 + 0.65 * entityMask.a);
        }

        float coverage = smoothstep(0.02, 0.14, entityMask.a);
        visionColor = mix(visionColor, entityCol, coverage);
    }

    // 2. Microbolometer Fixed Pattern Noise (Column FPN stripes inherent to uncooled FLIR sensors)
    float fpn = sin(uv.x * 1280.0) * ((u_DSPActive > 0.5) ? 0.003 : 0.015);
    visionColor += vec3(fpn);

    // 3. ENVG Edge Fusion (DSP Mode with CC: Tweaked)
    if (u_DSPActive > 0.5) {
        float edge = sobelEdge(uv);
        edge = smoothstep(0.12, 0.36, edge);
        vec3 edgeColor = vec3(0.12, 0.92, 1.0) * edge * 0.90;
        visionColor += edgeColor;

        // Digital NUC (Non-Uniformity Correction) shutter recalibration click every 35s
        float nucCycle = mod(time, 35.0);
        if (nucCycle < 0.06) {
            visionColor = mix(visionColor, vec3(0.08, 0.08, 0.12), 0.80);
        }
    }

    // 4. Sensor grain
    float noiseIntensity = clamp(0.08 + pow(wear, 1.5) * 0.35 + focus * 0.04, 0.0, 0.45);
    if (u_DSPActive > 0.5) noiseIntensity *= 0.25;
    visionColor *= (1.0 - noiseIntensity + noiseIntensity * hashNoise(uv * 400.0, time));

    // Thermal Interference (e.g. in the Nether)
    if (Interference > 0.0) {
        float interferenceNoise = hashNoise(uv * 300.0, time * 3.0) * Interference;
        visionColor += vec3(interferenceNoise * 2.0, interferenceNoise * 0.4, 0.0);

        float pulse = (sin(time * 15.0) * 0.5 + 0.5) * Interference;
        visionColor = mix(visionColor, vec3(1.0, 0.1, 0.0), pulse * 0.4);
    }

    visionColor -= sin(uv.y * 600.0) * 0.02;

    // Military thermal ocular vignette
    visionColor *= (1.0 - smoothstep(0.44 - focus * 0.08, 0.70 - focus * 0.14, dist));

    fragColor = vec4(visionColor * globalAlpha, 1.0);
}
