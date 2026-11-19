-- ============================================================================
-- EJEMPLO 3: VISOR DE COMBATE REACTIVO (SHADER GLSL + MONITOR ECG)
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Controlar uniforms reales del shader 'vision_goggles:shaders/post/nvg.json'
--      ('colorFilter' y 'Glare') mediante onUpdateUniforms(player, uniforms).
--   2. Adaptacion cromatica suave (lerp) segun entorno: Agua, Nether, End,
--      cuevas oscuras (luz <= 2) o vision nocturna estandar.
--   3. Deteccion de delta de dano (hp < lastHp) con destello de impacto,
--      pulso cardiaco doble ('lub-dub') en salud critica y avisos por niveles
--      de bateria baja (20%, 10%, 5%).
--   4. Osciloscopio ECG dibujado en tiempo real con hud.drawLine.
--
-- HARDWARE: Requiere modulo "nvg" (Vision Nocturna) instalado en Gafas Modulares.
-- FLASHEO:  /vg givescript cyber_visor "Visor Combate Reactivo" #00FFAA
-- ============================================================================

local lastHp = nil
local damageFlash = 0.0
local lastFrameTime = nil
local curR, curG, curB = 0.14, 1.00, 0.68
local activeProfile = "TAC-NVG"
local lastBattWarnLevel = 3 -- 3 = >20%, 2 = <=20%, 1 = <=10%, 0 = <=5%

local function clamp(v, lo, hi)
    if v < lo then return lo end
    if v > hi then return hi end
    return v
end

local function lerp(a, b, t)
    return a + (b - a) * t
end

-- Onda fisiologica de doble latido cardiaco (retorna 0.0 a 1.0)
local function cardiacPulse(phase)
    if phase < 0.14 then
        return math.sin((phase / 0.14) * math.pi)
    elseif phase >= 0.22 and phase < 0.36 then
        return math.sin(((phase - 0.22) / 0.14) * math.pi) * 0.72
    end
    return 0.0
end

-- Forma de onda ECG clasica para el monitor del HUD (-1.0 a 1.0)
local function ecgSample(phase)
    if phase >= 0.10 and phase < 0.18 then
        return 0.22 * math.sin(((phase - 0.10) / 0.08) * math.pi) -- Onda P
    elseif phase >= 0.22 and phase < 0.26 then
        return -0.25                                              -- Valle Q
    elseif phase >= 0.26 and phase < 0.32 then
        return 1.0 - math.abs((phase - 0.29) / 0.03) * 1.25       -- Pico R
    elseif phase >= 0.32 and phase < 0.37 then
        return -0.35 * (1.0 - (phase - 0.32) / 0.05)              -- Valle S
    elseif phase >= 0.45 and phase < 0.62 then
        return 0.30 * math.sin(((phase - 0.45) / 0.17) * math.pi) -- Onda T
    end
    return 0.0
end

