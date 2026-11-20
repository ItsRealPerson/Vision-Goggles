-- [VG-BUNDLED v36]
-- ============================================================================
-- EJEMPLO 3: VISOR DE COMBATE GEN-3 (AUTO-GATING DSP + PROYECTILES + FLANCOS)
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Activar el modo DSP real ('u_DSPActive = 1.0') en nvg.fsh para reducir
--      el grano analogico un 75% y habilitar Smart Auto-Gating anti-ceguera.
--   2. Detectar proyectiles entrantes con world.getProjectilesInRadius(24.0)
--      (flechas, bolas de fuego, proyectiles de shulker) en tiempo real.
--   3. Dibujar un anillo direccional 360° alrededor de la mira marcando
--      flancos hostiles y proyectiles entrantes con hud.drawLine.
--   4. Vigilar la durabilidad minima de la armadura (player.getArmorDurabilityPct)
--      y mostrar telemetria de dano recibido.
--
-- HARDWARE: Requiere modulo "nvg" (Vision Nocturna) instalado en Gafas Modulares.
-- FLASHEO:  /vg givescript cyber_visor "Visor Combate Gen-3" #00FFAA
-- ============================================================================

local threatArcs = {}
local projArcs = {}
local lastHp = nil
local lastDamageAmt = 0.0
local damageTimer = 0.0
local damageFlash = 0.0
local lastFrameTime = nil
local curR, curG, curB = 0.16, 1.00, 0.72
local activeProfile = "GEN3-TAC"
local lastProjAlert = 0
local armorWarned = false
local lastBattWarnLevel = 3

local CRITICAL_MOBS = {
    ["minecraft:creeper"] = true,
    ["minecraft:warden"] = true,
    ["minecraft:wither"] = true,
    ["minecraft:blaze"] = true,
    ["minecraft:ravager"] = true
}

local function clamp(v, lo, hi)
    if v < lo then return lo end
    if v > hi then return hi end
    return v
end

local function lerp(a, b, t)
    return a + (b - a) * t
end

