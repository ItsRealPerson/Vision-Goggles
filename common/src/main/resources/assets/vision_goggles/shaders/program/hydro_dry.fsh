#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform float battery;
uniform float focus;
uniform float u_DSPActive;

void main() {
    vec2 uv = texCoord;

    vec2 center = vec2(0.5, 0.5);
    vec2 maskPos = (uv - center);
    maskPos.x *= 1.15;
    float dist = length(maskPos);
    
    // 1. Procedural Condensation Droplets running down dry lens
    float dropPattern = sin(uv.x * 140.0) * sin(uv.y * 100.0 + time * 0.15);
    float drops = step(0.88, dropPattern) * 0.0035;
    vec2 dropUV = uv + vec2(drops, drops * 1.5);
    
    vec4 baseColor = texture(DiffuseSampler, dropUV);
    
    // 2. Optical Glass Fogging / Blur (simulating moisture-fogged tempered glass)
    float blurOffset = (u_DSPActive > 0.5) ? (0.0007 + focus * 0.0004) : (0.0018 + focus * 0.0008);
    vec4 blurColor = baseColor;
    blurColor += texture(DiffuseSampler, dropUV + vec2(blurOffset, blurOffset));
    blurColor += texture(DiffuseSampler, dropUV + vec2(-blurOffset, -blurOffset));
    blurColor += texture(DiffuseSampler, dropUV + vec2(blurOffset, -blurOffset));
    blurColor += texture(DiffuseSampler, dropUV + vec2(-blurOffset, blurOffset));
    blurColor /= 5.0;

    vec3 dryColor = blurColor.rgb;
    
    // Cool moisture tint with slight edge chromatic aberration
    dryColor *= vec3(0.92, 0.96, 1.00);

    // 3. Anatomical Diving Mask Peripheral Vignette
    float vignette = smoothstep(0.68 - focus * 0.18, 0.28 - focus * 0.09, dist);
    dryColor *= vignette;

    float globalAlpha = 1.0;
    if (battery < 0.15) {
        if (battery < 0.06) {
            float death = (0.06 - battery) / 0.06;
            float pulse = sin(time * (20.0 + death * 40.0));
            if (pulse > (1.2 - death)) globalAlpha = 0.0;
            if (fract(time * 100.0) < (death * 0.2)) globalAlpha = 0.0;
        }
    }

    fragColor = vec4(dryColor * globalAlpha, 1.0);
}