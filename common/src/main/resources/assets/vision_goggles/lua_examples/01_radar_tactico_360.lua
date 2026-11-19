-- [VG-BUNDLED v34]
-- ============================================================================
-- EJEMPLO 1: RADAR TACTICO CIRCULAR 360° (UPGRADE DSP PARA SONAR)
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Requerir el modulo fisico de Sonar ('required_module = "sonar"') para
--      ampliar el sonar basico cuadrado a un radar tactico 360° continuo sin
--      duplicar interfaces (SonarRadarModule cede automaticamente el HUD).
--   2. Leer player.getSonarMode() para respetar el filtro elegido en el menu
--      radial (0 = Todos, 1 = Solo Jugadores, 2 = Solo No-Muertos).
--   3. Proyectar contactos 3D respecto a player.getYaw() + sonar acustico.
--
-- HARDWARE: Requiere modulo "sonar" instalado en Gafas Modulares.
-- FLASHEO:  /vg givescript radar_amenazas "Sonar Pro 360" #00FFAA
-- ============================================================================

local RANGE = 36.0
local RADIUS = 36
local MAX_BLIPS = 18

local CRITICAL_TYPES = {
    ["minecraft:creeper"] = true,
    ["minecraft:warden"] = true,
    ["minecraft:wither"] = true,
    ["minecraft:ender_dragon"] = true
}

local cachedContacts = {}
local hostileCount = 0
local criticalCount = 0
local activeFilterMode = 0
local closestHostileDist = nil
local nextPingTime = 0
local nextCriticalAlert = 0

local function clamp(val, minV, maxV)
    if val < minV then return minV end
    if val > maxV then return maxV end
    return val
end

