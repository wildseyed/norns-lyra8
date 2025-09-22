-- LYRA-8 for Norns - Single File Version
-- A recreation of the Soma Labs Lyra-8 synthesizer
-- Based on MikeMorenoDSP LIRA-8 Pure Data implementation
-- 
-- ⚠️  DEVELOPMENT VERSION - UNTESTED ON HARDWARE
-- ⚠️  Single file version for easier testing
--
-- E1: Navigate hierarchy levels
-- E2: Select parameter 
-- E3: Adjust parameter value
-- K1: Hold for secondary functions
-- K2: Enter/exit edit mode
-- K3: Reset parameter to default
--
-- v1.0-dev-single @norns-lyra8

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

-- Engine state tracking
local engine_state = {
    loaded = false,
    voices = {false, false, false, false, false, false, false, false},
    voice_activity = {0, 0, 0, 0, 0, 0, 0, 0}
}

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

-- Parameter system initialization
local function init_params()
    print("LYRA-8: Loading parameter system...")
    
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
    
    print("LYRA-8: Loaded " .. params.count .. " parameters")
end

-- Engine interface functions
local function init_engine()
    print("LYRA-8: Initializing engine interface...")
    
    -- Engine loaded callback
    engine.loaded = function()
        print("LYRA-8: Engine loaded successfully")
        engine_state.loaded = true
        init_default_engine_state()
    end
    
    -- Set up voice activity callbacks (for visual feedback)
    for voice = 1, 8 do
        local callback_name = "voice" .. voice .. "Activity"
        engine[callback_name] = function(level)
            engine_state.voice_activity[voice] = level
        end
    end
end

local function init_default_engine_state()
    if not engine_state.loaded then return end
    
    -- Initialize all synthesis components
    engine.startSynthesis()
    
    -- Set default parameters
    engine.drive(17)
    engine.volume(100)
    engine.delMix(68)
    engine.feedback(54)
    engine.time1(95)
    engine.time2(57)
    engine.mod1(41) 
    engine.mod2(48)
    engine.delMod(2)
    engine.lfoRate(23)
    engine.lfoWav(0)
    engine.lfoAmnt(64)
    
    -- Initialize voice defaults
    for voice = 1, 8 do
        engine["pitch" .. voice](64)
        engine["mod" .. voice](0)
        engine["fine" .. voice](64)
        engine["sens" .. voice](0)
        engine["velA" .. voice](64)
        engine["velR" .. voice](64)
        engine["dist" .. voice](0)
        engine["hold" .. voice](0)
        engine["vib" .. voice](0)
    end
    
    print("LYRA-8: Engine initialized with defaults")
end

-- Engine parameter interface
local function set_engine_param(param_id, value)
    if not engine_state.loaded then
        print("LYRA-8: Engine not loaded, parameter ignored:", param_id, value)
        return
    end
    
    -- Call appropriate engine command
    if engine[param_id] then
        engine[param_id](value)
    else
        print("LYRA-8: Unknown parameter:", param_id)
    end
end

-- UI Drawing functions
local function draw_ui()
    screen.clear()
    
    -- Draw title bar
    screen.level(15)
    screen.move(2, 8)
    screen.text("LYRA-8")
    
    -- Show current voice if in voice mode
    if app.current_level == 4 then
        screen.move(50, 8)
        screen.text("Voice " .. app.selected_voice)
    end
    
    -- Show edit mode indicator
    if app.edit_mode then
        local blink = math.floor(app.blink_timer / 10) % 2 == 0
        if blink then
            screen.level(15)
            screen.move(128 - 25, 8)
            screen.text("EDIT")
        end
    end
    
    -- Draw separator line
    screen.level(4)
    screen.move(0, 12)
    screen.line(128, 12)
    screen.stroke()
    
    -- Draw level info
    local level_names = {"Global", "Quads", "Pairs", "Voices"}
    screen.level(8)
    screen.move(2, 22)
    screen.text("Level: " .. level_names[app.current_level])
    
    -- Draw level navigation indicators
    for i = 1, 4 do
        local x = 70 + (i - 1) * 12
        local y = 22
        
        if i == app.current_level then
            screen.level(15)
            screen.rect(x - 2, y - 6, 8, 8)
            screen.fill()
        else
            screen.level(4)
            screen.rect(x - 2, y - 6, 8, 8)
            screen.stroke()
        end
        
        screen.level(i == app.current_level and 0 or 8)
        screen.move(x, y)
        screen.text(tostring(i))
    end
    
    -- Draw simple status
    screen.level(4)
    screen.move(2, 64 - 2)
    if app.k1_held then
        screen.text("K1+K2:voice K1+K3:reset")
    else
        screen.text("K2:edit K3:default")
    end
    
    screen.update()
end

-- Initialize
function init()
    print("LYRA-8 v1.0-dev-single initializing...")
    
    -- Load parameter system
    print("Loading parameter system...")
    init_params()
    
    -- Initialize engine interface
    print("Loading engine interface...")
    init_engine()
    
    -- Set up parameter callbacks
    print("Setting up parameter callbacks...")
    -- Simplified - just basic functionality for now
    
    -- Start UI refresh timer
    print("Starting UI refresh timer...")
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
    if engine_state.loaded then
        engine.stopSynthesis()
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
        local level_params = app.levels[app.current_level].params
        app.selected_param = util.wrap(app.selected_param + delta, 1, #level_params)
        app.screen_dirty = true
        
    elseif n == 3 then
        -- Basic parameter adjustment (simplified for now)
        print("Parameter adjustment:", app.current_level, app.selected_param, delta)
        app.screen_dirty = true
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
            -- K1+K3: Reset functionality
            print("Reset triggered")
        else
            -- K3: Reset current parameter
            print("Parameter reset")
        end
        app.screen_dirty = true
    end
end

-- Main redraw function
function redraw()
    draw_ui()
end

-- Debug function
function print_state()
    print("Level:", app.current_level, "Voice:", app.selected_voice, "Param:", app.selected_param)
    print("Engine loaded:", engine_state.loaded)
end
