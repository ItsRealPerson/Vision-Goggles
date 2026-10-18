# Plan de Desarrollo v17 — Rediseño Táctico Integral de los 6 Componentes TEMS (Vanilla+ 16x16)

## 1. Diagnóstico del Feedback del Usuario
A partir de la captura en juego compartida por el usuario (`media_1792357719083_817c7352.png`), los 6 ítems iniciales presentaban fallos de legibilidad visual e identidad temática:
1. **`optical_lens`**: Parecía un óvalo irregular deforme azul/amarillo en lugar de un lente óptico de cámara.
2. **`tactical_frame`**: Parecía un andamio de alambre o marco roto con agujeros negros colgantes en lugar de un armazón balístico frontal.
3. **`flashlight_body`**: Parecía una aguja fina o lápiz negro de 2px en lugar del cuerpo tubular de una linterna táctica.
4. **`tactical_processor`**: Parecía un posavasos o alfombra verde con borde dentado amarillo en lugar de un microchip procesador DSP militar.
5. **`thermal_core`**: Parecía un disco de oro con un borrón naranja sin estructura de sensor infrarrojo criogénico.
6. **`lifesign_sensor`**: Parecía una máscara alienígena con orejas moradas en lugar de un pod telemétrico de signos vitales.

## 2. Rediseño Aplicado (Directrices Argon Vanilla+ 16x16)
Siguiendo la iluminación cenital 45° de Minecraft y las proporciones centradas con márgenes simétricos:

| Ítem | Rediseño Aplicado | Características Visuales Clave |
| :--- | :--- | :--- |
| **`optical_lens`** | Lente circular colimador de 12x12 centrado | Anillo exterior escalonado de latón roscado (`#D4AF37`, `#8A6615`), cuello interior oscuro, cristal óptico zafiro/cian con destello especular puro diagonal (`#FFFFFF`, `#80D8FF`) y halo antirreflejo violeta (`#7E3B8C`). |
| **`tactical_frame`** | Chasis de gafas militares visto frontalmente | Doble cavidad ocular hexagonal hueca con fondo oscuro, puente nasal anatómico, barra de ceja con montura Wilcox dorada (`#FFE073`, `#D4AF37`) y correas balísticas con hebillas de acero plateadas (`#8C96A5`). |
| **`flashlight_body`** | Cuerpo robusto de linterna táctica (4-5px de grosor) | Tubo mecanizado diagonal de aluminio anodizado/polímero oscuro con moleteado antideslizante (*diamond knurling*), clip de bolsillo de acero (`#D6E2EC`), pulsador trasero táctico rojo burdeos (`#B42828`) y cabezal roscado hueco de latón listo para recibir el módulo LED. |
| **`tactical_processor`** | Microprocesador DSP militar cuadrado de 12x12 | Paquete cerámico de pizarra oscura (`#2D333D`), muesca/triángulo dorado "Pin 1" en la esquina superior izquierda (`#FFF0A0`), pines de contacto BGA dorados, disipador térmico niquelado IHS central de 6x6 y die de silicio con matriz cian/cobalto (`#00D2D3`, `#54A0FF`). |
| **`thermal_core`** | Sensor microbolómetro criogénico FLIR | Cápsula central hermética de oro militar con aletas disipadoras de cobre puro en ambos flancos (`#E07A48`, `#7A3014`), puerto criogénico superior, lente frontal circular de Germanio carmesí (`#781622`) y ojo focal térmico ardiente amarillo/blanco (`#FFFCE6`, `#FFE066`, `#F56E0F`). |
| **`lifesign_sensor`** | Pod telemétrico biométrico de pulso vital | Chasis táctico oscuro de 12x14 con electrodos superiores de bioimpedancia (`#E6EEF5`), micro-LED de pulso cardíaco en rojo coral (`#FF3B30`), y pantalla OLED con traza de pulso ECG cardíaco (P-Q-R-S-T) en cian neón luminiscente (`#00FFCC`, `#E0FFF8`). |

## 3. Estado de Ejecución
- [x] Generación y validación pixel-art de los 6 sprites en 16x16.
- [x] Inyección directa a `common/src/main/resources/assets/vision_goggles/textures/item/`.
- [x] Copia de seguridad y versionado en `docs/prototipos_texturas/`.
- [x] Compilación y reempaquetado multi-cargador con subagente (`:common`, `:forge`, `:fabric`).
- [ ] Despliegue de JARs finales en ATLauncher.
- [ ] Actualización cronológica en `docs/CHANGELOG.md` y push a GitHub.