module = {
    id = "vision_goggles:cyber_visor",
    name = "Visor Combate Reactivo",
    type = "VISION_MODE",
    shader = "vision_goggles:shaders/post/nvg.json",
    required_module = "nvg",
    battery_drain = 0.25,

    onEnable = function(p)
        lastHp = p.getHealth()
        damageFlash = 0.0
        lastFrameTime = getTime()
        lastBattWarnLevel = 3
        sound.playBeep(1.55)
        hud.showAlert("SISTEMA OPTICO ADAPTATIVO EN LINEA", "#00FFAA", 2.0)
    end,

    onDisable = function(p)
        damageFlash = 0.0
        lastHp = nil
        sound.playBeep(0.75)
    end,

    onTick = function(p, batteryPct)
        local hp = p.getHealth()
        if lastHp ~= nil and hp < lastHp - 0.1 then
            damageFlash = 1.0
            sound.playBeep(0.55)
        end
        lastHp = hp

        -- Alertas escalonadas de bateria baja (solo una vez por umbral)
        if batteryPct <= 0.05 and lastBattWarnLevel > 0 then
            lastBattWarnLevel = 0
            sound.playAlert()
            hud.showAlert("BATERIA CRITICA (<5%)", "#FF2222", 2.5)
        elseif batteryPct <= 0.10 and lastBattWarnLevel > 1 then
            lastBattWarnLevel = 1
            sound.playBeep(0.7)
            hud.showAlert("BATERIA MUY BAJA (10%)", "#FF7722", 2.0)
        elseif batteryPct <= 0.20 and lastBattWarnLevel > 2 then
            lastBattWarnLevel = 2
            sound.playBeep(0.95)
        elseif batteryPct > 0.25 then
            lastBattWarnLevel = 3
        end
    end,

    -- Se ejecuta cada frame grafico antes de renderizar la pantalla
    onUpdateUniforms = function(p, uniforms)
        local now = getTime()
        local dt = 0.016
        if lastFrameTime ~= nil then
            dt = clamp(now - lastFrameTime, 0.001, 0.1)
        end
        lastFrameTime = now

        if damageFlash > 0.0 then
            damageFlash = math.max(0.0, damageFlash - dt * 2.4)
        end

        -- 1. Elegir perfil cromatico segun entorno real
        local tr, tg, tb = 0.14, 1.00, 0.68
        local dim = p.getDimension()
        if p.isInWater() then
            tr, tg, tb = 0.12, 0.90, 1.05
            activeProfile = "ABYSS-H2O"
        elseif dim == "the_nether" then
            tr, tg, tb = 1.08, 0.72, 0.20
            activeProfile = "NETHER-AMB"
        elseif dim == "the_end" then
            tr, tg, tb = 0.86, 0.94, 1.08
            activeProfile = "END-P45"
        elseif p.getLightLevel() <= 2 then
            tr, tg, tb = 0.80, 0.96, 1.05
            activeProfile = "CAVE-P45"
        else
            tr, tg, tb = 0.14, 1.00, 0.68
            activeProfile = "TAC-NVG"
        end

        local blend = math.min(1.0, dt * 4.5)
        curR = lerp(curR, tr, blend)
        curG = lerp(curG, tg, blend)
        curB = lerp(curB, tb, blend)

        local r, g, b = curR, curG, curB

        -- 2. Pulso cardiaco de emergencia cuando la vida baja del 35%
        local maxHp = math.max(1.0, p.getMaxHealth())
        local hpPct = clamp(p.getHealth() / maxHp, 0.0, 1.0)
        if hpPct < 0.35 then
            local period = 0.55 + hpPct * 1.2
            local phase = (now % period) / period
            local beat = cardiacPulse(phase) * (1.0 - hpPct / 0.35)
            r = lerp(r, 1.05, beat * 0.80)
            g = lerp(g, 0.14, beat * 0.80)
            b = lerp(b, 0.14, beat * 0.80)
        end

        -- 3. Onda de choque roja + micro-deslumbramiento al recibir dano
        if damageFlash > 0.02 then
            r = lerp(r, 1.15, damageFlash * 0.75)
            g = lerp(g, 0.12, damageFlash * 0.75)
            b = lerp(b, 0.12, damageFlash * 0.75)
            uniforms.setFloat("Glare", damageFlash * 0.35)
        end

        uniforms.setVec3("colorFilter", r, g, b)
    end,

    onRenderHud = function(h)
        local screenW = h.getWidth()
        local screenH = h.getHeight()
        local maxHp = math.max(1.0, player.getMaxHealth())
        local hpPct = clamp(player.getHealth() / maxHp, 0.0, 1.0)
        local now = getTime()

        -- Panel compacto de estado optico en esquina inferior derecha
        local boxW = 94
        local showEcg = (hpPct < 0.99 or damageFlash > 0.05)
        local boxH = showEcg and 36 or 18
        local bx = screenW - boxW - 10
        local by = screenH - boxH - 12

        local accent = hpPct < 0.35 and "#FF3344" or "#00FFAA"
        h.drawRect(bx, by, boxW, boxH, "#AA081016")
        h.drawRect(bx, by, 2, boxH, accent)
        h.drawText(bx + 6, by + 4, "OPT: " .. activeProfile, accent, 0.72)

        -- Osciloscopio ECG animado cuando hay combate o dano
        if showEcg then
            local bpm = math.floor(72 + (1.0 - hpPct) * 85 + damageFlash * 25)
            h.drawText(bx + 6, by + 13, bpm .. " BPM", accent, 0.68)

            local baseX = bx + 44
            local baseY = by + 23
            local samples = 18
            local stepX = 2.5
            local period = 60.0 / math.max(50, bpm)

            local prevX = baseX
            local prevY = baseY
            for i = 1, samples do
                local tOffset = now - (samples - i) * 0.04
                local ph = (tOffset % period) / period
                local amp = ecgSample(ph)
                local px = math.floor(baseX + i * stepX + 0.5)
                local py = math.floor(baseY - amp * 8.0 + 0.5)
                if i > 1 then
                    h.drawLine(prevX, prevY, px, py, accent)
                end
                prevX = px
                prevY = py
            end
        end
    end
}
