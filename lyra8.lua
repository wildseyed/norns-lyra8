-- LYRA-8 for Norns
-- A recreation of the Soma Labs Lyra-8 synthesizer
-- Based on MikeMorenoDSP LIRA-8 Pure Data implementation
-- 
-- E1: Navigate hierarchy levels
-- E2: Select parameter 
-- E3: Adjust parameter value
-- K1: Hold for secondary functions
-- K2: Enter/exit edit mode
-- K3: Reset parameter to default
--
-- v1.0 @norns-lyra8

local UI = require("lyra8/ui")
local Engine = require("lyra8/engine_interface")
local Params = require("lyra8/parameters")

engine.name = "Lyra8"

-- Global state
local app = {
    -- UI hierarchy levels: 1=Global, 2=Quads, 3=Pairs, 4=Voices
    current_level = 1,
    selected_voice = 1,
    selected_param = 1,
    edit_mode = false,
    k1_held = false,
    
    -- Parameter organization
    levels = {
        {name = "Global", params = {"drive", "volume", "delMix", "feedback", "lfoRate", "lfoWav"}},
        {name = "Quads", params = {"time1", "time2", "mod1", "mod2", "delMod"}},
        {name = "Pairs", params = {"fine1", "fine2", "sens1", "sens2", "velA1", "velA2"}},
        {name = "Voices", params = {"pitch", "mod", "fine", "sens", "velA", "velR", "dist", "hold", "vib"}}
    },
    
    -- Visual state
    screen_dirty = true,
    blink_timer = 0,
    activity = {0, 0, 0, 0, 0, 0, 0, 0}  -- Per-voice activity indicators
}

-- Initialize
function init()
    print("LYRA-8 v1.0 initializing...")
    
    -- Load parameter system
    Params.init()
    
    -- Initialize engine interface
    Engine.init()
    
    -- Initialize UI
    UI.init()
    
    -- Set up parameter callbacks
    setup_param_callbacks()
    
    -- Start UI refresh timer
    app.refresh_timer = metro.init()
    app.refresh_timer.time = 1/30  -- 30fps
    app.refresh_timer.count = -1
    app.refresh_timer.event = function()
        app.blink_timer = (app.blink_timer + 1) % 30
        if app.screen_dirty then
            redraw()
            app.screen_dirty = false
        end
    end
    app.refresh_timer:start()
    
    print("LYRA-8 ready!")
end

function cleanup()
    if app.refresh_timer then
        app.refresh_timer:stop()
    end
    Engine.cleanup()
end

-- Parameter system callbacks
function setup_param_callbacks()
    -- Voice parameters (1-8)
    for voice = 1, 8 do
        local voice_params = {"pitch", "mod", "fine", "sens", "velA", "velR", "dist", "hold", "vib"}
        for _, param in ipairs(voice_params) do
            local param_id = param .. voice
            params:set_action(param_id, function(x)
                Engine.set_param(param_id, x)
                app.screen_dirty = true
            end)
        end
    end
    
    -- Global parameters
    local global_params = {"drive", "volume", "delMix", "feedback", "time1", "time2", 
                          "mod1", "mod2", "delMod", "lfoRate", "lfoWav", "lfoAmnt"}
    for _, param in ipairs(global_params) do
        params:set_action(param, function(x)
            Engine.set_param(param, x)
            app.screen_dirty = true
        end)
    end
end

-- Encoder input
function enc(n, delta)
    if n == 1 then
        -- Navigate hierarchy levels
        app.current_level = util.clamp(app.current_level + delta, 1, 4)
        app.selected_param = 1  -- Reset parameter selection
        app.screen_dirty = true
        
    elseif n == 2 then
        -- Select parameter within current level
        local level_params = get_current_level_params()
        app.selected_param = util.wrap(app.selected_param + delta, 1, #level_params)
        app.screen_dirty = true
        
    elseif n == 3 then
        -- Adjust parameter value
        local param_id = get_current_param_id()
        if param_id then
            if app.edit_mode then
                -- Fine adjustment in edit mode
                params:delta(param_id, delta * 0.1)
            else
                -- Normal adjustment
                params:delta(param_id, delta)
            end
            app.screen_dirty = true
        end
    end
end

-- Key input
function key(n, z)
    if n == 1 then
        app.k1_held = (z == 1)
        app.screen_dirty = true
        
    elseif n == 2 and z == 1 then
        if app.k1_held then
            -- K1+K2: Change voice selection (for voice-specific params)
            if app.current_level == 4 then
                app.selected_voice = (app.selected_voice % 8) + 1
                app.screen_dirty = true
            end
        else
            -- K2: Toggle edit mode
            app.edit_mode = not app.edit_mode
            app.screen_dirty = true
        end
        
    elseif n == 3 and z == 1 then
        if app.k1_held then
            -- K1+K3: Reset all parameters to defaults
            reset_current_level_params()
        else
            -- K3: Reset current parameter to default
            local param_id = get_current_param_id()
            if param_id then
                params:set(param_id, params:get_default(param_id))
                app.screen_dirty = true
            end
        end
    end
end

-- Helper functions
function get_current_level_params()
    if app.current_level == 4 then
        -- Voice level: filter by selected voice
        return app.levels[4].params
    else
        return app.levels[app.current_level].params
    end
end

function get_current_param_id()
    local level_params = get_current_level_params()
    local param_name = level_params[app.selected_param]
    
    if not param_name then return nil end
    
    if app.current_level == 4 then
        -- Voice-specific parameter
        return param_name .. app.selected_voice
    elseif app.current_level == 3 then
        -- Pair-specific parameter (voices 1-2, 3-4, 5-6, 7-8)
        local pair = math.ceil(app.selected_voice / 2)
        if param_name:match("[12]$") then
            return param_name  -- Already has voice number
        else
            return param_name .. pair
        end
    elseif app.current_level == 2 then
        -- Quad-specific parameter (voices 1-4, 5-8)
        local quad = math.ceil(app.selected_voice / 4)
        return param_name .. quad
    else
        -- Global parameter
        return param_name
    end
end

function reset_current_level_params()
    local level_params = get_current_level_params()
    for _, param_name in ipairs(level_params) do
        local param_id
        if app.current_level == 4 then
            param_id = param_name .. app.selected_voice
        elseif app.current_level == 3 then
            local pair = math.ceil(app.selected_voice / 2)
            param_id = param_name .. pair
        elseif app.current_level == 2 then
            local quad = math.ceil(app.selected_voice / 4)
            param_id = param_name .. quad
        else
            param_id = param_name
        end
        
        if params:lookup_param(param_id) then
            params:set(param_id, params:get_default(param_id))
        end
    end
    app.screen_dirty = true
end

-- Main redraw function
function redraw()
    UI.draw(app)
end

-- MIDI input (optional)
function midi.add()
    -- Future: Add MIDI note input for triggering voices
end

-- For debugging
function print_state()
    print("Level:", app.current_level, "Voice:", app.selected_voice, "Param:", app.selected_param)
    print("Current param ID:", get_current_param_id())
end
