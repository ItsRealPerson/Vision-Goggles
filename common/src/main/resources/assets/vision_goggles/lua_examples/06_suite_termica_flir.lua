-- [VG-BUNDLED v34]
-- ============================================================================
-- EJEMPLO 6: SUITE TERMICA FLIR (IFF IRONBOW + ESTABILIZADOR CRIOGENICO)
-- ============================================================================
-- QUE APRENDERAS CON ESTE SCRIPT:
--   1. Como funciona onThermalColor(entityType, isHostile) con el shader real
--      'thermal.fsh': evita colores azules puros (disparan coldSignal -> negro)
--      y usa la escala calida Ironbow (#00FFFF blanco critico, #FFBB00
--      amarillo-oro hostil, #FF8800 naranja calido para fauna de sangre caliente).
--   2. Filtrar entidades no-termicas (no-muertos, aranas, slimes) usando
--      e.emits_heat para no mostrar etiquetas [HOT] fantasmas.
--   3. Reducir 'Interference' en onUpdateUniforms en Nether/biomas calidos
--      y dibujar la barra calibrada Ironbow + etiquetas AR 3D.
--
-- HARDWARE: Requiere modulo "thermal" (Termico) instalado en Gafas Modulares.
-- FLASHEO:  /vg givescript thermal_palette "Suite Termica FLIR" #FF8800
-- ============================================================================

local CRITICAL_MOBS = {
    ["minecraft:player"] = true,
    ["minecraft:creeper"] = true,
    ["minecraft:warden"] = true,
    ["minecraft:ender_dragon"] = true,
    ["minecraft:blaze"] = true,
    ["minecraft:magma_cube"] = true,
    ["minecraft:evoker"] = true,
    ["minecraft:ravager"] = true
}

local IRONBOW_SCALE = {
    "#FFFFFF", -- 100% Incandescente (Critico / Blanco)
    "#FFF24D", --  85% Muy caliente (Amarillo)
    "#FF8C00", --  65% Caliente (Naranja)
    "#D91926", --  45% Tibio (Rojo)
    "#47008C", --  25% Templado (Purpura)
    "#050820"  --   0% Fondo frio (Azul marino)
}

local critCount = 0
local hostCount = 0
local passCount = 0
local coldCount = 0
local priorityTargets = {}
local cryoActive = false

local function isHotEnvironment(dim, biome)
    if dim == "the_nether" then return true end
    local b = string.lower(biome or "")
    if string.find(b, "desert", 1, true)
        or string.find(b, "badlands", 1, true)
        or string.find(b, "savanna", 1, true)
        or string.find(b, "nether", 1, true)
        or string.find(b, "basalt", 1, true)
        or string.find(b, "crimson", 1, true)
        or string.find(b, "warped", 1, true) then
        return true
    end
    return false
end

local function estimateAmbientTempC(p)
    local dim = p.getDimension()
    if dim == "the_nether" then return 68 end
    if dim == "the_end" then return -18 end

    local b = string.lower(p.getBiome() or "")
    local temp = 21
    if string.find(b, "snow", 1, true) or string.find(b, "ice", 1, true)
        or string.find(b, "frozen", 1, true) or string.find(b, "peaks", 1, true) then
        temp = -12
    elseif string.find(b, "desert", 1, true) or string.find(b, "badlands", 1, true) then
        temp = 39
    elseif string.find(b, "savanna", 1, true) or string.find(b, "jungle", 1, true) then
        temp = 31
    end

    if world.isRaining() then temp = temp - 4 end
    if not p.isDay() then temp = temp - 6 end
    if p.isInWater() then temp = temp - 10 end
    return temp
end

module = {
    id = "vision_goggles:thermal_palette",
    name = "Suite Termica FLIR",
    type = "UTILITY",
    required_module = "thermal",
    battery_drain = 0.08,

    onEnable = function(p)
        critCount, hostCount, passCount, coldCount = 0, 0, 0, 0
        priorityTargets = {}
        sound.playBeep(1.2)
    end,

    -- Paleta IFF calida Ironbow verificada contra thermal.fsh (sin tonos azules que apaguen animales)
    onThermalColor = function(entityType, isHostile)
        if CRITICAL_MOBS[entityType] then
            return "#00FFFF" -- Activa cyanBias -> Blanco Incandescente maximo
        elseif isHostile then
            return "#FFBB00" -- Hostil de sangre caliente -> Amarillo-oro intenso
        else
            return "#FF8800" -- Pasivo de sangre caliente -> Naranja calido bien visible incluso tras 1 cristal/madera
        end
    end,

    onUpdateUniforms = function(p, uniforms)
        if isHotEnvironment(p.getDimension(), p.getBiome()) then
            cryoActive = true
            uniforms.setFloat("Interference", 0.12)
        else
            cryoActive = false
        end
    end,

    onTick = function(p, batteryPct)
        local nearby = world.getEntitiesInRadius(40.0)
        if nearby == nil then
            critCount, hostCount, passCount, coldCount = 0, 0, 0, 0
            priorityTargets = {}
            return
        end

        local cCnt, hCnt, pCnt, fCnt = 0, 0, 0, 0
        local list = {}

        for i = 1, #nearby do
            local e = nearby[i]
            if e.emits_heat == false then
                fCnt = fCnt + 1
            else
                local isCrit = CRITICAL_MOBS[e.type] == true
                if isCrit then
                    cCnt = cCnt + 1
                elseif e.is_hostile then
                    hCnt = hCnt + 1
                else
                    pCnt = pCnt + 1
                end

                if isCrit or e.is_hostile then
                    list[#list + 1] = {
                        x = e.x,
                        y = e.y,
                        z = e.z,
                        height = e.height or 1.8,
                        distance = e.distance,
                        is_crit = isCrit
                    }
                end
            end
        end

        table.sort(list, function(a, b)
            if a.is_crit ~= b.is_crit then
                return a.is_crit
            end
            return a.distance < b.distance
        end)

        local top = {}
        for i = 1, math.min(#list, 4) do
            top[i] = list[i]
        end

        critCount = cCnt
        hostCount = hCnt
        passCount = pCnt
        coldCount = fCnt
        priorityTargets = top
    end,

    onRenderHud = function(h)
        local w = h.getWidth()
        local tempC = estimateAmbientTempC(player)

        -- 1. Panel superior derecho de telemetria FLIR
        local boxX = w - 126
        local boxY = 8
        h.drawRect(boxX, boxY, 118, 29, "#AA0A0814")
        local cryoTxt = cryoActive and "CRYO-DSP: ACTIVO" or "FLIR IRONBOW: OK"
        local cryoCol = cryoActive and "#00EEFF" or "#FFAA33"
        h.drawText(boxX + 5, boxY + 4, cryoTxt, cryoCol, 0.68)
        h.drawText(boxX + 5, boxY + 13, string.format("AMB: %+d C", tempC), "#FFFFFF", 0.68)
        local censusStr = coldCount > 0
                and string.format("C:%d H:%d P:%d | F:%d", critCount, hostCount, passCount, coldCount)
                or string.format("C:%d H:%d P:%d", critCount, hostCount, passCount)
        h.drawText(boxX + 5, boxY + 21, censusStr, "#FFCC66", 0.64)

        -- 2. Barra vertical calibrada Ironbow FLIR en el borde derecho
        local barX = w - 16
        local barY = 41
        local segH = 8
        h.drawRect(barX - 1, barY - 1, 10, (#IRONBOW_SCALE * segH) + 2, "#CC000000")
        for i = 1, #IRONBOW_SCALE do
            h.drawRect(barX, barY + (i - 1) * segH, 8, segH, IRONBOW_SCALE[i])
        end
        h.drawText(barX - 24, barY, "+95C", "#FFFFFF", 0.58)
        h.drawText(barX - 24, barY + (#IRONBOW_SCALE - 1) * segH, string.format("%+dC", tempC), "#77AAFF", 0.58)

        -- 3. Etiquetas AR 3D solo sobre entidades que realmente emiten firma termica
        for i = 1, #priorityTargets do
            local t = priorityTargets[i]
            local proj = world.worldToScreen(t.x, t.y + t.height + 0.35, t.z)
            if proj ~= nil and proj.visible and not proj.behind then
                local sx = math.floor(proj.x + 0.5)
                local sy = math.floor(proj.y + 0.5)
                local tag = t.is_crit and "[CRIT]" or "[HOT]"
                local col = t.is_crit and "#FFFFFF" or "#FFBB33"
                h.drawText(sx - math.floor(h.getTextWidth(tag, 0.62) / 2), sy, tag, col, 0.62)
            end
        end
    end
}
