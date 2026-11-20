-- [VG-BUNDLED v37]
-- ============================================================================
-- EJEMPLO 4: HUD DE PILOTO Y AVIONICA (BRUJULA DESLIZANTE, VSI, GPWS, RELOJ)
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Brujula deslizante superior con conversion real de Yaw en Minecraft
--      (bearing = (yaw + 180) % 360 donde 0° = Norte, 90° = Este).
--   2. Calculo de velocidad vertical VSI (dy/dt) y alarma GPWS "PULL UP!" al
--      caer a gran velocidad usando player.isFlying() (creativo y Elytras).
--   3. Reloj solar 24h desde world.getTimeOfDay() y estado climatico.
--   4. Lectura de efectos de pocion activos con player.getEffects().
--
-- HARDWARE: Autonomo (funciona en cualquier Gafa Modular sin modulo previo).
-- FLASHEO:  /vg givescript tactical_hud "HUD Piloto Avionica" #00FFCC
-- ============================================================================

local lastY = nil
local lastTime = nil
local smoothedVsi = 0.0
local lastGpwsAlert = 0

local COMPASS_MARKS = {
    { deg = 0,   label = "N"  },
    { deg = 45,  label = "NE" },
    { deg = 90,  label = "E"  },
    { deg = 135, label = "SE" },
    { deg = 180, label = "S"  },
    { deg = 225, label = "SW" },
    { deg = 270, label = "W"  },
    { deg = 315, label = "NW" }
}

local function clamp(v, lo, hi)
    if v < lo then return lo end
    if v > hi then return hi end
    return v
end

local function formatEffectName(rawId)
    local name = rawId
    local colon = string.find(name, ":", 1, true)
    if colon ~= nil then
        name = string.sub(name, colon + 1)
    end
    name = string.gsub(name, "_", " ")
    if #name > 12 then
        name = string.sub(name, 1, 11) .. "."
    end
    return string.upper(name)
end

local function getStratum(y)
    if y >= 190 then return "ESTRATO: CIELO" end
    if y >= 55  then return "ESTRATO: SUPERFICIE" end
    if y >= 0   then return "ESTRATO: CUEVAS" end
    if y >= -45 then return "ESTRATO: PIZARRA" end
    return "ESTRATO: DIAMANTE"
end

