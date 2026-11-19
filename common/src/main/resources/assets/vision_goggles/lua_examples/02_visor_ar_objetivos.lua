-- [VG-BUNDLED v34]
-- ============================================================================
-- EJEMPLO 2: VISOR AR 3D DE OBJETIVOS Y ANALIZADOR IFF EN MIRA
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Proyeccion Realidad Aumentada 3D -> 2D con world.worldToScreen(x, y, z)
--      para dibujar corchetes tacticos [  ] sobre amenazas en el mundo.
--   2. Indicadores direccionales fuera de campo (< / >) cuando un enemigo
--      peligroso se acerca por tus flancos o por la espalda.
--   3. Fijacion de objetivo con world.getTargetEntity(32) mostrando barra
--      de vida, armadura real, distancia y desnivel vertical.
--   4. Calculo de ancho de texto dinamico con hud.getTextWidth(texto, escala).
--
-- HARDWARE: Requiere modulo "bio" (Biometrico) instalado en Gafas Modulares.
-- FLASHEO:  /vg givescript threat_scanner "Visor AR IFF" #FF4444
-- ============================================================================

local SCAN_RANGE = 28.0
local MAX_AR_MARKERS = 6

local PRIORITY_TYPES = {
    ["minecraft:creeper"] = true,
    ["minecraft:warden"] = true,
    ["minecraft:wither"] = true,
    ["minecraft:ender_dragon"] = true,
    ["minecraft:skeleton"] = true,
    ["minecraft:blaze"] = true,
    ["minecraft:pillager"] = true,
    ["minecraft:vindicator"] = true
}

local arTargets = {}
local lastDangerAlert = 0
local lastLockedKey = nil
local lockStartTime = 0

local function clamp(v, lo, hi)
    if v < lo then return lo end
    if v > hi then return hi end
    return v
end

local function drawCornerBrackets(h, cx, cy, halfSize, armLen, color)
    local x1 = cx - halfSize
    local x2 = cx + halfSize
    local y1 = cy - halfSize
    local y2 = cy + halfSize

    -- Esquina superior izquierda
    h.drawLine(x1, y1, x1 + armLen, y1, color)
    h.drawLine(x1, y1, x1, y1 + armLen, color)
    -- Esquina superior derecha
    h.drawLine(x2 - armLen, y1, x2, y1, color)
    h.drawLine(x2, y1, x2, y1 + armLen, color)
    -- Esquina inferior izquierda
    h.drawLine(x1, y2 - armLen, x1, y2, color)
    h.drawLine(x1, y2, x1 + armLen, y2, color)
    -- Esquina inferior derecha
    h.drawLine(x2 - armLen, y2, x2, y2, color)
    h.drawLine(x2, y2 - armLen, x2, y2, color)
end

local function getRelativeDirectionLabel(tx, tz)
    local dx = tx - player.getX()
    local dz = tz - player.getZ()
    local yawRad = math.rad(player.getYaw())
    local fx = -math.sin(yawRad)
    local fz = math.cos(yawRad)
    local rx = -math.cos(yawRad)
    local rz = -math.sin(yawRad)

    local fwd = dx * fx + dz * fz
    local right = dx * rx + dz * rz

    if math.abs(fwd) >= math.abs(right) then
        return fwd >= 0 and "AL FRENTE" or "DETRAS"
    else
        return right >= 0 and "A TU DER." or "A TU IZQ."
    end
end

