#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

uniform float AspectRatio;
uniform float FlashlightActive;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    
    if (FlashlightActive > 0.5) {
        vec2 uv = texCoord - 0.5;
        uv.x *= AspectRatio;
        
        float dist = length(uv);
        
        // 1. EL FOCO (Centro intenso)
        float spot = smoothstep(0.12, 0.0, dist);
        
        // 2. LA CORONA (Aro exterior de la linterna)
        // Creamos un aro sutil entre 0.12 y 0.40
        float corona = smoothstep(0.42, 0.10, dist) * 0.4;
        
        // 3. EFECTO DE LENTE (Un pequeño destello en el centro mismo)
        float flare = smoothstep(0.03, 0.0, dist) * 0.5;
        
        // Combinación de luces
        float totalLight = (spot * 2.2) + corona + flare;
        
        // Color de linterna (Blanco frío/xenón)
        vec3 lightColor = vec3(0.9, 0.95, 1.0);
        
        // Aplicar: El haz aclara la imagen y resalta el color natural
        vec3 lit = color.rgb * (1.0 + totalLight * 1.8);
        
        // Añadir un tinte sutil de la luz en las zonas más brillantes del haz
        lit += (spot * 0.15 + corona * 0.05) * lightColor;
        
        color.rgb = lit;
    }
    
    fragColor = color;
}