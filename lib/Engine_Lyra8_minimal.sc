// LYRA-8 Engine - Minimal Test Version
// A minimal SuperCollider engine to test basic functionality

Engine_Lyra8 : CroneEngine {
    
    // Test variables
    var <synths;
    var <busses;
    
    *new { arg context, doneCallback;
        ^super.new(context, doneCallback);
    }
    
    alloc {
        
        // Simple test synth
        SynthDef(\lyra8_test, {
            arg out = 0, freq = 440, amp = 0.1, gate = 1;
            var sig, env;
            
            env = EnvGen.kr(Env.asr(0.1, 1, 0.1), gate, doneAction: 2);
            sig = SinOsc.ar(freq) * amp * env;
            
            Out.ar(out, sig ! 2);
        }).add;
        
        // Initialize synths array
        synths = Array.fill(8, nil);
        
        // Wait for SynthDef to load
        context.server.sync;
        
        // Add OSC commands - start with just basic ones
        this.addCommand("test", "f", { arg msg;
            ("LYRA-8: Test command received: " ++ msg[1]).postln;
        });
        
        this.addCommand("startSynthesis", "", { 
            "LYRA-8: Start synthesis called".postln;
        });
        
        this.addCommand("stopSynthesis", "", { 
            "LYRA-8: Stop synthesis called".postln;
            // Stop all synths
            synths.do({ arg synth;
                if(synth.notNil, {
                    synth.release;
                });
            });
        });
        
        // Basic parameter commands
        this.addCommand("drive", "f", { arg msg;
            ("LYRA-8: Drive set to " ++ msg[1]).postln;
        });
        
        this.addCommand("volume", "f", { arg msg;
            ("LYRA-8: Volume set to " ++ msg[1]).postln;
        });
        
        // Voice parameters (just log for now)
        (1..8).do({ arg voice;
            this.addCommand("pitch" ++ voice, "f", { arg msg;
                ("LYRA-8: Voice " ++ voice ++ " pitch set to " ++ msg[1]).postln;
            });
            
            this.addCommand("sens" ++ voice, "f", { arg msg;
                var sens = msg[1];
                ("LYRA-8: Voice " ++ voice ++ " sens set to " ++ sens).postln;
                
                // Simple test: trigger a sine wave when sens > 0
                if(sens > 0, {
                    if(synths[voice-1].notNil, {
                        synths[voice-1].release;
                    });
                    synths[voice-1] = Synth(\lyra8_test, [
                        \freq, 200 + (voice * 50), 
                        \amp, sens / 127 * 0.1
                    ]);
                }, {
                    if(synths[voice-1].notNil, {
                        synths[voice-1].release;
                        synths[voice-1] = nil;
                    });
                });
            });
        });
        
        "LYRA-8: Minimal engine loaded successfully".postln;
    }
    
    free {
        // Clean up
        synths.do({ arg synth;
            if(synth.notNil, {
                synth.free;
            });
        });
    }
}
