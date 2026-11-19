-- [VG-BUNDLED v34]
-- ============================================================================
-- EJEMPLO 5: PROSPECTOR GEOLOGICO AR DE 14M (VETAS + LAVA + GEIGER)
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Escaneo esferico rapido en Java mediante world.scanBlocks(14, lista, 24)
--      cada 15 ticks (0.75s) consumiendo minimas instrucciones Lua.
--   2. Agrupamiento de bloques contiguos por tipo en vetas ('DIAMANTE x4 10.5m')
--      para no saturar la pantalla con etiquetas repetidas.
--   3. Proyeccion AR 3D con rombos tacticos + flechas de borde fuera de camara
--      + Geiger acustico progresivo segun cercania a la veta principal.
--   4. Detector doble de lava (4m bajo los pies y 3.5m frente a la mira).
--
-- HARDWARE: Autonomo (funciona en cualquier Gafa Modular sin modulo previo).
-- FLASHEO:  /vg givescript geo_scanner "Prospector Geo AR" #33DDFF
-- ============================================================================

local SCAN_RADIUS = 14

local VALUABLE_BLOCKS = {
    ["minecraft:ancient_debris"]        = { label = "ESCOMBROS", color = "#FF9933", prio = 5 },
    ["minecraft:spawner"]               = { label = "SPAWNER",   color = "#FF3366", prio = 5 },
    ["minecraft:diamond_ore"]           = { label = "DIAMANTE",  color = "#33FFFF", prio = 4 },
    ["minecraft:deepslate_diamond_ore"] = { label = "DIAMANTE",  color = "#33FFFF", prio = 4 },
    ["minecraft:emerald_ore"]           = { label = "ESMERALDA", color = "#33FF77", prio = 4 },
    ["minecraft:deepslate_emerald_ore"] = { label = "ESMERALDA", color = "#33FF77", prio = 4 },
    ["minecraft:chest"]                 = { label = "COFRE",     color = "#FFAA44", prio = 4 },
    ["minecraft:trapped_chest"]         = { label = "COFRE-TR",  color = "#FF7744", prio = 4 },
    ["minecraft:barrel"]                = { label = "BARRIL",    color = "#FFAA44", prio = 4 },
    ["minecraft:budding_amethyst"]      = { label = "AMATISTA",  color = "#CC77FF", prio = 4 },
    ["minecraft:gold_ore"]              = { label = "ORO",       color = "#FFDD33", prio = 3 },
    ["minecraft:deepslate_gold_ore"]    = { label = "ORO",       color = "#FFDD33", prio = 3 },
    ["minecraft:nether_gold_ore"]       = { label = "ORO",       color = "#FFDD33", prio = 3 },
    ["minecraft:lapis_ore"]             = { label = "LAPIS",     color = "#4488FF", prio = 3 },
    ["minecraft:deepslate_lapis_ore"]   = { label = "LAPIS",     color = "#4488FF", prio = 3 },
    ["minecraft:iron_ore"]              = { label = "HIERRO",    color = "#FFCC99", prio = 2 },
    ["minecraft:deepslate_iron_ore"]    = { label = "HIERRO",    color = "#FFCC99", prio = 2 },
    ["minecraft:redstone_ore"]          = { label = "REDSTONE",  color = "#FF4444", prio = 2 },
    ["minecraft:deepslate_redstone_ore"]= { label = "REDSTONE",  color = "#FF4444", prio = 2 }
}

