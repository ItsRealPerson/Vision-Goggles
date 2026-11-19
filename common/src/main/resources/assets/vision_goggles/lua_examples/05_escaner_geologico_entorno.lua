-- ============================================================================
-- EJEMPLO 5: ESCANER GEOLOGICO Y ENTORNO (TIME-SLICING + DETECTOR AR)
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Tecnica de Time-Slicing (particionado por ticks) usando world.getBlock(x,y,z)
--      para escanear 567 bloques (9x7x9) sin superar el limite de 100,000
--      instrucciones por invocacion (81 bloques/tick en 7 capas).
--   2. Analisis de bloque en mira con world.raycastBlock(24) (nombre, luz, cielo).
--   3. Marcadores AR 3D sobre menas valiosas con world.worldToScreen(x, y, z)
--      y alerta inmediata de lava bajo los pies.
--   4. Lectura ambiental con player.getBiome() y player.getLightLevel().
--
-- HARDWARE: Autonomo (funciona en cualquier Gafa Modular sin modulo previo).
-- FLASHEO:  /vg givescript geo_scanner "Escaner Geologico" #33DDFF
-- ============================================================================

local SCAN_RADIUS = 4
local Y_OFFSETS = { -3, -2, -1, 0, 1, 2, 3 }

local VALUABLE_BLOCKS = {
    ["minecraft:diamond_ore"]           = { label = "DIAMANTE", color = "#33FFFF", prio = 4 },
    ["minecraft:deepslate_diamond_ore"] = { label = "DIAMANTE", color = "#33FFFF", prio = 4 },
    ["minecraft:ancient_debris"]        = { label = "ESCOMBROS", color = "#FFAA33", prio = 5 },
    ["minecraft:emerald_ore"]           = { label = "ESMERALDA", color = "#33FF77", prio = 3 },
    ["minecraft:deepslate_emerald_ore"] = { label = "ESMERALDA", color = "#33FF77", prio = 3 },
    ["minecraft:gold_ore"]              = { label = "ORO", color = "#FFDD33", prio = 2 },
    ["minecraft:deepslate_gold_ore"]    = { label = "ORO", color = "#FFDD33", prio = 2 },
    ["minecraft:spawner"]               = { label = "SPAWNER", color = "#FF3366", prio = 5 }
}

local sliceIdx = 1
local anchorX, anchorY, anchorZ = 0, 0, 0
local pendingOres = {}
local detectedOres = {}
local lastLavaAlert = 0
local lastGeigerTime = 0
local lastPingedKey = nil

local function formatId(rawId)
    if rawId == nil or rawId == "" then return "DESCONOCIDO" end
    local s = rawId
    local colon = string.find(s, ":", 1, true)
    if colon ~= nil then
        s = string.sub(s, colon + 1)
    end
    s = string.gsub(s, "_", " ")
    if #s > 18 then
        s = string.sub(s, 1, 17) .. "."
    end
    return string.upper(s)
end

local function classifyBlock(blockId)
    if VALUABLE_BLOCKS[blockId] ~= nil then
        return "[VALIOSO]", VALUABLE_BLOCKS[blockId].color
    end
    if string.find(blockId, "lava", 1, true) or string.find(blockId, "magma", 1, true) or string.find(blockId, "tnt", 1, true) then
        return "[PELIGRO]", "#FF4433"
    end
    if string.find(blockId, "_ore", 1, true) or string.find(blockId, "redstone", 1, true) then
        return "[MINERAL]", "#FFCC44"
    end
    return "[BLOQUE]", "#99CCDD"
end

