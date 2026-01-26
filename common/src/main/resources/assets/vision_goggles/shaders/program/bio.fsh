#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float time;
uniform float battery;

void main() {
    vec2 uv = texCoord;

    // Digital Scanline (moving down)
    float scanline = sin(uv.y * 800.0 - time * 10.0) * 0.05;
    
    // Scanner bar (bright line moving down)
    float scanBar = 0.0;
    float barPos = fract(time * 0.2);
    if (abs(uv.y - barPos) < 0.02) {
        scanBar = 0.2;
    }

    vec4 baseColor = texture2D(DiffuseSampler, uv);
    
    // High Contrast Look
    float gray = dot(baseColor.rgb, vec3(0.299, 0.587, 0.114));
    
    // Edge detectionish effect (simplified high pass)
    // Actually let's just do a high-tech amber look
    vec3 techColor = vec3(gray * 1.5, gray * 0.9, gray * 0.2); // Amber/Gold
    
    // Add grid overlay
    float grid = 0.0;
    if (mod(uv.x * 50.0, 1.0) < 0.05 || mod(uv.y * 30.0, 1.0) < 0.05) {
        grid = 0.1;
    }

    vec3 finalColor = techColor + scanline + scanBar + grid;

    // Battery logic
    float globalAlpha = 1.0;
    if (battery < 0.1) {
        finalColor *= 0.5; // Dim
        if (fract(time * 5.0) < 0.2) globalAlpha = 0.8; // Glitch
    }

    gl_FragColor = vec4(finalColor * globalAlpha, 1.0);
}