module = {
    id = "vision_goggles:tactical_hud",
    name = "HUD Piloto y Avionica",
    type = "HUD",

    onEnable = function(p)
        lastY = p.getY()
        lastTime = getTime()
        smoothedVsi = 0.0
        sound.playBeep(1.25)
    end,

    onTick = function(p, batteryPct)
        local now = getTime()
        local y = p.getY()
        if lastY ~= nil and lastTime ~= nil then
            local dt = now - lastTime
            if dt >= 0.04 then
                local rawVsi = (y - lastY) / dt
                smoothedVsi = smoothedVsi + (rawVsi - smoothedVsi) * 0.35
                lastY = y
                lastTime = now
            end
        else
            lastY = y
            lastTime = now
        end

        -- Alarma GPWS de descenso peligroso cuando esta en el aire sin agua
        if not p.onGround() and not p.isInWater() and smoothedVsi < -12.0 then
            if (now - lastGpwsAlert) >= 2.2 then
                sound.playAlert()
                hud.showAlert("¡PULL UP! DESCENSO RAPIDO", "#FF2222", 1.5)
                lastGpwsAlert = now
            end
        end
    end,

    onRenderHud = function(h)
        local w = h.getWidth()
        local screenH = h.getHeight()
        local midX = math.floor(w / 2)
        local midY = math.floor(screenH / 2)

        -- ====================================================================
        -- 1. CINTA DE BRUJULA DESLIZANTE SUPERIOR (000° = Norte)
        -- ====================================================================
        local bearing = ((player.getYaw() + 180.0) % 360.0 + 360.0) % 360.0
        local tapeW = 140
        local halfTape = 70
        local pxPerDeg = halfTape / 55.0

        h.drawRect(midX - halfTape, 8, tapeW, 16, "#99070D14")
        h.drawLine(midX - halfTape, 24, midX + halfTape, 24, "#6600FFCC")

        -- Ticks cada 15°
        for deg = 0, 345, 15 do
            local diff = ((deg - bearing + 540.0) % 360.0) - 180.0
            if math.abs(diff) <= 52.0 then
                local tx = math.floor(midX + diff * pxPerDeg + 0.5)
                local hTick = (deg % 45 == 0) and 5 or 3
                h.drawLine(tx, 24 - hTick, tx, 24, "#8800FFCC")
            end
        end

        -- Etiquetas cardinales e intercardinales
        for i = 1, #COMPASS_MARKS do
            local mark = COMPASS_MARKS[i]
            local diff = ((mark.deg - bearing + 540.0) % 360.0) - 180.0
            if math.abs(diff) <= 48.0 then
                local lx = math.floor(midX + diff * pxPerDeg - h.getTextWidth(mark.label, 0.72) / 2 + 0.5)
                local col = (mark.deg == 0) and "#FFCC33" or "#CCFFEE"
                h.drawText(lx, 11, mark.label, col, 0.72)
            end
        end

        -- Caja central del rumbo numerico
        local hdgStr = string.format("%03d", math.floor(bearing + 0.5) % 360)
        h.drawRect(midX - 15, 25, 30, 11, "#CC0A1622")
        h.drawLine(midX, 23, midX, 26, "#00FFCC")
        h.drawText(midX - 10, 27, hdgStr, "#00FFCC", 0.78)

        -- ====================================================================
        -- 2. ESCALERA DE CABECEO (PITCH LADDER) EN VUELO O AIRE
        -- ====================================================================
        local isAirborne = player.isFlying() or (not player.onGround() and not player.isInWater())
        if isAirborne then
            local pitch = player.getPitch()
            local horizonY = midY - math.floor(pitch * 0.9)
            if horizonY > midY - 60 and horizonY < midY + 60 then
                local hCol = smoothedVsi < -12.0 and "#AAFF3333" or "#6600FFAA"
                h.drawLine(midX - 42, horizonY, midX - 20, horizonY, hCol)
                h.drawLine(midX + 20, horizonY, midX + 42, horizonY, hCol)
            end
        end

        -- ====================================================================
        -- 3. CINTA IZQUIERDA: VELOCIDAD, VSI Y ESTADO DE MOVIMIENTO
        -- ====================================================================
        local spd = player.getSpeed()
        local stateTag = "[PIE]"
        local stateCol = "#88FFBB"
        if player.isFlying() then
            stateTag = "[VUELO]"
            stateCol = "#00EEFF"
        elseif player.isInWater() then
            stateTag = "[AGUA]"
            stateCol = "#33CCFF"
        elseif not player.onGround() and smoothedVsi < -6.0 then
            stateTag = "[CAIDA]"
            stateCol = "#FF5544"
        elseif player.isSprinting() then
            stateTag = "[SPRINT]"
            stateCol = "#FFCC44"
        elseif player.isCrouching() then
            stateTag = "[SIGILO]"
            stateCol = "#AAAAAA"
        end

        local leftY = midY - 26
        h.drawRect(8, leftY, 84, 34, "#88070D14")
        h.drawRect(8, leftY, 2, 34, stateCol)
        h.drawText(14, leftY + 4, stateTag, stateCol, 0.75)
        h.drawText(14, leftY + 14, string.format("SPD: %.1f m/s", spd), "#EEEEEE", 0.72)
        local vsiCol = smoothedVsi < -12.0 and "#FF3344" or "#AAAAAA"
        h.drawText(14, leftY + 23, string.format("VSI: %+.1f m/s", smoothedVsi), vsiCol, 0.70)

        -- ====================================================================
        -- 4. CINTA DERECHA: ALTITUD, ESTRATO, RELOJ 24H Y CLIMA
        -- ====================================================================
        local yBlock = math.floor(player.getY())
        local tod = world.getTimeOfDay()
        local totalMinutes = math.floor(((tod + 6000) % 24000) * (1440.0 / 24000.0))
        local hh = math.floor(totalMinutes / 60)
        local mm = totalMinutes % 60
        local weatherStr = "DESPEJADO"
        local weatherCol = "#88FFBB"
        if world.isThundering() then
            weatherStr = "TORMENTA"
            weatherCol = "#FF6644"
        elseif world.isRaining() then
            weatherStr = "LLUVIA"
            weatherCol = "#55CCFF"
        end

        local rx = w - 98
        h.drawRect(rx, leftY, 90, 42, "#88070D14")
        h.drawRect(rx + 88, leftY, 2, 42, "#00FFCC")
        h.drawText(rx + 5, leftY + 4, string.format("ALT: %dm", yBlock), "#00FFCC", 0.75)
        h.drawText(rx + 5, leftY + 13, getStratum(yBlock), "#AAAAAA", 0.65)
        h.drawText(rx + 5, leftY + 22, string.format("HORA: %02d:%02d", hh, mm), "#EEEEEE", 0.70)
        h.drawText(rx + 5, leftY + 31, weatherStr, weatherCol, 0.68)

        -- ====================================================================
        -- 5. OXIGENO BAJO EL AGUA Y EFECTOS DE POCION ACTIVOS
        -- ====================================================================
        local oxy = player.getOxygen()
        if player.isInWater() or oxy < 99.5 then
            local oCol = oxy < 25.0 and "#FF3333" or "#33DDFF"
            h.drawText(14, leftY + 40, string.format("O2: %.0f%%", oxy), oCol, 0.72)
            h.drawProgressBar(14, leftY + 49, 72, 4, clamp(oxy / 100.0, 0.0, 1.0), oCol)
        end

        local effects = player.getEffects()
        if effects ~= nil and #effects > 0 then
            local shown = math.min(#effects, 4)
            local ey = leftY + 48
            for i = 1, shown do
                local eff = effects[i]
                local eCol = eff.is_beneficial and "#66FF99" or "#FF5555"
                local timeStr = "INF"
                if eff.duration >= 0 then
                    local secs = math.floor(eff.duration / 20)
                    local eMin = math.floor(secs / 60)
                    local eSec = secs % 60
                    timeStr = string.format("%d:%02d", eMin, eSec)
                end
                local line = string.format("%s %d (%s)", formatEffectName(eff.id), eff.amplifier + 1, timeStr)
                h.drawText(rx + 2, ey + (i - 1) * 9, line, eCol, 0.65)
            end
        end
    end
}
