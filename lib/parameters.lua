-- LYRA-8 Parameter System
-- Manages all synthesis parameters with proper scaling and defaults

local Params = {}

-- Parameter definitions with defaults matching LIRA-8 analysis
local param_specs = {
    -- Global parameters
    drive = {min = 0, max = 127, default = 17, formatter = function(param) return param:get() .. " drive" end},
    volume = {min = 0, max = 127, default = 100, formatter = function(param) return param:get() .. " vol" end},
    delMix = {min = 0, max = 127, default = 68, formatter = function(param) return param:get() .. " mix" end},
    feedback = {min = 0, max = 127, default = 54, formatter = function(param) return param:get() .. " fb" end},
    time1 = {min = 0, max = 127, default = 95, formatter = function(param) return param:get() .. " t1" end},
    time2 = {min = 0, max = 127, default = 57, formatter = function(param) return param:get() .. " t2" end},
    mod1 = {min = 0, max = 127, default = 41, formatter = function(param) return param:get() .. " m1" end},
    mod2 = {min = 0, max = 127, default = 48, formatter = function(param) return param:get() .. " m2" end},
    delMod = {min = 0, max = 2, default = 2, formatter = function(param) 
        local modes = {"self", "off", "lfo"}
        return modes[param:get() + 1] or "off"
    end},
    lfoRate = {min = 0, max = 127, default = 23, formatter = function(param) return param:get() .. " rate" end},
    lfoWav = {min = 0, max = 1, default = 0, formatter = function(param) 
        return param:get() == 0 and "tri" or "sqr"
    end},
    lfoAmnt = {min = 0, max = 127, default = 64, formatter = function(param) return param:get() .. " amt" end},
    
    -- Voice parameters (per voice)
    pitch = {min = 0, max = 127, default = 64, formatter = function(param) return param:get() .. " hz" end},
    mod = {min = 0, max = 127, default = 0, formatter = function(param) return param:get() .. " mod" end},
    fine = {min = 0, max = 127, default = 64, formatter = function(param) return param:get() .. " fine" end},
    sens = {min = 0, max = 127, default = 0, formatter = function(param) return param:get() .. " sens" end},
    velA = {min = 0, max = 127, default = 64, formatter = function(param) return param:get() .. " att" end},
    velR = {min = 0, max = 127, default = 64, formatter = function(param) return param:get() .. " rel" end},
    dist = {min = 0, max = 127, default = 0, formatter = function(param) return param:get() .. " dist" end},
    hold = {min = 0, max = 1, default = 0, formatter = function(param) 
        return param:get() == 0 and "off" or "hold"
    end},
    vib = {min = 0, max = 127, default = 0, formatter = function(param) return param:get() .. " vib" end}
}

function Params.init()
    -- Add global parameters
    for param_name, spec in pairs(param_specs) do
        if not param_name:match("%d$") then  -- Skip voice-numbered parameters
            params:add_control(param_name, param_name, 
                controlspec.new(spec.min, spec.max, 'lin', 1, spec.default))
            params:set_formatter(param_name, spec.formatter)
        end
    end
    
    -- Add voice-specific parameters (8 voices)
    for voice = 1, 8 do
        local voice_params = {"pitch", "mod", "fine", "sens", "velA", "velR", "dist", "hold", "vib"}
        
        params:add_separator("voice" .. voice, "Voice " .. voice)
        
        for _, param_name in ipairs(voice_params) do
            local param_id = param_name .. voice
            local spec = param_specs[param_name]
            
            params:add_control(param_id, param_name .. " " .. voice,
                controlspec.new(spec.min, spec.max, 'lin', 1, spec.default))
            params:set_formatter(param_id, spec.formatter)
        end
    end
    
    -- Add pair parameters (4 pairs: 1-2, 3-4, 5-6, 7-8)
    for pair = 1, 4 do
        params:add_separator("pair" .. pair, "Pair " .. pair)
        
        -- Fine tuning for pairs
        for voice_in_pair = 1, 2 do
            local param_id = "fine" .. voice_in_pair
            local spec = param_specs.fine
            params:add_control(param_id, "fine " .. voice_in_pair,
                controlspec.new(spec.min, spec.max, 'lin', 1, spec.default))
            params:set_formatter(param_id, spec.formatter)
        end
        
        -- Sensor values for pairs  
        for voice_in_pair = 1, 2 do
            local param_id = "sens" .. voice_in_pair
            local spec = param_specs.sens
            params:add_control(param_id, "sens " .. voice_in_pair,
                controlspec.new(spec.min, spec.max, 'lin', 1, spec.default))
            params:set_formatter(param_id, spec.formatter)
        end
        
        -- Velocity attack for pairs
        for voice_in_pair = 1, 2 do
            local param_id = "velA" .. voice_in_pair  
            local spec = param_specs.velA
            params:add_control(param_id, "velA " .. voice_in_pair,
                controlspec.new(spec.min, spec.max, 'lin', 1, spec.default))
            params:set_formatter(param_id, spec.formatter)
        end
    end
    
    -- Add quad parameters (2 quads: 1-4, 5-8)
    for quad = 1, 2 do
        params:add_separator("quad" .. quad, "Quad " .. quad)
        
        -- Delay parameters for quads
        local quad_params = {"time1", "time2", "mod1", "mod2", "delMod"}
        for _, param_name in ipairs(quad_params) do
            local param_id = param_name .. quad
            local spec = param_specs[param_name]
            
            params:add_control(param_id, param_name .. " " .. quad,
                controlspec.new(spec.min, spec.max, 'lin', 1, spec.default))
            params:set_formatter(param_id, spec.formatter)
        end
    end
    
    print("LYRA-8: Loaded " .. params.count .. " parameters")
end

-- Helper to get parameter specification
function Params.get_spec(param_name)
    return param_specs[param_name]
end

-- Helper to get default value for any parameter
function Params.get_default(param_id)
    local base_name = param_id:match("^(%a+)")
    local spec = param_specs[base_name]
    return spec and spec.default or 64
end

-- Batch parameter updates (for presets/randomization)
function Params.set_voice_defaults(voice)
    local voice_params = {"pitch", "mod", "fine", "sens", "velA", "velR", "dist", "hold", "vib"}
    for _, param_name in ipairs(voice_params) do
        local param_id = param_name .. voice
        local default = Params.get_default(param_id)
        if params:lookup_param(param_id) then
            params:set(param_id, default)
        end
    end
end

function Params.randomize_voice(voice, amount)
    amount = amount or 0.3  -- 30% randomization by default
    local voice_params = {"pitch", "mod", "fine", "sens", "velA", "velR", "dist", "vib"}
    
    for _, param_name in ipairs(voice_params) do
        local param_id = param_name .. voice
        local spec = param_specs[param_name]
        if spec and params:lookup_param(param_id) then
            local range = spec.max - spec.min
            local offset = (math.random() - 0.5) * range * amount
            local new_value = util.clamp(spec.default + offset, spec.min, spec.max)
            params:set(param_id, math.floor(new_value))
        end
    end
end

return Params
