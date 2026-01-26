#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;
uniform float focus;

void main() {
    vec2 uv = texCoord;

    // No wobble (dry)
    
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);
    
    vec4 baseColor = texture2D(DiffuseSampler, uv);
    
    // Desaturated and blurry look (simulating dry glass masking vision)
    // Blur increases slightly with focus? Or decreases to simulate "focusing"
    float blurOffset = 0.002 + focus * 0.001;
    vec4 blurColor = baseColor;
    blurColor += texture2D(DiffuseSampler, uv + vec2(blurOffset, blurOffset));
    blurColor += texture2D(DiffuseSampler, uv + vec2(-blurOffset, -blurOffset));
    blurColor += texture2D(DiffuseSampler, uv + vec2(blurOffset, -blurOffset));
    blurColor += texture2D(DiffuseSampler, uv + vec2(-blurOffset, blurOffset));
    blurColor /= 5.0;

    vec3 dryColor = blurColor.rgb;
    
    // Slight tint
    dryColor *= vec3(0.9, 0.95, 1.0);

    // Heavy Vignette (Tunnel vision out of water), tightens with focus
    float vignette = smoothstep(0.7 - focus * 0.2, 0.25 - focus * 0.1, dist);
    dryColor *= vignette;

    float globalAlpha = 1.0;
    if (battery < 0.15) {
        // ... (rest of the code)
        if (battery < 0.06) {
            float death = (0.06 - battery) / 0.06;
            float pulse = sin(time * (20.0 + death * 40.0));
            if (pulse > (1.2 - death)) globalAlpha = 0.0;
            if (fract(time * 100.0) < (death * 0.2)) globalAlpha = 0.0;
        }
    }

    gl_FragColor = vec4(dryColor * globalAlpha, 1.0);
}