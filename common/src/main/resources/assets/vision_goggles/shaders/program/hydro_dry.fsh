#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float battery;

void main() {
    vec2 uv = texCoord;

    // No wobble (dry)
    
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);
    
    vec4 baseColor = texture2D(DiffuseSampler, uv);
    
    // Desaturated and blurry look (simulating dry glass masking vision)
    // Simple blur approximation by sampling nearby
    vec4 blurColor = baseColor;
    blurColor += texture2D(DiffuseSampler, uv + vec2(0.002, 0.002));
    blurColor += texture2D(DiffuseSampler, uv + vec2(-0.002, -0.002));
    blurColor += texture2D(DiffuseSampler, uv + vec2(0.002, -0.002));
    blurColor += texture2D(DiffuseSampler, uv + vec2(-0.002, 0.002));
    blurColor /= 5.0;

    vec3 dryColor = blurColor.rgb;
    
    // Slight tint
    dryColor *= vec3(0.9, 0.95, 1.0);

    // Heavy Vignette (Tunnel vision out of water)
    float vignette = smoothstep(0.7, 0.25, dist);
    dryColor *= vignette;

    float globalAlpha = 1.0;
    if (battery < 0.1) globalAlpha = 0.5;

    gl_FragColor = vec4(dryColor * globalAlpha, 1.0);
}