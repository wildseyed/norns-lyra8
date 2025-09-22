-- LYRA-8 Simple Engine Test
-- Test script for simplified SuperCollider engine

engine.name = "Lyra8Simple"

local test_freq = 440

function init()
    print("LYRA-8 Simple Test: Starting...")
    
    -- Basic UI
    screen.clear()
    screen.level(15)
    screen.move(10, 20)
    screen.text("LYRA-8 Simple Test")
    screen.move(10, 35)
    screen.text("K2: Test Tone")
    screen.move(10, 45)
    screen.text("K3: Stop All")
    screen.move(10, 55)
    screen.text("E2: Change Freq")
    screen.update()
    
    print("LYRA-8 Simple Test: Ready")
end

function enc(n, delta)
    if n == 2 then
        test_freq = test_freq + (delta * 10)
        test_freq = math.max(100, math.min(1000, test_freq))
        print("Test frequency:", test_freq)
        
        screen.clear()
        screen.level(15)
        screen.move(10, 20)
        screen.text("LYRA-8 Simple Test")
        screen.move(10, 35)
        screen.text("Freq: " .. math.floor(test_freq) .. " Hz")
        screen.update()
    end
end

function key(n, z)
    if z == 1 then
        if n == 2 then
            print("Testing simple tone at", test_freq, "Hz")
            engine.testTone(test_freq, 0.1)
        elseif n == 3 then
            print("Stopping all sounds")
            engine.stopAll()
        end
    end
end

function cleanup()
    print("LYRA-8 Simple Test: Cleanup")
    engine.stopAll()
end
