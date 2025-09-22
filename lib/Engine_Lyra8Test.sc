// LYRA-8 Test Engine - Minimal Version
// Simple test to verify SuperCollider integration

Engine_Lyra8Test : CroneEngine {
    
    // Simple test SynthDef
    *initClass {
        StartUp.add {
            "LYRA-8 Test Engine loaded".postln;
        }
    }
    
    alloc {
        "LYRA-8 Test Engine: alloc called".postln;
        
        // Simple test sine wave SynthDef
        SynthDef(\testSine, {
            var sig = SinOsc.ar(\freq.kr(440), 0, \amp.kr(0.1));
            Out.ar(\out.kr(0), sig ! 2);
        }).add;
        
        // Add a simple OSC command
        this.addCommand("test", "f", { |msg|
            ("LYRA-8 Test: received test command with value: " ++ msg[1]).postln;
        });
        
        this.addCommand("playTest", "f", { |msg|
            var freq = msg[1] ? 440;
            Synth(\testSine, [\freq, freq, \amp, 0.1, \out, 0]);
            ("LYRA-8 Test: playing sine at " ++ freq ++ " Hz").postln;
        });
        
        "LYRA-8 Test Engine: ready".postln;
    }
    
    free {
        "LYRA-8 Test Engine: free called".postln;
    }
}
