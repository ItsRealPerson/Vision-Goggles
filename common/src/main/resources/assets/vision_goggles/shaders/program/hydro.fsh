#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform float battery;
uniform float focus;
uniform float u_DSPActive;
uniform float AspectRatio;

void main() {
    vec2 uv = texCoord;

    // Underwater hydro-fluid wobble effect (digitally stabilized in DSP mode)
    float wobbleFactor = (u_DSPActive > 0.5) ? 0.0012 : 0.0045;
    uv.x += sin(uv.y * 32.0 + time * 0.06) * wobbleFactor;
    uv.y += cos(uv.x * 32.0 + time * 0.06) * wobbleFactor;

    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);
    
    // 1. Water Chromatic Refraction (Red wavelengths absorb fast underwater, blue/cyan penetrate)
    float ca = 0.0020;
    float r = texture(DiffuseSampler, uv + vec2(ca, 0.0)).r;
    float g = texture(DiffuseSampler, uv).g;
    float b = texture(DiffuseSampler, uv - vec2(ca, 0.0)).b;
    vec4 baseColor = vec4(r, g, b, 1.0);
    
    float lum = dot(baseColor.rgb, vec3(0.28, 0.60, 0.12));
    
    // 2. High-Tech Abyssal Hydro Palette (Rich Cyan / Deep Marine Blue with contrast boost)
    vec3 deepWater = vec3(lum * 0.08, lum * 0.72, lum * 1.05);
    vec3 hydroColor = mix(baseColor.rgb * 0.35, deepWater, 0.85);
    
    // Brightness boost for crystal-clear navigation
    hydroColor *= (1.25 + focus * 0.35);

    // Battery failure effect
    float globalAlpha = 1.0;
    if (battery < 0.15) {
        float wear = (0.15 - battery) / 0.15;
        hydroColor *= (battery / 0.15);
        
        if (battery < 0.06) {
            float death = (0.06 - battery) / 0.06;
            float pulse = sin(time * (20.0 + death * 40.0));
            if (pulse > (1.2 - death)) globalAlpha = 0.0;
            if (fract(time * 100.0) < (death * 0.2)) globalAlpha = 0.0;
        }
    }

    // 3. Tempered Glass Diving Mask Optical Vignette with Aspect Ratio correction
    vec2 maskPos = vec2((uv.x - 0.5) * AspectRatio, uv.y - 0.5);
    float maskDist = length(maskPos);
    float vignette = smoothstep(0.64 - focus * 0.15, 0.30 - focus * 0.08, maskDist);
    hydroColor *= vignette;

    fragColor = vec4(hydroColor * globalAlpha, 1.0);
}