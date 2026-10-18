#version 150

uniform sampler2D DiffuseSampler;
uniform float time;
uniform float battery;
uniform float AspectRatio;
uniform float FlashlightMode;
uniform float u_DSPActive;

in vec2 texCoord;
out vec4 fragColor;

// stable integer hash noise
float hashNoise(vec2 p, float t) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z + fract(t * 13.37));
}

void main() {
    vec4 baseColor = texture(DiffuseSampler, texCoord);
    
    if (battery <= 0.0) {
        fragColor = baseColor;
        return;
    }

    vec2 center = vec2(0.5, 0.5);
    vec2 pos = texCoord;
    pos.x = (pos.x - 0.5) * AspectRatio + 0.5;
    
    float dist = distance(pos, center);
    
    // Parabolic Reflector Optical Geometry per Mode
    // 0: FOCUSED (Searchlight / Spot - High-power long thrower)
    float hotspotRadius = 0.08;
    float spillRadius = 0.30;
    float cutoffRadius = 0.45;
    float hotspotPower = 0.52;
    float spillPower = 0.08;
    
    if (FlashlightMode > 1.5) {
        // 2: CLOSE / FLOOD MODE (Wide room-clearing wall of light)
        hotspotRadius = 0.26;
        spillRadius = 0.70;
        cutoffRadius = 0.88;
        hotspotPower = 0.24;
        spillPower = 0.18;
    } else if (FlashlightMode > 0.5) {
        // 1: WIDE / BALANCED MODE (Medium-long range duty light)
        hotspotRadius = 0.15;
        spillRadius = 0.50;
        cutoffRadius = 0.68;
        hotspotPower = 0.32;
        spillPower = 0.12;
    }

    // DSP Mode: Precision Micro-Collimator enhancement
    if (u_DSPActive > 0.5) {
        hotspotPower *= 1.30;
        spillPower *= 1.20;
    }
    
    // 1. Hotspot: High-intensity emitter core with subtle orange-peel micro-fluting
    float hotspot = 1.0 - smoothstep(0.0, hotspotRadius, dist);
    float emitterRings = sin(dist * 260.0) * 0.015 * hotspot;
    hotspot = clamp(hotspot + emitterRings, 0.0, 1.0);
    
    // 2. Spill / Corona: Secondary reflector emission
    float spill = 1.0 - smoothstep(hotspotRadius * 0.75, spillRadius, dist);
    
    // 3. Bezel Cutoff with TIR Chromatic Dispersion (Color Fringing)
    float cutoff = 1.0 - smoothstep(spillRadius, cutoffRadius, dist);
    float fringe = smoothstep(spillRadius * 0.92, cutoffRadius, dist) * cutoff;
    
    // Chromatic separation at lens edge: warm interior, cool cyan/blue exterior rim
    vec3 fringeColor = mix(vec3(1.02, 0.97, 0.90), vec3(0.85, 0.96, 1.20), fringe);
    
    // 4. Smooth Atmospheric Forward Scattering (Clean, smooth light without harsh noise dots)
    float beamCore = (hotspot * hotspotPower + spill * spillPower) * cutoff;
    float airHaze = (sin(texCoord.x * 24.0 + time * 0.15) * cos(texCoord.y * 24.0 - time * 0.1) * 0.006) * beamCore;
    float totalBeam = max(0.0, beamCore + airHaze);
    
    // 5. Ambient Back-Scatter (Soft indirect diffuse bounce)
    float ambientBounce = (1.0 - smoothstep(0.0, cutoffRadius * 1.3, dist)) * 0.012 * hotspotPower;
    
    // 6. Dynamic LED Voltage / Thermal Temperature Curve (5500K -> 3200K warm as battery drains)
    vec3 highKelvin = vec3(0.96, 0.98, 1.00); // 5500K crisp tactical daylight
    vec3 lowKelvin  = vec3(1.00, 0.84, 0.58); // 3200K incandescent brownout
    vec3 ledColor = mix(lowKelvin, highKelvin, clamp(battery * 1.6, 0.0, 1.0));
    if (u_DSPActive > 0.5) ledColor = vec3(0.97, 0.99, 1.0); // Regulated constant-current driver
    
    vec3 finalLight = ledColor * fringeColor * (totalBeam * battery) + (ledColor * ambientBounce * battery);
    
    vec3 finalColor = min(baseColor.rgb + finalLight, vec3(1.0));
    fragColor = vec4(finalColor, 1.0);
}
