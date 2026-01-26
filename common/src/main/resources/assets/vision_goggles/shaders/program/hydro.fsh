#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;
uniform float focus;

void main() {
    vec2 uv = texCoord;

    // Underwater wobble effect
    uv.x += sin(uv.y * 30.0 + time * 0.05) * 0.005;
    uv.y += cos(uv.x * 30.0 + time * 0.05) * 0.005;

    // Vignette
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);
    
    vec4 baseColor = texture2D(DiffuseSampler, uv);
    float lum = dot(baseColor.rgb, vec3(0.3, 0.59, 0.11));
    
    // Hydro Color Palette (Cyan/Deep Blue)
    // Deep blue-ish tint mixed with the original luminance
    vec3 hydroColor = vec3(lum * 0.1, lum * 0.8, lum * 1.0);
    
    // Boost brightness slightly
    hydroColor *= (1.2 + focus * 0.3);

    // Battery failure effect (flicker and darken)
    float globalAlpha = 1.0;
    if (battery < 0.15) {
        float wear = (0.15 - battery) / 0.15;
        hydroColor *= (battery / 0.15); // Dimming
        
        // IMPROVED CHAOTIC FLICKER AT < 6%
        if (battery < 0.06) {
            float death = (0.06 - battery) / 0.06;
            float pulse = sin(time * (20.0 + death * 40.0));
            if (pulse > (1.2 - death)) globalAlpha = 0.0;
            if (fract(time * 100.0) < (death * 0.2)) globalAlpha = 0.0;
        }
    }

    // Heavy vignette for diving mask feel, tightens with focus
    float vignette = smoothstep(0.6 - focus * 0.2, 0.3 - focus * 0.1, dist);
    hydroColor *= vignette;

    gl_FragColor = vec4(hydroColor * globalAlpha, 1.0);
}