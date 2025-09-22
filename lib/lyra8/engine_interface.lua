-- LYRA-8 Engine Interface
-- Manages communication with SuperCollider engine

local Engine = {}

-- Engine state tracking
local engine_state = {
    loaded = false,
    voices = {false, false, false, false, false, false, false, false},
    voice_activity = {0, 0, 0, 0, 0, 0, 0, 0}
}

function Engine.init()
    print("LYRA-8: Initializing engine interface...")
    
    -- Wait for engine to load
    local load_timer = metro.init()
    load_timer.time = 0.1
    load_timer.count = 50  -- 5 second timeout
    load_timer.event = function()
        if engine_state.loaded then
            load_timer:stop()
            Engine.start_synthesis()
        end
    end
    load_timer:start()
    
    -- Set up engine callbacks
    Engine.setup_callbacks()
end

function Engine.setup_callbacks()
    -- Engine loaded callback
    engine.loaded = function()
        print("LYRA-8: Engine loaded successfully")
        engine_state.loaded = true
        Engine.init_default_state()
    end
    
    -- Voice activity callbacks (for visual feedback)
    for voice = 1, 8 do
        local callback_name = "voice" .. voice .. "Activity"
        engine[callback_name] = function(level)
            engine_state.voice_activity[voice] = level
        end
    end
end

function Engine.init_default_state()
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

function Engine.start_synthesis()
    if engine_state.loaded then
        engine.startSynthesis()
        print("LYRA-8: Synthesis started")
    end
end

function Engine.stop_synthesis()
    if engine_state.loaded then
        engine.stopSynthesis()
        print("LYRA-8: Synthesis stopped")
    end
end

-- Parameter interface
function Engine.set_param(param_id, value)
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

-- Voice control
function Engine.trigger_voice(voice, sens_value)
    if voice >= 1 and voice <= 8 and engine_state.loaded then
        engine["sens" .. voice](sens_value or 127)
        engine_state.voices[voice] = true
    end
end

function Engine.release_voice(voice)
    if voice >= 1 and voice <= 8 and engine_state.loaded then
        engine["sens" .. voice](0)
        engine_state.voices[voice] = false
    end
end

function Engine.release_all_voices()
    for voice = 1, 8 do
        Engine.release_voice(voice)
    end
end

-- Activity monitoring
function Engine.get_voice_activity(voice)
    return engine_state.voice_activity[voice] or 0
end

function Engine.get_all_activity()
    return engine_state.voice_activity
end

-- System status
function Engine.is_loaded()
    return engine_state.loaded
end

function Engine.get_active_voices()
    local active = {}
    for i, voice_active in ipairs(engine_state.voices) do
        if voice_active then
            table.insert(active, i)
        end
    end
    return active
end

-- Cleanup
function Engine.cleanup()
    Engine.stop_synthesis()
    Engine.release_all_voices()
end

-- Preset management
function Engine.save_state()
    local state = {}
    
    -- Save current parameter values (would need to read from params)
    -- This is a simplified version - real implementation would save all params
    state.active_voices = engine_state.voices
    state.loaded = engine_state.loaded
    
    return state
end

function Engine.load_state(state)
    if not state or not engine_state.loaded then
        return false
    end
    
    -- Restore voice states
    for voice = 1, 8 do
        if state.active_voices and state.active_voices[voice] then
            Engine.trigger_voice(voice, 127)
        else
            Engine.release_voice(voice)
        end
    end
    
    return true
end

-- Development/debugging helpers
function Engine.print_status()
    print("LYRA-8 Engine Status:")
    print("  Loaded:", engine_state.loaded)
    print("  Active voices:", table.concat(Engine.get_active_voices(), ", "))
    
    local activity = Engine.get_all_activity()
    local activity_str = {}
    for i, level in ipairs(activity) do
        table.insert(activity_str, string.format("V%d:%.2f", i, level))
    end
    print("  Activity:", table.concat(activity_str, " "))
end

-- Emergency reset
function Engine.emergency_reset()
    print("LYRA-8: Emergency reset triggered")
    Engine.release_all_voices()
    
    -- Reset to safe defaults
    if engine_state.loaded then
        engine.drive(0)     -- No drive to prevent harsh sounds
        engine.volume(50)   -- Lower volume
        engine.feedback(0)  -- No feedback to prevent runaway
    end
end

return Engine
