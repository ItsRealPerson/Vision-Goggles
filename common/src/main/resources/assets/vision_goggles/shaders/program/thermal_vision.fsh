#version 150

uniform sampler2D EntityMask;
uniform vec4 TintColor;
uniform float UseVertexColor;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 sampleCol = texture(EntityMask, texCoord);
    float alpha = sampleCol.a;
    if (alpha < 0.01) discard;
    vec3 rgb = (UseVertexColor > 0.5) ? sampleCol.rgb : TintColor.rgb;
    fragColor = vec4(rgb, alpha * TintColor.a);
}
