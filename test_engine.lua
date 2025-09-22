-- LYRA-8 Engine Test Script
-- Minimal script to test SuperCollider engine loading

engine.name = "Lyra8Test"

function init()
    print("LYRA-8 Test: Starting...")
    
    -- Simple UI
    screen.clear()
    screen.level(15)
    screen.move(10, 30)
    screen.text("LYRA-8 Engine Test")
    screen.move(10, 45)
    screen.text("K2: Test Sound")
    screen.update()
    
    print("LYRA-8 Test: Ready")
end

function key(n, z)
    if n == 2 and z == 1 then
        print("Testing engine command...")
        engine.test(math.random() * 1000)
        engine.playTest(440 + math.random() * 200)
    end
end

function cleanup()
    print("LYRA-8 Test: Cleanup")
end
