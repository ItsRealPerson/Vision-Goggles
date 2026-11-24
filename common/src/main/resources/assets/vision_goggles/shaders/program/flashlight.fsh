#version 150

uniform sampler2D DiffuseSampler;
uniform float time;
uniform float battery;
uniform float AspectRatio;
uniform float FlashlightMode;
uniform float u_DSPActive;
uniform float IrisActive;
uniform float BeamDistance;
uniform float StrobeFactor;

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
    
    if (FlashlightMode > 2.5) {
        // 3: STROBE MODE (10 Hz crisp tactical strobe driven by real-time StrobeFactor)
        hotspotRadius = 0.14;
        spillRadius = 0.48;
        cutoffRadius = 0.64;
        
        float strobeMultiplier = (StrobeFactor > 0.001) ? StrobeFactor : 1.0;
        hotspotPower = 0.60 * strobeMultiplier;
        spillPower = 0.16 * strobeMultiplier;
    } else if (FlashlightMode > 1.5) {
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
    
    vec3 finalColor;
    if (IrisActive > 0.5) {
        // When an Iris/Oculus shaderpack is active, world-space ShaderRewriter/SodiumShaderPatcher
        // is bypassed so terrain inside the cone has near-zero darkness RGB.
        // Combine multiplicative shadow-detail lifting (so block textures/edges are revealed in the dark)
        // with distance-attenuated additive beam luminance and soft shoulder compression.
        float maxRange = (FlashlightMode > 2.5) ? 30.0 : ((FlashlightMode > 1.5) ? 18.0 : ((FlashlightMode > 0.5) ? 32.0 : 52.0));
        float distNorm = clamp(BeamDistance / maxRange, 0.0, 1.0);
        float rangeAtten = 1.0 / (1.0 + 0.03 * BeamDistance + 0.004 * BeamDistance * BeamDistance);
        rangeAtten *= 1.0 - smoothstep(0.82, 1.0, distNorm);
        // Minimum 0.28 beam presence so sky/distant air still shows a subtle volumetric cone while nearby surfaces illuminate strongly
        float effectiveAtten = mix(0.28, 1.0, clamp(rangeAtten * 1.35, 0.0, 1.0));

        float beamMask = clamp((totalBeam + ambientBounce) * battery * effectiveAtten, 0.0, 1.0);
        // Shadow gamma lift & exposure boost inside the illuminated cone to reveal dark shaderpack textures
        vec3 liftedBase = pow(max(baseColor.rgb, vec3(0.0)), vec3(mix(1.0, 0.52, clamp(beamMask * 1.4, 0.0, 1.0))));
        vec3 litSurface = liftedBase * (1.0 + beamMask * 5.5) * mix(vec3(1.0), ledColor * fringeColor, clamp(beamMask * 1.2, 0.0, 1.0));
        // Soft volumetric fill so pitch-black shadow pixels still show the beam's optical falloff
        vec3 volumetricFill = finalLight * effectiveAtten * 0.42;
        vec3 combined = max(baseColor.rgb, litSurface) + volumetricFill;
        finalColor = combined / (1.0 + max(vec3(0.0), combined - vec3(0.65)) * 0.55);
        finalColor = clamp(finalColor, 0.0, 1.0);
    } else {
        finalColor = min(baseColor.rgb + finalLight, vec3(1.0));
    }
    fragColor = vec4(finalColor, 1.0);
}