module = {
    id = "vision_goggles:radar_amenazas",
    name = "Sonar Pro 360",
    type = "UTILITY",
    required_module = "sonar",
    battery_drain = 0.25,

    onEnable = function(p)
        cachedContacts = {}
        activeFilterMode = p.getSonarMode()
        nextPingTime = getTime()
        sound.playBeep(1.4)
    end,

    onDisable = function(p)
        cachedContacts = {}
        hostileCount = 0
        criticalCount = 0
        closestHostileDist = nil
    end,

    onTick = function(p, batteryPct)
        local sonarMode = p.getSonarMode()
        activeFilterMode = sonarMode

        local nearby = world.getEntitiesInRadius(RANGE)
        if nearby == nil then
            cachedContacts = {}
            hostileCount = 0
            criticalCount = 0
            closestHostileDist = nil
            return
        end

        local list = {}
        local hCount = 0
        local cCount = 0
        local minHostile = nil
        local minCritical = nil
        local critName = nil

        for i = 1, #nearby do
            local e = nearby[i]
            local include = true
            if sonarMode == 1 then
                include = (e.is_player == true)
            elseif sonarMode == 2 then
                include = (e.is_undead == true)
            end

            if include then
                local isCrit = CRITICAL_TYPES[e.type] == true
                if e.is_hostile or isCrit then
                    hCount = hCount + 1
                    if minHostile == nil or e.distance < minHostile then
                        minHostile = e.distance
                    end
                end
                if isCrit then
                    cCount = cCount + 1
                    if minCritical == nil or e.distance < minCritical then
                        minCritical = e.distance
                        critName = e.name
                    end
                end

                local prio = 0
                if isCrit then
                    prio = 3
                elseif e.is_player then
                    prio = 2
                elseif e.is_hostile then
                    prio = 1
                end

                list[#list + 1] = {
                    x = e.x,
                    y = e.y,
                    z = e.z,
                    distance = e.distance,
                    is_hostile = e.is_hostile,
                    is_player = e.is_player,
                    is_critical = isCrit,
                    prio = prio
                }
            end
        end

        table.sort(list, function(a, b)
            if a.prio ~= b.prio then
                return a.prio > b.prio
            end
            return a.distance < b.distance
        end)

        local trimmed = {}
        local limit = math.min(#list, MAX_BLIPS)
        for i = 1, limit do
            trimmed[i] = list[i]
        end

        cachedContacts = trimmed
        hostileCount = hCount
        criticalCount = cCount
        closestHostileDist = minHostile

        local now = getTime()
        if minHostile ~= nil and minHostile <= 16.0 then
            local proximity = clamp((16.0 - minHostile) / 16.0, 0.0, 1.0)
            local interval = 1.5 - proximity * 1.15
            local pitch = 0.90 + proximity * 0.95
            if now >= nextPingTime then
                sound.playBeep(pitch)
                nextPingTime = now + interval
            end
        end

        if minCritical ~= nil and minCritical <= 7.5 and now >= nextCriticalAlert then
            sound.playAlert()
            hud.showAlert("PROXIMIDAD: " .. string.upper(critName) .. " (" .. string.format("%.1f", minCritical) .. "m)", "#FF2244", 1.8)
            nextCriticalAlert = now + 4.0
        end
    end,

    onRenderHud = function(h)
        local screenH = h.getHeight()
        local cx = 50
        local cy = screenH - 56
        local now = getTime()

        h.drawDisc(cx, cy, RADIUS + 2, "#B8071018")
        h.drawCircle(cx, cy, RADIUS, "#DD00FFAA")
        h.drawCircle(cx, cy, math.floor(RADIUS * 0.66), "#4400CC88")
        h.drawCircle(cx, cy, math.floor(RADIUS * 0.33), "#3300CC88")

        h.drawLine(cx - RADIUS, cy, cx + RADIUS, cy, "#3300FFAA")
        h.drawLine(cx, cy - RADIUS, cx, cy + RADIUS, "#3300FFAA")

        local sweepAngle = (now * 2.4) % (2.0 * math.pi)
        local sx = cx + math.floor(math.cos(sweepAngle) * (RADIUS - 1) + 0.5)
        local sy = cy + math.floor(math.sin(sweepAngle) * (RADIUS - 1) + 0.5)
        h.drawLine(cx, cy, sx, sy, "#8800FFCC")

        local yawRad = math.rad(player.getYaw())
        local fx = -math.sin(yawRad)
        local fz = math.cos(yawRad)
        local rx = -math.cos(yawRad)
        local rz = -math.sin(yawRad)

        local northFwd = -fz
        local northRight = -rz
        local nx = cx + math.floor(northRight * (RADIUS - 4) + 0.5)
        local ny = cy - math.floor(northFwd * (RADIUS - 4) + 0.5)
        h.drawText(nx - 2, ny - 3, "N", "#FFCC33", 0.65)

        local px = player.getX()
        local py = player.getY()
        local pz = player.getZ()
        local innerR = RADIUS - 3
        local pulse = (math.sin(now * 8.0) + 1.0) * 0.5

        for i = 1, #cachedContacts do
            local c = cachedContacts[i]
            local dx = c.x - px
            local dy = c.y - py
            local dz = c.z - pz

            local fwd = dx * fx + dz * fz
            local right = dx * rx + dz * rz

            local bx = cx + math.floor((right / RANGE) * innerR + 0.5)
            local by = cy - math.floor((fwd / RANGE) * innerR + 0.5)

            local ox = bx - cx
            local oy = by - cy
            local mag = math.sqrt(ox * ox + oy * oy)
            if mag > innerR then
                bx = cx + math.floor(ox * (innerR / mag) + 0.5)
                by = cy + math.floor(oy * (innerR / mag) + 0.5)
            end

            if c.is_critical then
                local critColor = pulse > 0.5 and "#FF1155" or "#FFFF44"
                h.drawDisc(bx, by, 2, critColor)
                h.drawCircle(bx, by, 4, "#AAFF2244")
            elseif c.is_player then
                h.drawDisc(bx, by, 2, "#00EEFF")
            elseif c.is_hostile then
                h.drawDisc(bx, by, 2, "#FF4433")
            else
                h.drawDisc(bx, by, 1, "#55DD77")
            end

            if dy >= 4.0 then
                h.drawLine(bx, by - 3, bx, by - 5, "#AAFFFFFF")
            elseif dy <= -4.0 then
                h.drawLine(bx, by + 3, bx, by + 5, "#AAFFFFFF")
            end
        end

        h.drawLine(cx, cy - 3, cx - 2, cy + 2, "#FFFFFF")
        h.drawLine(cx, cy - 3, cx + 2, cy + 2, "#FFFFFF")
        h.drawLine(cx - 2, cy + 2, cx + 2, cy + 2, "#FFFFFF")

        local labelColor = criticalCount > 0 and "#FF4466" or (hostileCount > 0 and "#FFAA33" or "#00FFAA")
        local clsText = closestHostileDist ~= nil and string.format("%.1fm", closestHostileDist) or "--"
        local modeTag = (activeFilterMode == 1) and "PLY" or ((activeFilterMode == 2) and "UND" or "ALL")
        local infoStr = string.format("%s H:%d %s", modeTag, hostileCount, clsText)
        local textScale = 0.68
        local boxW = math.max(78, math.floor(h.getTextWidth(infoStr, textScale) + 10))
        local boxX = math.max(4, cx - math.floor(boxW / 2))
        h.drawRect(boxX, cy + RADIUS + 3, boxW, 12, "#99071018")
        h.drawText(boxX + math.floor((boxW - h.getTextWidth(infoStr, textScale)) / 2), cy + RADIUS + 5, infoStr, labelColor, textScale)
    end
}
