// Engine_Lyra8Simple.sc
// Simplified SuperCollider Engine for Norns - LIRA-8 Emulation
// Conservative syntax for compatibility

Engine_Lyra8Simple : CroneEngine {
    var voices;
    var voiceBus;
    
    alloc {
        "LYRA-8 Simple: Starting alloc".postln;
        
        // Allocate simple bus
        voiceBus = Bus.audio(context.server, 8);
        
        // Create simple SynthDef first
        SynthDef(\lyraVoiceSimple, { |out, freq=440, amp=0.1, gate=0|
            var sig, env;
            env = EnvGen.kr(Env.asr(0.01, 1, 0.3), gate, doneAction: 0);
            sig = SinOsc.ar(freq, 0, amp * env);
            Out.ar(out, sig ! 2);
        }).add;
        
        "LYRA-8 Simple: SynthDef added".postln;
        
        // Wait for SynthDef to load
        context.server.sync;
        
        // Add simple commands
        this.addCommand("testTone", "ff", { |msg|
            var freq = msg[1];
            var amp = msg[2];
            Synth(\lyraVoiceSimple, [
                \out, 0,
                \freq, freq,
                \amp, amp,
                \gate, 1
            ]);
            ("LYRA-8 Simple: Playing tone at " ++ freq ++ " Hz").postln;
        });
        
        this.addCommand("stopAll", "", { |msg|
            context.server.freeAll;
            "LYRA-8 Simple: Stopped all".postln;
        });
        
        // Basic voice parameters
        8.do { |i|
            var voiceNum = i + 1;
            
            this.addCommand("pitch" ++ voiceNum, "f", { |msg|
                var pitch = msg[1];
                ("LYRA-8 Simple: Voice " ++ voiceNum ++ " pitch = " ++ pitch).postln;
            });
            
            this.addCommand("sens" ++ voiceNum, "f", { |msg|
                var sens = msg[1];
                ("LYRA-8 Simple: Voice " ++ voiceNum ++ " sens = " ++ sens).postln;
            });
        };
        
        "LYRA-8 Simple: Engine ready".postln;
    }
    
    free {
        "LYRA-8 Simple: Freeing resources".postln;
        voiceBus.free;
    }
}