local SCAN_IDS = {}
for id, _ in pairs(VALUABLE_BLOCKS) do
    SCAN_IDS[#SCAN_IDS + 1] = id
end

local tickCounter = 0
local veins = {}
local lastLavaAlert = 0
local lastFrontLavaAlert = 0
local lavaAhead = false
local nextGeigerTime = 0

local function formatId(rawId)
    if rawId == nil or rawId == "" then return "DESCONOCIDO" end
    local s = rawId
    local colon = string.find(s, ":", 1, true)
    if colon ~= nil then
        s = string.sub(s, colon + 1)
    end
    s = string.gsub(s, "_", " ")
    if #s > 16 then
        s = string.sub(s, 1, 15) .. "."
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
    if string.find(blockId, "_ore", 1, true) or string.find(blockId, "copper", 1, true) or string.find(blockId, "quartz", 1, true) then
        return "[MINERAL]", "#FFCC44"
    end
    return nil, nil
end

local function getDirTag(vx, vy, vz)
    local px = player.getX()
    local py = player.getY()
    local pz = player.getZ()
    local yawRad = math.rad(player.getYaw())
    local fx = -math.sin(yawRad)
    local fz = math.cos(yawRad)
    local rx = -math.cos(yawRad)
    local rz = -math.sin(yawRad)

    local dx = vx - px
    local dz = vz - pz
    local fwd = dx * fx + dz * fz
    local right = dx * rx + dz * rz

    local dir = ""
    if math.abs(fwd) >= math.abs(right) then
        dir = fwd >= 0 and "FRE" or "ATR"
    else
        dir = right >= 0 and "DER" or "IZQ"
    end

    local dy = vy - py
    if dy >= 2.5 then
        return "ARR-" .. dir
    elseif dy <= -2.5 then
        return "ABJ-" .. dir
    end
    return dir
end

module = {
    id = "vision_goggles:geo_scanner",
    name = "Prospector Geologico AR",
    type = "UTILITY",
    battery_drain = 0.15,

    onEnable = function(p)
        tickCounter = 0
        veins = {}
        lavaAhead = false
        nextGeigerTime = getTime()
        sound.playBeep(1.45)
    end,

    onDisable = function(p)
        veins = {}
        lavaAhead = false
    end,

    onTick = function(p, batteryPct)
        local now = getTime()
        local px = math.floor(p.getX())
        local py = math.floor(p.getY())
        local pz = math.floor(p.getZ())

        -- 1. Seguridad de lava bajo los pies (hasta 4 bloques abajo)
        local lavaBelowDist = nil
        for d = 1, 4 do
            if world.getBlock(px, py - d, pz) == "minecraft:lava" then
                lavaBelowDist = d
                break
            end
        end
        if lavaBelowDist ~= nil and (now - lastLavaAlert) >= 4.0 then
            sound.playAlert()
            hud.showAlert("¡LAVA BAJO TUS PIES (" .. lavaBelowDist .. "m)!", "#FF5500", 2.0)
            lastLavaAlert = now
        end

        -- 2. Seguridad de bolsillo de lava frente a la mira (hasta 3.5 bloques)
        local yawRad = math.rad(p.getYaw())
        local pitchRad = math.rad(p.getPitch())
        local cp = math.cos(pitchRad)
        local lx = -math.sin(yawRad) * cp
        local ly = -math.sin(pitchRad)
        local lz = math.cos(yawRad) * cp
        local eyeY = p.getY() + 1.5
        local pX = p.getX()
        local pZ = p.getZ()

        local foundFrontLava = false
        for step = 1.5, 3.5, 1.0 do
            local blk = world.getBlock(pX + lx * step, eyeY + ly * step, pZ + lz * step)
            if blk == "minecraft:lava" then
                foundFrontLava = true
                break
            end
        end
        lavaAhead = foundFrontLava
        if foundFrontLava and (now - lastFrontLavaAlert) >= 3.5 then
            sound.playAlert()
            hud.showAlert("¡BOLSILLO DE LAVA ADELANTE!", "#FF4422", 1.8)
            lastFrontLavaAlert = now
        end

        -- 3. Barrido geologico rapido en Java cada 15 ticks (0.75s) + agrupacion de vetas
        tickCounter = tickCounter + 1
        if tickCounter == 1 or (tickCounter % 15) == 0 then
            local hits = world.scanBlocks(SCAN_RADIUS, SCAN_IDS, 24)
            local clusters = {}

            if hits ~= nil then
                for i = 1, #hits do
                    local b = hits[i]
                    local info = VALUABLE_BLOCKS[b.id]
                    if info ~= nil then
                        local merged = false
                        for j = 1, #clusters do
                            local c = clusters[j]
                            if c.label == info.label then
                                local ddx = b.x - c.seedX
                                local ddy = b.y - c.seedY
                                local ddz = b.z - c.seedZ
                                if (ddx * ddx + ddy * ddy + ddz * ddz) <= 6.5 then
                                    c.count = c.count + 1
                                    c.sumX = c.sumX + b.x
                                    c.sumY = c.sumY + b.y
                                    c.sumZ = c.sumZ + b.z
                                    c.x = c.sumX / c.count
                                    c.y = c.sumY / c.count
                                    c.z = c.sumZ / c.count
                                    if b.distance < c.dist then
                                        c.dist = b.distance
                                    end
                                    merged = true
                                    break
                                end
                            end
                        end

                        if not merged then
                            clusters[#clusters + 1] = {
                                label = info.label,
                                color = info.color,
                                prio = info.prio,
                                count = 1,
                                seedX = b.x,
                                seedY = b.y,
                                seedZ = b.z,
                                sumX = b.x,
                                sumY = b.y,
                                sumZ = b.z,
                                x = b.x,
                                y = b.y,
                                z = b.z,
                                dist = b.distance
                            }
                        end
                    end
                end
            end

            table.sort(clusters, function(a, b)
                if a.prio ~= b.prio then
                    return a.prio > b.prio
                end
                return a.dist < b.dist
            end)

            local top = {}
            for i = 1, math.min(#clusters, 4) do
                top[i] = clusters[i]
            end
            veins = top
        end

        -- 4. Geiger progresivo segun cercania a la veta prioritaria (<= 12m)
        if #veins > 0 and veins[1].dist <= 12.0 then
            local d = veins[1].dist
            local ratio = math.max(0.0, math.min(1.0, (12.0 - d) / 12.0))
            local interval = 2.2 - ratio * 1.75
            local pitch = 1.1 + ratio * 0.95
            if now >= nextGeigerTime then
                sound.playBeep(pitch)
                nextGeigerTime = now + interval
            end
        end
    end,

    onRenderHud = function(h)
        local w = h.getWidth()
        local screenH = h.getHeight()
        local midY = math.floor(screenH / 2)
        local now = getTime()

        -- 1. Panel de prospeccion bajo las coordenadas (x=8, y=34) sin solapar HUD base
        local lightLvl = player.getLightLevel()
        local safeSpawn = lightLvl > 0
        local borderCol = lavaAhead and "#FF3322" or (safeSpawn and "#33DDFF" or "#FF5555")
        local panelW = 136
        local panelH = 33

        h.drawRect(8, 34, panelW, panelH, "#99070D14")
        h.drawRect(8, 34, 2, panelH, borderCol)

        if lavaAhead then
            local flash = math.sin(now * 14.0) > 0 and "#FF3322" or "#FFAA33"
            h.drawText(14, 37, "¡PELIGRO: LAVA ADELANTE!", flash, 0.68)
        else
            local lightCol = safeSpawn and "#66FF99" or "#FF5555"
            local lightTag = safeSpawn and "SEG" or "SPAWN!"
            h.drawText(14, 37, string.format("PROSPECTOR %dm | L:%02d %s", SCAN_RADIUS, lightLvl, lightTag), lightCol, 0.64)
        end

        if #veins > 0 then
            local top = veins[1]
            local dir = getDirTag(top.x, top.y, top.z)
            local l1 = string.format("%s x%d | %.1fm [%s]", top.label, top.count, top.dist, dir)
            h.drawText(14, 46, l1, top.color, 0.68)

            local extra = (#veins >= 2)
                    and string.format("VETAS:%d | +%s %.0fm", #veins, veins[2].label, veins[2].dist)
                    or string.format("VETAS EN RANGO: %d", #veins)
            h.drawText(14, 55, extra, "#AABBCC", 0.62)
        else
            h.drawText(14, 46, "SIN VETAS EN 14M", "#8899AA", 0.66)
            h.drawText(14, 55, formatId(player.getBiome()), "#667788", 0.62)
        end

        -- 2. Marcadores AR 3D sobre hasta 4 vetas detectadas + flecha si la principal queda fuera
        local yawRad = math.rad(player.getYaw())
        local rx = -math.cos(yawRad)
        local rz = -math.sin(yawRad)
        local px = player.getX()
        local pz = player.getZ()

        for i = 1, #veins do
            local v = veins[i]
            local proj = world.worldToScreen(v.x + 0.5, v.y + 0.5, v.z + 0.5)
            if proj ~= nil and proj.visible and not proj.behind then
                local sx = math.floor(proj.x + 0.5)
                local sy = math.floor(proj.y + 0.5)
                local r = (i == 1) and 6 or 4
                h.drawLine(sx, sy - r, sx + r, sy, v.color)
                h.drawLine(sx + r, sy, sx, sy + r, v.color)
                h.drawLine(sx, sy + r, sx - r, sy, v.color)
                h.drawLine(sx - r, sy, sx, sy - r, v.color)

                local tag = v.count > 1
                        and string.format("%s x%d %.1fm", v.label, v.count, proj.distance)
                        or string.format("%s %.1fm", v.label, proj.distance)
                h.drawText(sx - math.floor(h.getTextWidth(tag, 0.62) / 2), sy - r - 8, tag, v.color, 0.62)
            elseif i == 1 then
                local rightDot = (v.x - px) * rx + (v.z - pz) * rz
                local blink = math.sin(now * 6.0) > -0.2
                if blink then
                    if rightDot >= 0 then
                        h.drawLine(w - 14, midY - 6, w - 8, midY, v.color)
                        h.drawLine(w - 8, midY, w - 14, midY + 6, v.color)
                    else
                        h.drawLine(14, midY - 6, 8, midY, v.color)
                        h.drawLine(8, midY, 14, midY + 6, v.color)
                    end
                end
            end
        end

        -- 3. Inspector compacto en mira solo ante minerales/peligros o al agacharse
        local hit = world.raycastBlock(24.0)
        if hit ~= nil then
            local cat, col = classifyBlock(hit.name)
            if cat ~= nil or player.isCrouching() then
                cat = cat or "[BLOQUE]"
                col = col or "#99CCDD"
                local bName = formatId(hit.name)
                local cardW = 136
                local cardX = 8
                local cardY = 70

                h.drawRect(cardX, cardY, cardW, 24, "#99070D14")
                h.drawRect(cardX, cardY, 2, 24, col)
                h.drawText(cardX + 6, cardY + 4, cat .. " " .. bName, col, 0.65)
                h.drawText(cardX + 6, cardY + 13, string.format("%.1fm | L:%d %s", hit.distance, hit.light_level, hit.can_see_sky and "CIELO" or "SUB"), "#CCCCCC", 0.62)
            end
        end
    end
}
