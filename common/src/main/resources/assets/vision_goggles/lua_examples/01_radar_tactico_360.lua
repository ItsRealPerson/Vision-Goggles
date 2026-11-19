-- ============================================================================
-- EJEMPLO 1: RADAR TACTICO CIRCULAR 360° CON SONAR DE PROXIMIDAD
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Usar variables 'local' para no contaminar otros scripts cargados.
--   2. Escaneo eficiente de entidades en onTick (20 TPS) + dibujo fluido en
--      onRenderHud (a los FPS del juego) sin superar el limite de instrucciones.
--   3. Trigonometria de rotacion relativa al jugador (player.getYaw) para
--      proyectar contactos reales en un radar giratorio de 360 grados.
--   4. Audio adaptativo (sound.playBeep / sound.playAlert) segun distancia.
--
-- HARDWARE: Autonomo (funciona en cualquier Gafa Modular sin modulo previo).
-- FLASHEO:  /vg givescript radar_amenazas "Radar Tactico 360" #00FFAA
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

-- Cache compartida entre onTick y onRenderHud (siempre 'local')
local cachedContacts = {}
local hostileCount = 0
local criticalCount = 0
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
    name = "Radar Tactico 360",
    type = "UTILITY",
    battery_drain = 0.05,

    onEnable = function(p)
        cachedContacts = {}
        nextPingTime = getTime()
        sound.playBeep(1.4)
    end,

    onDisable = function(p)
        cachedContacts = {}
        hostileCount = 0
        criticalCount = 0
        closestHostileDist = nil
    end,

    -- El escaneo pesado se hace en onTick (20 veces/seg), nunca en onRenderHud
    onTick = function(p, batteryPct)
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

            -- Prioridad visual: criticos (3) > jugadores (2) > hostiles (1) > pasivos (0)
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

        -- Ordenar primero por prioridad y luego por cercania
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

        -- Sonar acustico proporcional a la cercania del enemigo
        local now = getTime()
        if minHostile ~= nil and minHostile <= 16.0 then
            local proximity = clamp((16.0 - minHostile) / 16.0, 0.0, 1.0)
            local interval = 1.5 - proximity * 1.15 -- de 1.50s a 0.35s
            local pitch = 0.90 + proximity * 0.95   -- de 0.90 a 1.85
            if now >= nextPingTime then
                sound.playBeep(pitch)
                nextPingTime = now + interval
            end
        end

        -- Alarma critica si un Creeper o Warden entra en zona mortal
        if minCritical ~= nil and minCritical <= 7.5 and now >= nextCriticalAlert then
            sound.playAlert()
            hud.showAlert("PROXIMIDAD: " .. string.upper(critName) .. " (" .. string.format("%.1f", minCritical) .. "m)", "#FF2244", 1.8)
            nextCriticalAlert = now + 4.0
        end
    end,

    onRenderHud = function(h)
        local screenH = h.getHeight()
        local cx = 48
        local cy = screenH - 56
        local now = getTime()

        -- 1. Chasis circular del radar y anillos concentricos (12m, 24m, 36m)
        h.drawDisc(cx, cy, RADIUS + 2, "#B8071018")
        h.drawCircle(cx, cy, RADIUS, "#DD00FFAA")
        h.drawCircle(cx, cy, math.floor(RADIUS * 0.66), "#4400CC88")
        h.drawCircle(cx, cy, math.floor(RADIUS * 0.33), "#3300CC88")

        -- Cruz de ejes tacticos
        h.drawLine(cx - RADIUS, cy, cx + RADIUS, cy, "#3300FFAA")
        h.drawLine(cx, cy - RADIUS, cx, cy + RADIUS, "#3300FFAA")

        -- 2. Haz de barrido rotatorio animado con getTime()
        local sweepAngle = (now * 2.4) % (2.0 * math.pi)
        local sx = cx + math.floor(math.cos(sweepAngle) * (RADIUS - 1) + 0.5)
        local sy = cy + math.floor(math.sin(sweepAngle) * (RADIUS - 1) + 0.5)
        h.drawLine(cx, cy, sx, sy, "#8800FFCC")

        -- 3. Base trigonometrica del jugador (ARRIBA en radar = hacia donde miras)
        local yawRad = math.rad(player.getYaw())
        local fx = -math.sin(yawRad)
        local fz = math.cos(yawRad)
        local rx = -math.cos(yawRad)
        local rz = -math.sin(yawRad)

        -- Marcador cardinal Norte ('N') orbitando el borde del radar
        local northFwd = -fz
        local northRight = -rz
        local nx = cx + math.floor(northRight * (RADIUS - 4) + 0.5)
        local ny = cy - math.floor(northFwd * (RADIUS - 4) + 0.5)
        h.drawText(nx - 2, ny - 3, "N", "#FFCC33", 0.65)

        -- 4. Proyectar blips guardados
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

            -- Clamping radial si el objetivo roza el borde
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

            -- Indicador sutil de desnivel vertical (>= 4 bloques arriba o abajo)
            if dy >= 4.0 then
                h.drawLine(bx, by - 3, bx, by - 5, "#AAFFFFFF")
            elseif dy <= -4.0 then
                h.drawLine(bx, by + 3, bx, by + 5, "#AAFFFFFF")
            end
        end

        -- 5. Triangulo central del jugador apuntando hacia arriba
        h.drawLine(cx, cy - 3, cx - 2, cy + 2, "#FFFFFF")
        h.drawLine(cx, cy - 3, cx + 2, cy + 2, "#FFFFFF")
        h.drawLine(cx - 2, cy + 2, cx + 2, cy + 2, "#FFFFFF")

        -- 6. Telemetria inferior compacta
        local labelColor = criticalCount > 0 and "#FF4466" or (hostileCount > 0 and "#FFAA33" or "#00FFAA")
        local clsText = closestHostileDist ~= nil and string.format("%.1fm", closestHostileDist) or "--"
        h.drawRect(cx - 38, cy + RADIUS + 3, 76, 12, "#99071018")
        h.drawText(cx - 35, cy + RADIUS + 5, string.format("H:%d  CLS:%s", hostileCount, clsText), labelColor, 0.72)
    end
}
