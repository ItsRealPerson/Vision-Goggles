#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;

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
    hydroColor *= 1.2;

    // Battery failure effect (flicker and darken)
    float globalAlpha = 1.0;
    if (battery < 0.15) {
         hydroColor *= (battery / 0.15); // Dimming
         if (battery < 0.05) {
            if (fract(time * 20.0) < 0.5) globalAlpha = 0.5; // Flicker
         }
    }

    // Heavy vignette for diving mask feel
    float vignette = smoothstep(0.6, 0.3, dist);
    hydroColor *= vignette;

    gl_FragColor = vec4(hydroColor * globalAlpha, 1.0);
}