module = {
    id = "vision_goggles:cyber_visor",
    name = "Visor Combate Gen-3",
    type = "VISION_MODE",
    shader = "vision_goggles:shaders/post/nvg.json",
    required_module = "nvg",
    battery_drain = 0.20,

    onEnable = function(p)
        threatArcs = {}
        projArcs = {}
        lastHp = p.getHealth()
        lastDamageAmt = 0.0
        damageTimer = 0.0
        damageFlash = 0.0
        lastFrameTime = getTime()
        lastProjAlert = 0
        armorWarned = false
        lastBattWarnLevel = 3
        sound.playBeep(1.55)
        hud.showAlert("OPTICA GEN-3 AUTO-GATE ACTIVA", "#00FFAA", 1.8)
    end,

    onDisable = function(p)
        threatArcs = {}
        projArcs = {}
        damageFlash = 0.0
        lastHp = nil
        sound.playBeep(0.75)
    end,

    onTick = function(p, batteryPct)
        local now = getTime()

        -- 1. Deteccion de dano recibido
        local hp = p.getHealth()
        if lastHp ~= nil and hp < lastHp - 0.25 then
            lastDamageAmt = lastHp - hp
            damageTimer = 2.4
            damageFlash = 1.0
            sound.playBeep(0.55)
        end
        lastHp = hp
        if damageTimer > 0.0 then
            damageTimer = math.max(0.0, damageTimer - 0.05)
        end

        -- 2. Escanear amenazas hostiles en 18m para el anillo alrededor de la mira
        local nearby = world.getEntitiesInRadius(18.0)
        local tList = {}
        if nearby ~= nil then
            for i = 1, #nearby do
                local e = nearby[i]
                local isCrit = CRITICAL_MOBS[e.type] == true
                if e.is_hostile or isCrit then
                    tList[#tList + 1] = {
                        x = e.x,
                        z = e.z,
                        dist = e.distance,
                        urgent = (isCrit or e.distance <= 7.0)
                    }
                end
            end
            table.sort(tList, function(a, b)
                if a.urgent ~= b.urgent then
                    return a.urgent
                end
                return a.dist < b.dist
            end)
        end
        local trimmedThreats = {}
        for i = 1, math.min(#tList, 8) do
            trimmedThreats[i] = tList[i]
        end
        threatArcs = trimmedThreats

        -- 3. Detector de proyectiles entrantes en tiempo real (flechas, fireballs, tridentes)
        local projs = world.getProjectilesInRadius(24.0)
        local pList = {}
        local closestProj = nil
        if projs ~= nil then
            for i = 1, #projs do
                local pr = projs[i]
                if pr.approaching then
                    pList[#pList + 1] = {
                        x = pr.x,
                        z = pr.z,
                        dist = pr.distance
                    }
                    if closestProj == nil or pr.distance < closestProj then
                        closestProj = pr.distance
                    end
                end
            end
            table.sort(pList, function(a, b) return a.dist < b.dist end)
        end
        local trimmedProj = {}
        for i = 1, math.min(#pList, 4) do
            trimmedProj[i] = pList[i]
        end
        projArcs = trimmedProj

        if closestProj ~= nil and (now - lastProjAlert) >= 1.8 then
            sound.playAlert()
            hud.showAlert(string.format("¡PROYECTIL ENTRANTE! (%.0fm)", closestProj), "#FFCC00", 1.1)
            lastProjAlert = now
        end

        -- 4. Alerta de durabilidad de armadura critica (<25%)
        local armDur = p.getArmorDurabilityPct()
        if armDur >= 0.0 and armDur < 0.25 and not armorWarned then
            armorWarned = true
            sound.playBeep(0.65)
            hud.showAlert(string.format("¡ARMADURA CRITICA: %.0f%%!", armDur * 100.0), "#FF6633", 2.0)
        elseif armDur >= 0.35 or armDur < 0.0 then
            armorWarned = false
        end

        -- 5. Avisos escalonados de bateria baja
        if batteryPct <= 0.05 and lastBattWarnLevel > 0 then
            lastBattWarnLevel = 0
            sound.playAlert()
            hud.showAlert("BATERIA CRITICA (<5%)", "#FF2222", 2.5)
        elseif batteryPct <= 0.10 and lastBattWarnLevel > 1 then
            lastBattWarnLevel = 1
            sound.playBeep(0.70)
        elseif batteryPct > 0.18 then
            lastBattWarnLevel = 3
        end
    end,

    onUpdateUniforms = function(p, uniforms)
        local now = getTime()
        local dt = 0.016
        if lastFrameTime ~= nil then
            dt = clamp(now - lastFrameTime, 0.001, 0.1)
        end
        lastFrameTime = now

        if damageFlash > 0.0 then
            damageFlash = math.max(0.0, damageFlash - dt * 2.6)
        end

        -- Habilita DSP Auto-Gating y reduccion de ruido digital (75% menos grano) en nvg.fsh
        uniforms.setFloat("u_DSPActive", 1.0)

        local tr, tg, tb = 0.16, 1.00, 0.72
        local dim = p.getDimension()
        if p.isInWater() then
            tr, tg, tb = 0.12, 0.92, 1.06
            activeProfile = "ABYSS-H2O"
        elseif dim == "the_nether" then
            tr, tg, tb = 1.06, 0.74, 0.22
            activeProfile = "NETHER-AMB"
        elseif p.getLightLevel() <= 2 then
            tr, tg, tb = 0.84, 0.96, 1.08
            activeProfile = "CAVE-P45"
        else
            tr, tg, tb = 0.16, 1.00, 0.72
            activeProfile = "GEN3-TAC"
        end

        local blend = math.min(1.0, dt * 5.0)
        curR = lerp(curR, tr, blend)
        curG = lerp(curG, tg, blend)
        curB = lerp(curB, tb, blend)

        local r, g, b = curR, curG, curB
        local maxHp = math.max(1.0, p.getMaxHealth())
        local hpPct = clamp(p.getHealth() / maxHp, 0.0, 1.0)
        if hpPct < 0.35 then
            local pulse = (math.sin(now * 7.5) + 1.0) * 0.5
            local weight = (1.0 - hpPct / 0.35) * pulse * 0.45
            r = lerp(r, 1.05, weight)
            g = lerp(g, 0.18, weight)
            b = lerp(b, 0.18, weight)
        end
        if damageFlash > 0.02 then
            r = lerp(r, 1.12, damageFlash * 0.65)
            g = lerp(g, 0.15, damageFlash * 0.65)
            b = lerp(b, 0.15, damageFlash * 0.65)
        end

        uniforms.setVec3("colorFilter", r, g, b)
    end,

    onRenderHud = function(h)
        local screenW = h.getWidth()
        local screenH = h.getHeight()
        local midX = math.floor(screenW / 2)
        local midY = math.floor(screenH / 2)
        local now = getTime()

        -- 1. Anillo direccional 360° alrededor de la mira para amenazas y proyectiles
        if #threatArcs > 0 or #projArcs > 0 then
            local px = player.getX()
            local pz = player.getZ()
            local yawRad = math.rad(player.getYaw())
            local fx = -math.sin(yawRad)
            local fz = math.cos(yawRad)
            local rx = -math.cos(yawRad)
            local rz = -math.sin(yawRad)

            for i = 1, #threatArcs do
                local t = threatArcs[i]
                local dx = t.x - px
                local dz = t.z - pz
                local fwd = dx * fx + dz * fz
                local right = dx * rx + dz * rz
                local mag = math.sqrt(fwd * fwd + right * right)
                if mag > 0.2 then
                    local ux = right / mag
                    local uy = -fwd / mag
                    local perpX = -uy
                    local perpY = ux
                    local col = t.urgent and "#EEFF2244" or "#CCFF7722"
                    local rIn = 21.0
                    local rOut = t.urgent and 28.0 or 26.0
                    local x1 = math.floor(midX + ux * rIn + 0.5)
                    local y1 = math.floor(midY + uy * rIn + 0.5)
                    local x2 = math.floor(midX + ux * rOut + 0.5)
                    local y2 = math.floor(midY + uy * rOut + 0.5)
                    h.drawLine(x1, y1, x2, y2, col)
                    local wx1 = math.floor(midX + ux * (rIn + 2.0) + perpX * 2.5 + 0.5)
                    local wy1 = math.floor(midY + uy * (rIn + 2.0) + perpY * 2.5 + 0.5)
                    local wx2 = math.floor(midX + ux * (rIn + 2.0) - perpX * 2.5 + 0.5)
                    local wy2 = math.floor(midY + uy * (rIn + 2.0) - perpY * 2.5 + 0.5)
                    h.drawLine(wx1, wy1, x2, y2, col)
                    h.drawLine(wx2, wy2, x2, y2, col)
                end
            end

            local blink = math.sin(now * 16.0) > -0.2
            if blink then
                for i = 1, #projArcs do
                    local pr = projArcs[i]
                    local dx = pr.x - px
                    local dz = pr.z - pz
                    local fwd = dx * fx + dz * fz
                    local right = dx * rx + dz * rz
                    local mag = math.sqrt(fwd * fwd + right * right)
                    if mag > 0.2 then
                        local ux = right / mag
                        local uy = -fwd / mag
                        local perpX = -uy
                        local perpY = ux
                        local rTip = 33.0
                        local rBase = 29.0
                        local tx = math.floor(midX + ux * rTip + 0.5)
                        local ty = math.floor(midY + uy * rTip + 0.5)
                        local bx1 = math.floor(midX + ux * rBase + perpX * 3.5 + 0.5)
                        local by1 = math.floor(midY + uy * rBase + perpY * 3.5 + 0.5)
                        local bx2 = math.floor(midX + ux * rBase - perpX * 3.5 + 0.5)
                        local by2 = math.floor(midY + uy * rBase - perpY * 3.5 + 0.5)
                        h.drawLine(bx1, by1, tx, ty, "#FFEE22")
                        h.drawLine(bx2, by2, tx, ty, "#FFEE22")
                    end
                end
            end
        end

        -- 2. Panel tactico en esquina inferior derecha encima de 'MODO:' (sin solapar bateria)
        local armPct = player.getArmorDurabilityPct()
        local showDmg = damageTimer > 0.0 and lastDamageAmt > 0.2
        local showArm = armPct >= 0.0 and armPct < 0.50
        local lines = 2 + (showDmg and 1 or 0) + (showArm and 1 or 0)
        local boxW = 100
        local boxH = 8 + lines * 9
        local bx = screenW - boxW - 8
        local by = screenH - boxH - 22

        local maxHp = math.max(1.0, player.getMaxHealth())
        local hpPct = clamp(player.getHealth() / maxHp, 0.0, 1.0)
        local accent = (#projArcs > 0) and "#FFDD22" or (hpPct < 0.35 and "#FF3344" or "#00FFAA")

        h.drawRect(bx, by, boxW, boxH, "#AA071016")
        h.drawRect(bx + boxW - 2, by, 2, boxH, accent)

        local yCursor = by + 4
        h.drawText(bx + 5, yCursor, "DSP: " .. activeProfile, accent, 0.68)
        yCursor = yCursor + 9

        local thrCol = #threatArcs > 0 and "#FF7733" or "#88FFBB"
        local projTag = #projArcs > 0 and string.format(" PRJ:%d", #projArcs) or ""
        h.drawText(bx + 5, yCursor, string.format("AMEN:%d%s", #threatArcs, projTag), thrCol, 0.68)
        yCursor = yCursor + 9

        if showArm then
            local aCol = armPct < 0.25 and "#FF3344" or "#FFAA33"
            h.drawText(bx + 5, yCursor, string.format("ARM MIN: %.0f%%", armPct * 100.0), aCol, 0.65)
            yCursor = yCursor + 9
        end
        if showDmg then
            h.drawText(bx + 5, yCursor, string.format("IMPACTO: -%.1f HP", lastDamageAmt), "#FF4455", 0.65)
        end
    end
}
