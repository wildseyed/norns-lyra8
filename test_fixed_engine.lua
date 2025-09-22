-- test_fixed_engine.lua
-- Test the fixed LIRA-8 engine

engine.name = 'Lyra8'

function init()
    print("Testing fixed LIRA-8 engine...")
    
    -- Test basic voice control
    print("Setting voice 1 gate on...")
    engine.voice_gate_1(1)
    
    -- Test LFO parameters
    print("Setting LFO parameters...")
    engine.f_a(50)
    engine.f_b(70)
    
    -- Test delay parameters
    print("Setting delay parameters...")
    engine.time_1(80)
    engine.feedback(30)
    engine.del_mix(50)
    
    -- Test master volume
    print("Setting master volume...")
    engine.vol(80)
    
    print("Engine test completed - check for audio output")
    print("Press K2 to toggle voice 1")
    print("Press K3 to test voice tuning")
    
    redraw()
end

function key(n, z)
    if z == 1 then
        if n == 2 then
            -- Toggle voice 1
            local current_gate = params:get("voice_1_gate") or 0
            local new_gate = current_gate > 0 and 0 or 1
            engine.voice_gate_1(new_gate)
            print("Voice 1 gate: " .. new_gate)
            redraw()
        elseif n == 3 then
            -- Change voice 1 tuning
            local tune = math.random(40, 90)
            engine.voice_tune_1(tune)
            print("Voice 1 tune: " .. tune)
            redraw()
        end
    end
end

function redraw()
    screen.clear()
    screen.move(64, 20)
    screen.text_center("LIRA-8 Engine Test")
    
    screen.move(64, 35)
    screen.text_center("K2: Toggle Voice 1")
    
    screen.move(64, 45)
    screen.text_center("K3: Random Tune")
    
    screen.update()
end