module = {
    id = "vision_goggles:geo_scanner",
    name = "Escaner Geologico",
    type = "UTILITY",
    battery_drain = 0.05,

    onEnable = function(p)
        sliceIdx = 1
        pendingOres = {}
        detectedOres = {}
        lastPingedKey = nil
        sound.playBeep(1.45)
    end,

    onDisable = function(p)
        pendingOres = {}
        detectedOres = {}
    end,

    onTick = function(p, batteryPct)
        local now = getTime()
        local px = math.floor(p.getX())
        local py = math.floor(p.getY())
        local pz = math.floor(p.getZ())

        -- 1. Seguridad inmediata bajo los pies (solo 2 consultas por tick)
        local b1 = world.getBlock(px, py - 1, pz)
        local b2 = world.getBlock(px, py - 2, pz)
        if b1 == "minecraft:lava" or b2 == "minecraft:lava" then
            if (now - lastLavaAlert) >= 4.0 then
                sound.playAlert()
                hud.showAlert("¡CUIDADO: LAVA BAJO TUS PIES!", "#FF5500", 2.0)
                lastLavaAlert = now
            end
        end

        -- 2. Barrido particionado por capas Y (81 bloques por tick -> 7 ticks por ciclo)
        if sliceIdx == 1 then
            anchorX, anchorY, anchorZ = px, py, pz
            pendingOres = {}
        end

        local dy = Y_OFFSETS[sliceIdx]
        local scanY = anchorY + dy
        for dx = -SCAN_RADIUS, SCAN_RADIUS do
            for dz = -SCAN_RADIUS, SCAN_RADIUS do
                local bx = anchorX + dx
                local bz = anchorZ + dz
                local id = world.getBlock(bx, scanY, bz)
                local info = VALUABLE_BLOCKS[id]
                if info ~= nil then
                    local distSq = dx * dx + dy * dy + dz * dz
                    pendingOres[#pendingOres + 1] = {
                        x = bx,
                        y = scanY,
                        z = bz,
                        label = info.label,
                        color = info.color,
                        prio = info.prio,
                        dist = math.sqrt(distSq)
                    }
                end
            end
        end

        sliceIdx = sliceIdx + 1
        if sliceIdx > #Y_OFFSETS then
            sliceIdx = 1
            table.sort(pendingOres, function(a, b)
                if a.prio ~= b.prio then
                    return a.prio > b.prio
                end
                return a.dist < b.dist
            end)

            local top = {}
            local count = math.min(#pendingOres, 3)
            for i = 1, count do
                top[i] = pendingOres[i]
            end
            detectedOres = top

            if #top > 0 and top[1].dist <= 5.5 then
                local key = top[1].x .. ":" .. top[1].y .. ":" .. top[1].z
                if key ~= lastPingedKey and (now - lastGeigerTime) >= 3.0 then
                    sound.playBeep(1.9)
                    lastPingedKey = key
                    lastGeigerTime = now
                end
            end
        end
    end,

    onRenderHud = function(h)
        local w = h.getWidth()
        local screenH = h.getHeight()
        local midY = math.floor(screenH / 2)

        -- 1. Panel superior izquierdo: Bioma y Seguridad de Spawn (luz local)
        local lightLvl = player.getLightLevel()
        local biomeName = formatId(player.getBiome())
        local safeSpawn = lightLvl > 0
        local lightCol = safeSpawn and "#44FF88" or "#FF4444"
        local lightTag = safeSpawn and "[SEGURO]" or "[SPAWN!]"

        h.drawRect(8, 8, 124, 28, "#99070D14")
        h.drawRect(8, 8, 2, 28, lightCol)
        h.drawText(14, 11, "BIOMA: " .. biomeName, "#CCEEFF", 0.68)
        h.drawText(14, 20, string.format("LUZ: %02d/15 %s", lightLvl, lightTag), lightCol, 0.68)
        h.drawProgressBar(14, 29, 112, 3, lightLvl / 15.0, lightCol)

        -- 2. Marcadores AR 3D sobre menas/spawners detectados cerca
        for i = 1, #detectedOres do
            local ore = detectedOres[i]
            local proj = world.worldToScreen(ore.x + 0.5, ore.y + 0.5, ore.z + 0.5)
            if proj ~= nil and proj.visible and not proj.behind then
                local sx = math.floor(proj.x + 0.5)
                local sy = math.floor(proj.y + 0.5)
                local r = 5
                -- Rombo tactico AR
                h.drawLine(sx, sy - r, sx + r, sy, ore.color)
                h.drawLine(sx + r, sy, sx, sy + r, ore.color)
                h.drawLine(sx, sy + r, sx - r, sy, ore.color)
                h.drawLine(sx - r, sy, sx, sy - r, ore.color)

                local tag = string.format("%s %.1fm", ore.label, proj.distance)
                h.drawText(sx - math.floor(h.getTextWidth(tag, 0.65) / 2), sy - 13, tag, ore.color, 0.65)
            end
        end

        -- 3. Analizador de bloque apuntado con la mira (24 bloques)
        local hit = world.raycastBlock(24.0)
        if hit ~= nil then
            local cat, col = classifyBlock(hit.name)
            local bName = formatId(hit.name)
            local cardW = 122
            local cardX = w - cardW - 8
            local cardY = midY + 34

            h.drawRect(cardX, cardY, cardW, 38, "#99070D14")
            h.drawRect(cardX + cardW - 2, cardY, 2, 38, col)
            h.drawText(cardX + 5, cardY + 4, cat .. " " .. bName, col, 0.68)
            h.drawText(cardX + 5, cardY + 14, string.format("POS: %d, %d, %d", hit.x, hit.y, hit.z), "#CCCCCC", 0.65)
            local skyStr = hit.can_see_sky and "CIELO" or "CUBIERTO"
            h.drawText(cardX + 5, cardY + 23, string.format("DIST: %.1fm | %s", hit.distance, skyStr), "#AAAAAA", 0.65)
            local bLightCol = hit.light_level == 0 and "#FF5555" or "#88FFAA"
            h.drawText(cardX + 5, cardY + 31, string.format("LUZ BLOQUE: %d/15", hit.light_level), bLightCol, 0.62)
        end
    end
}
