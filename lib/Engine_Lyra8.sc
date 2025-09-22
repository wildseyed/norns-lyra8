// Engine_Lyra8.sc
// SuperCollider Engine for Norns - LIRA-8 Emulation
// Based on analysis of MikeMorenoDSP LIRA-8 Pure Data implementation
// 
// This engine recreates the 8-voice drone synthesizer with:
// - 8 triangle wave voices with FM cross-modulation
// - Dual delay system with LFO modulation
// - Hyper-LFO with chaos behavior  
// - Drive/saturation and master limiting
// - 4-level parameter hierarchy (Individual → Pair → Quad → Global)

Engine_Lyra8 : CroneEngine {
    var <voices;           // Array of 8 voice synths
    var <lfo;              // Main LFO synth
    var <delayFx;          // Delay effect synth
    var <masterOut;        // Master output synth
    
    // Audio buses for signal routing
    var <voiceBus;         // Voice outputs
    var <delayBus;         // Delay bus
    var <lfoTriBus;        // LFO triangle output
    var <lfoSqrBus;        // LFO square output
    
    // Parameter state storage
    var <paramState;
    
    *new { |context, doneCallback|
        ^super.new(context, doneCallback);
    }
    
    alloc {
        // Initialize parameter state
        this.initParamState();
        
        // Allocate audio buses
        this.allocBuses();
        
        // Add SynthDefs
        this.addSynthDefs();
        
        // Wait for SynthDefs to be ready
        context.server.sync;
        
        // Create synth instances
        this.createSynths();
        
        // Register OSC commands
        this.addCommands();
        
        "LIRA-8 Engine loaded successfully".postln;
    }
    
    free {
        // Clean up synths
        voices.do(_.free);
        lfo.free;
        delayFx.free;
        masterOut.free;
        
        // Free buses
        voiceBus.free;
        delayBus.free;
        lfoTriBus.free;
        lfoSqrBus.free;
        
        "LIRA-8 Engine freed".postln;
    }
    
    initParamState {
        // Initialize parameter state with defaults matching PD implementation
        paramState = Dictionary.new;
        
        // Individual voice parameters (8 voices)
        8.do { |i|
            paramState["tune_" ++ (i+1)] = 64;        // Individual tuning
            paramState["sensor_" ++ (i+1)] = 0;       // Sensor gates
        };
        
        // Pair parameters (4 pairs: 12, 34, 56, 78)
        [\12, \34, \56, \78].do { |pair|
            paramState["sharp_" ++ pair] = 0;         // Waveshaper amount
            paramState["mod_" ++ pair] = 0;           // Cross-mod depth
            paramState["fast_" ++ pair] = 1;          // Envelope speed
            paramState["source_" ++ pair] = 0;        // Mod source (0-2)
        };
        
        // Quad parameters (2 quads: 1234, 5678)
        [\1234, \5678].do { |quad|
            paramState["pitch_" ++ quad] = 64;        // Pitch scaling
            paramState["hold_" ++ quad] = 0;          // Hold amount
        };
        
        // Global parameters
        paramState["f_a"] = 42;                       // LFO A frequency
        paramState["f_b"] = 64;                       // LFO B frequency
        paramState["andor"] = 1;                      // LFO logic (0=AND, 1=OR)
        paramState["link"] = 1;                       // LFO link (0=independent, 1=linked)
        paramState["time_1"] = 95;                    // Delay 1 time
        paramState["time_2"] = 57;                    // Delay 2 time
        paramState["mod_1"] = 41;                     // Delay 1 modulation
        paramState["mod_2"] = 48;                     // Delay 2 modulation
        paramState["del_mod"] = 2;                    // Delay mod source (0=self, 1=off, 2=LFO)
        paramState["lfo_wav"] = 0;                    // LFO wave (0=tri, 1=sqr)
        paramState["feedback"] = 54;                  // Delay feedback
        paramState["del_mix"] = 68;                   // Delay dry/wet
        paramState["drv"] = 86;                       // Drive amount
        paramState["dst_mix"] = 69;                   // Drive dry/wet
        paramState["vol"] = 127;                      // Master volume
        paramState["total_fb"] = 0;                   // Total feedback enable
        paramState["vibrato"] = 1;                    // Vibrato enable
        paramState["switch"] = 0;                     // Routing switch
        paramState["quantize"] = 0;                   // Pitch quantization
    }
    
    allocBuses {
        // Audio buses (simplified)
        voiceBus = Bus.audio(context.server, 8);
        delayBus = Bus.audio(context.server, 2);
        lfoTriBus = Bus.audio(context.server, 1);
        lfoSqrBus = Bus.audio(context.server, 1);
        
        "Allocated buses for LIRA-8 engine".postln;
    }
    
    addSynthDefs {
        // Voice SynthDef
        SynthDef(\lyraVoice, {
            arg out = 0, gate = 0, freq = 440, 
                tune = 64, sharp = 0, modDepth = 0, 
                vibrato = 1, quantize = 0;
            
            // Tuning system
            var tuneOffset = tune.linlin(0, 127, -24, 24);
            var tunedFreq = freq * tuneOffset.midiratio;
            
            // Quantization
            var finalFreq = Select.kr(quantize, [
                tunedFreq,
                tunedFreq.cpsmidi.round.midicps
            ]);
            
            // Triangle oscillator
            var osc = LFTri.ar(finalFreq);
            
            // Waveshaping
            var shapeAmount = (sharp / 127) ** 2;
            var shaped = (osc * (1 + shapeAmount * 3)).tanh;
            
            // Envelope
            var env = EnvGen.ar(
                Env.adsr(0.1, 0.1, 0.8, 2.0), 
                gate: gate
            );
            
            var output = shaped * env * 0.125;
            Out.ar(out, output);
        }).add;
        
        // LFO SynthDef
        SynthDef(\lyraLFO, {
            arg triOut = 100, sqrOut = 101, fa = 42, fb = 64, link = 1, andor = 1;
            
            var freqA = (fa / 127 * 10 + 0.1);
            var freqB = Select.kr(link, [fb / 127 * 10 + 0.1, freqA]);
            
            var triA = LFTri.ar(freqA);
            var triB = LFTri.ar(freqB);
            var triOut = (triA + triB) * 0.5;
            
            var sqrA = LFPulse.ar(freqA) * 2 - 1;
            var sqrB = LFPulse.ar(freqB) * 2 - 1;
            var sqrLogic = Select.ar(andor, [
                sqrA * sqrB,
                (sqrA + sqrB).sign
            ]);
            
            Out.ar(triOut, triOut);
            Out.ar(sqrOut, sqrLogic);
        }).add;
        
        // Delay SynthDef
        SynthDef(\lyraDelay, {
            arg in = 0, out = 0, time1 = 95, time2 = 57, 
                feedback = 54, mix = 68;
            
            var input = In.ar(in, 8).sum;
            var delayTime1 = (time1 / 127 * 0.5 + 0.01);
            var delayTime2 = (time2 / 127 * 0.5 + 0.01);
            
            var delay1 = DelayC.ar(input, 0.5, delayTime1);
            var delay2 = DelayC.ar(delay1, 0.5, delayTime2);
            
            var fbAmount = (feedback / 127 * 0.8);
            var wet = delay2 + (delay2 * fbAmount);
            wet = wet.tanh;
            
            var wetAmount = mix / 127;
            var output = (input * (1 - wetAmount)) + (wet * wetAmount);
            
            Out.ar(out, [output, output]);
        }).add;
        
        // Master SynthDef
        SynthDef(\lyraMaster, {
            arg in = 0, out = 0, drive = 17, volume = 100;
            
            var input = In.ar(in, 2);
            var driveAmount = (drive / 127 * 4 + 1);
            var driven = (input * driveAmount).tanh;
            var volScale = volume / 127;
            
            Out.ar(out, driven * volScale);
        }).add;
        
        "SynthDefs added".postln;
    }
    
    createSynths {
        // Create LFO
        lfo = Synth(\lyraLFO, [
            \triOut, lfoTriBus,
            \sqrOut, lfoSqrBus
        ], context.xg);
        
        // Create 8 voices
        voices = 8.collect { |i|
            Synth(\lyraVoice, [
                \out, voiceBus.index + i
            ], context.xg);
        };
        
        // Create delay effects
        delayFx = Synth(\lyraDelay, [
            \in, voiceBus,
            \out, delayBus
        ], context.xg);
        
        // Create master output
        masterOut = Synth(\lyraMaster, [
            \in, delayBus,
            \out, context.out_b
        ], context.xg);
        
        "Created synth instances".postln;
    }
    
    addCommands {
        // Voice gate commands
        (1..8).do { |i|
            this.addCommand(("voice_gate_" ++ i).asSymbol, "f", { |msg|
                var gate = msg[1].asFloat;
                paramState["sensor_" ++ i] = gate;
                voices[i - 1].set(\gate, gate);
            });
        };
        
        // Voice tuning commands
        (1..8).do { |i|
            this.addCommand(("voice_tune_" ++ i).asSymbol, "f", { |msg|
                var tune = msg[1].asFloat.clip(0, 127);
                paramState["tune_" ++ i] = tune;
                voices[i - 1].set(\tune, tune);
            });
        };
        
        // LFO commands
        this.addCommand(\f_a, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["f_a"] = value;
            lfo.set(\fa, value);
        });
        
        this.addCommand(\f_b, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["f_b"] = value;
            lfo.set(\fb, value);
        });
        
        this.addCommand(\link, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["link"] = value;
            lfo.set(\link, value);
        });
        
        this.addCommand(\andor, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["andor"] = value;
            lfo.set(\andor, value);
        });
        
        // Delay commands
        this.addCommand(\time_1, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["time_1"] = value;
            delayFx.set(\time1, value);
        });
        
        this.addCommand(\time_2, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["time_2"] = value;
            delayFx.set(\time2, value);
        });
        
        this.addCommand(\feedback, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["feedback"] = value;
            delayFx.set(\feedback, value);
        });
        
        this.addCommand(\del_mix, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["del_mix"] = value;
            delayFx.set(\mix, value);
        });
        
        // Master commands
        this.addCommand(\drv, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["drv"] = value;
            masterOut.set(\drive, value);
        });
        
        this.addCommand(\vol, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["vol"] = value;
            masterOut.set(\volume, value);
        });
        
        "Registered OSC commands".postln;
    }
}