module = {
    id = "vision_goggles:threat_scanner",
    name = "Visor AR y Analizador IFF",
    type = "UTILITY",
    required_module = "bio",
    battery_drain = 0.08,

    onEnable = function(p)
        arTargets = {}
        lastLockedKey = nil
        sound.playBeep(1.3)
        hud.showAlert("ESCANER AR BIOMETRICO ACTIVO", "#33FFAA", 1.8)
    end,

    onDisable = function(p)
        arTargets = {}
        lastLockedKey = nil
    end,

    onTick = function(p, batteryPct)
        local nearby = world.getEntitiesInRadius(SCAN_RANGE)
        if nearby == nil then
            arTargets = {}
            return
        end

        local list = {}
        local urgentMob = nil
        local now = getTime()

        for i = 1, #nearby do
            local e = nearby[i]
            local isSpecial = (e.type == "minecraft:creeper" or e.type == "minecraft:warden")
            if isSpecial and e.distance <= 10.0 then
                if urgentMob == nil or e.distance < urgentMob.distance then
                    urgentMob = e
                end
            end

            if e.is_hostile or e.is_player then
                local weight = 1
                if e.type == "minecraft:creeper" or e.type == "minecraft:warden" or e.type == "minecraft:wither" then
                    weight = 3
                elseif PRIORITY_TYPES[e.type] then
                    weight = 2
                end
                list[#list + 1] = {
                    name = e.name,
                    type = e.type,
                    x = e.x,
                    y = e.y,
                    z = e.z,
                    height = e.height or 1.8,
                    distance = e.distance,
                    is_hostile = e.is_hostile,
                    is_player = e.is_player,
                    weight = weight
                }
            end
        end

        table.sort(list, function(a, b)
            if a.weight ~= b.weight then
                return a.weight > b.weight
            end
            return a.distance < b.distance
        end)

        local trimmed = {}
        for i = 1, math.min(#list, MAX_AR_MARKERS) do
            trimmed[i] = list[i]
        end
        arTargets = trimmed

        -- Alerta de proximidad con cooldown en segundos reales (4.5s)
        if urgentMob ~= nil and (now - lastDangerAlert) >= 4.5 then
            local dirLabel = getRelativeDirectionLabel(urgentMob.x, urgentMob.z)
            sound.playAlert()
            hud.showAlert("¡PELIGRO: " .. string.upper(urgentMob.name) .. " " .. dirLabel .. "!", "#FF2222", 2.0)
            lastDangerAlert = now
        end
    end,

    onRenderHud = function(h)
        local screenW = h.getWidth()
        local screenH = h.getHeight()
        local midX = math.floor(screenW / 2)
        local midY = math.floor(screenH / 2)
        local now = getTime()

        -- 1. Proyeccion AR 3D sobre los objetivos prioritarios
        local yawRad = math.rad(player.getYaw())
        local rx = -math.cos(yawRad)
        local rz = -math.sin(yawRad)
        local px = player.getX()
        local pz = player.getZ()

        local hasLeftOffscreen = false
        local hasRightOffscreen = false

        for i = 1, #arTargets do
            local t = arTargets[i]
            local proj = world.worldToScreen(t.x, t.y + t.height * 0.5, t.z)

            if proj ~= nil and proj.visible and not proj.behind then
                local sx = math.floor(proj.x + 0.5)
                local sy = math.floor(proj.y + 0.5)
                local half = clamp(math.floor(130.0 / math.max(2.5, proj.distance)), 7, 22)
                local arm = math.max(3, math.floor(half * 0.45))
                local boxColor = "#FF5544"
                if t.is_player then
                    boxColor = "#00EEFF"
                elseif t.weight >= 3 then
                    boxColor = (math.sin(now * 10.0) > 0) and "#FF1155" or "#FFCC00"
                end

                drawCornerBrackets(h, sx, sy, half, arm, boxColor)

                local distTxt = string.format("%.0fm", proj.distance)
                h.drawText(sx - math.floor(h.getTextWidth(distTxt, 0.65) / 2), sy + half + 2, distTxt, boxColor, 0.65)
            elseif t.is_hostile and t.distance <= 14.0 then
                -- Si la amenaza cercana esta fuera de camara, marcar borde izquierdo/derecho
                local rightDot = (t.x - px) * rx + (t.z - pz) * rz
                if rightDot >= 0 then
                    hasRightOffscreen = true
                else
                    hasLeftOffscreen = true
                end
            end
        end

        -- Chevrones laterales para amenazas cercanas fuera de vista
        local blink = math.sin(now * 9.0) > 0
        if hasLeftOffscreen and blink then
            h.drawLine(14, midY - 6, 8, midY, "#FF3344")
            h.drawLine(8, midY, 14, midY + 6, "#FF3344")
        end
        if hasRightOffscreen and blink then
            h.drawLine(screenW - 14, midY - 6, screenW - 8, midY, "#FF3344")
            h.drawLine(screenW - 8, midY, screenW - 14, midY + 6, "#FF3344")
        end

        -- 2. Analizador tactico de objetivo directo en mira (sin atravesar muros)
        local target = world.getTargetEntity(32.0, false)
        if target ~= nil then
            local key = target.type .. ":" .. target.name
            if key ~= lastLockedKey then
                lastLockedKey = key
                lockStartTime = now
                sound.playClick()
            end

            -- Animacion de fijacion de corchetes al adquirir un nuevo blanco
            local lockAnim = clamp((now - lockStartTime) / 0.25, 0.0, 1.0)
            local retHalf = math.floor(18 - lockAnim * 7)
            local retColor = target.is_hostile and "#FF4444" or (target.is_player and "#00FFFF" or "#44FF88")
            drawCornerBrackets(h, midX, midY, retHalf, 4, retColor)

            -- Clasificacion IFF
            local iffTag = "[NEUTRAL]"
            if target.type == "minecraft:creeper" or target.type == "minecraft:warden" or target.type == "minecraft:wither" then
                iffTag = "[CRITICO]"
                retColor = "#FF2255"
            elseif target.is_player then
                iffTag = "[ALIADO]"
                retColor = "#00EEFF"
            elseif target.is_hostile then
                iffTag = "[HOSTIL]"
                retColor = "#FF4444"
            end

            local title = iffTag .. " " .. target.name
            local maxHp = math.max(0.1, target.max_health)
            local hpPct = clamp(target.health / maxHp, 0.0, 1.0)
            local hpStr = string.format("HP: %.1f / %.1f", target.health, target.max_health)
            local dy = target.y - player.getY()
            local metaStr = string.format("DIST: %.1fm  ALT: %+.1fm", target.distance, dy)
            if target.armor > 0 then
                metaStr = metaStr .. "  ARM: " .. target.armor
            end

            local cardW = math.max(122, math.floor(math.max(h.getTextWidth(title, 0.8), h.getTextWidth(metaStr, 0.7)) + 12))
            local cardH = 38
            local cardX = midX - math.floor(cardW / 2)
            local cardY = midY + 16

            h.drawRect(cardX, cardY, cardW, cardH, "#B80A111A")
            h.drawRect(cardX, cardY, 2, cardH, retColor)
            h.drawText(cardX + 6, cardY + 4, title, retColor, 0.80)
            h.drawProgressBar(cardX + 6, cardY + 14, cardW - 12, 4, hpPct, retColor)
            h.drawText(cardX + 6, cardY + 21, hpStr, "#EEEEEE", 0.70)
            h.drawText(cardX + 6, cardY + 29, metaStr, "#AAAAAA", 0.68)
        else
            lastLockedKey = nil
        end
    end
}
