#version 150

uniform sampler2D EntityMask;
uniform vec4 TintColor;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float alpha = texture(EntityMask, texCoord).a;
    if (alpha < 0.01) discard;
    fragColor = vec4(TintColor.rgb, alpha * TintColor.a);
}
