#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform float battery;
uniform float focus;
uniform float u_DSPActive;

float hashNoise(vec2 p, float t) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z + fract(t * 13.37));
}

float drawBracketReticle(vec2 uv, float f, float t) {
    vec2 p = abs(uv - vec2(0.5, 0.5));
    
    // 1. Center Crosshair (Fine mil-dot cross)
    float cross = 0.0;
    if ((p.x < 0.010 && p.y < 0.0008) || (p.y < 0.010 && p.x < 0.0008)) {
        if (p.x > 0.0025 || p.y > 0.0025) cross = 0.95;
    }
    
    // 2. SDF Corner Brackets [ + ]
    float box = mix(0.075, 0.030, f);
    float arm = 0.018;
    float thick = 0.0012;
    float brackets = 0.0;
    
    // Antialiased horizontal arms
    float hDist = abs(p.y - box);
    if (p.x > (box - arm) && p.x < (box + thick * 2.0)) {
        brackets = max(brackets, 1.0 - smoothstep(0.0, thick, hDist));
    }
    // Antialiased vertical arms
    float vDist = abs(p.x - box);
    if (p.y < (box + thick * 2.0) && p.y > (box - arm)) {
        brackets = max(brackets, 1.0 - smoothstep(0.0, thick, vDist));
    }
    
    // 3. Elevation Pitch Ladder Ticks (Aircraft / HMD tactical ladder)
    float ladder = 0.0;
    float lSpacing = 0.04;
    float lDistY = abs(p.y - lSpacing);
    if (lDistY < thick && p.x > 0.02 && p.x < 0.045) {
        ladder = 1.0 - smoothstep(0.0, thick, lDistY) * 0.75;
    }
    
    return max(cross, max(brackets, ladder));
}

void main() {
    vec2 uv = texCoord;
    
    // Zoom effect: Tighten the view slightly
    if (focus > 0.0) {
        vec2 center = vec2(0.5, 0.5);
        uv = mix(uv, center + (uv - center) * 0.95, focus);
    }

    // Digital Scanline (moving down)
    float scanline = sin(uv.y * 800.0 - time * 10.0) * ((u_DSPActive > 0.5) ? 0.015 : 0.04);
    
    // Scanner bar (laser acquisition bar sweep)
    float scanBar = 0.0;
    float barPos = fract(time * 0.25);
    float barDist = abs(uv.y - barPos);
    if (barDist < 0.025) {
        scanBar = (1.0 - smoothstep(0.0, 0.025, barDist)) * ((u_DSPActive > 0.5) ? 0.15 : 0.22);
    }

    vec4 baseColor = texture(DiffuseSampler, uv);
    
    // High Contrast Tactical Sensor Look
    float gray = dot(baseColor.rgb, vec3(0.299, 0.587, 0.114));
    
    // High-tech amber military HUD palette
    vec3 techColor = vec3(gray * 1.45, gray * 0.92, gray * 0.22); 
    
    // Digital CRT / OLED subpixel micro-grid
    float subpixelMask = 0.96 + 0.04 * sin(uv.x * 1200.0) * sin(uv.y * 1200.0);
    techColor *= subpixelMask;

    // DSP Mode Improvements: SDF Target Acquisition & IFF
    if (u_DSPActive > 0.5) {
        float reticle = drawBracketReticle(uv, focus, time);
        vec3 reticleColor = vec3(0.12, 0.92, 1.00); // Tactical Cyan vector
        
        // IFF (Identify Friend or Foe) System
        float chrominance = max(baseColor.r, max(baseColor.g, baseColor.b)) - min(baseColor.r, min(baseColor.g, baseColor.b));
        float isEntity = clamp(chrominance * 2.8, 0.0, 1.0);
        if (isEntity > 0.35) {
            bool isHostile = (baseColor.g > baseColor.r * 1.15) || (baseColor.r > 0.52 && baseColor.b < 0.38);
            vec3 iffColor = isHostile ? 
                vec3(1.00, 0.12, 0.08) * (0.85 + 0.15 * sin(time * 14.0)) : // Hostile pulsing red
                vec3(0.08, 1.00, 0.45); // Friendly / Neutral emerald green
            
            techColor = mix(techColor, iffColor, 0.72 * isEntity);
        }
        
        techColor += reticleColor * reticle;
    }

    vec3 finalColor = techColor + scanline + scanBar;

    // Tactical HMD Vignette
    vec2 distVec = uv - vec2(0.5, 0.5);
    float dist = length(distVec);
    float vignette = 1.0 - smoothstep(0.42 - focus * 0.1, 0.82 - focus * 0.2, dist);
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

    fragColor = vec4(finalColor * globalAlpha, 1.0);
}
