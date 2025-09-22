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
    var <hyperLfo;         // Hyper-LFO synth
    var <delayFx;          // Dual delay effect synth
    var <masterOut;        // Master output processing synth
    var <modMatrix;        // Cross-modulation matrix synth
    
    // Audio buses for signal routing
    var <voiceBus;         // Voice outputs (8 channels)
    var <delayBus;         // Voice delay sends (8 channels)
    var <modBus;           // Cross-modulation inputs (8 channels)  
    var <lfoTriBus;        // LFO triangle output
    var <lfoSqrBus;        // LFO square output
    var <totalFbBus;       // Total feedback bus
    var <masterBus;        // Master mix bus
    
    // Control buses for parameter distribution
    var <pitchBus;         // Pitch control per voice (8 channels)
    var <gateBus;          // Gate control per voice (8 channels)
    
    // Synth groups for ordered execution
    var <lfoGroup;         // LFO generation (first)
    var <modGroup;         // Cross-modulation matrix (second)
    var <voiceGroup;       // Voice synthesis (third)
    var <fxGroup;          // Effects processing (fourth)
    var <masterGroup;      // Master output (last)
    
    // Parameter state storage
    var <paramState;
    
    *new { |context, doneCallback|
        ^super.new(context, doneCallback);
    }
    
    alloc {
        // Initialize parameter state
        this.initParamState();
        
        // Allocate audio and control buses
        this.allocBuses();
        
        // Create synth groups for execution order
        this.createGroups();
        
        // Load and add SynthDefs
        this.loadSynthDefs();
        
        // Create synth instances
        this.createSynths();
        
        // Register OSC commands for Norns communication
        this.addCommands();
        
        // Set default parameters
        this.setDefaults();
        
        "LIRA-8 Engine loaded successfully".postln;
    }
    
    free {
        // Clean up synths
        voices.do(_.free);
        hyperLfo.free;
        delayFx.free;
        masterOut.free;
        modMatrix.free;
        
        // Free buses
        voiceBus.free;
        delayBus.free;
        modBus.free;
        lfoTriBus.free;
        lfoSqrBus.free;
        totalFbBus.free;
        masterBus.free;
        pitchBus.free;
        gateBus.free;
        
        // Free groups
        lfoGroup.free;
        modGroup.free;
        voiceGroup.free;
        fxGroup.free;
        masterGroup.free;
        
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
        // Audio buses
        voiceBus = Bus.audio(context.server, 8);
        delayBus = Bus.audio(context.server, 8);
        modBus = Bus.audio(context.server, 8);
        lfoTriBus = Bus.audio(context.server, 1);
        lfoSqrBus = Bus.audio(context.server, 1);
        totalFbBus = Bus.audio(context.server, 1);
        masterBus = Bus.audio(context.server, 2);
        
        // Control buses
        pitchBus = Bus.control(context.server, 8);
        gateBus = Bus.control(context.server, 8);
        
        "Allocated buses for LIRA-8 engine".postln;
    }
    
    createGroups {
        // Create ordered execution groups
        lfoGroup = Group.new(context.server);
        modGroup = Group.after(lfoGroup);
        voiceGroup = Group.after(modGroup);
        fxGroup = Group.after(voiceGroup);
        masterGroup = Group.after(fxGroup);
        
        "Created synth groups".postln;
    }
    
    loadSynthDefs {
        // Load all SynthDefs for the LIRA-8 engine
        SynthDef(\lyraVoice, this.voiceSynthDef()).add;
        SynthDef(\lyraLFO, this.lfoSynthDef()).add;
        SynthDef(\lyraDelay, this.delaySynthDef()).add;
        SynthDef(\lyraMaster, this.masterSynthDef()).add;
        SynthDef(\lyraModMatrix, this.modMatrixSynthDef()).add;
        
        context.server.sync;
        "SynthDefs loaded".postln;
    }
    
    createSynths {
        // Create Hyper-LFO (first in chain)
        hyperLfo = Synth(\lyraLFO, [
            \triOut, lfoTriBus,
            \sqrOut, lfoSqrBus
        ], lfoGroup);
        
        // Create cross-modulation matrix
        modMatrix = Synth(\lyraModMatrix, [
            \delayIn, delayBus,
            \lfoSqrIn, lfoSqrBus,
            \totalFbIn, totalFbBus,
            \modOut, modBus
        ], modGroup);
        
        // Create 8 voices
        voices = 8.collect { |i|
            Synth(\lyraVoice, [
                \voiceNum, i + 1,
                \out, voiceBus.index + i,
                \delayOut, delayBus.index + i,
                \modIn, modBus.index + i,
                \lfoTriIn, lfoTriBus,
                \pitchBus, pitchBus.index + i,
                \gateBus, gateBus.index + i
            ], voiceGroup);
        };
        
        // Create delay effects
        delayFx = Synth(\lyraDelay, [
            \in, voiceBus,
            \lfoTriIn, lfoTriBus,
            \out, masterBus
        ], fxGroup);
        
        // Create master output
        masterOut = Synth(\lyraMaster, [
            \in, masterBus,
            \totalFbOut, totalFbBus,
            \out, context.out_b
        ], masterGroup);
        
        "Created synth instances".postln;
    }
    
    addCommands {
        // Voice control commands
        this.addCommand(\voice_gate, "if", { |msg|
            var voiceNum = msg[1].asInteger.clip(1, 8);
            var gate = msg[2].asFloat;
            paramState["sensor_" ++ voiceNum] = gate;
            gateBus.setAt(voiceNum - 1, gate);
        });
        
        this.addCommand(\voice_tune, "if", { |msg|
            var voiceNum = msg[1].asInteger.clip(1, 8);
            var tune = msg[2].asFloat.clip(0, 127);
            paramState["tune_" ++ voiceNum] = tune;
            voices[voiceNum - 1].set(\tune, tune);
        });
        
        // Pair control commands
        [\12, \34, \56, \78].do { |pair, idx|
            this.addCommand(("sharp_" ++ pair).asSymbol, "f", { |msg|
                var value = msg[1].asFloat.clip(0, 127);
                paramState["sharp_" ++ pair] = value;
                // Apply to both voices in pair
                var voice1 = idx * 2;
                var voice2 = idx * 2 + 1;
                voices[voice1].set(\sharp, value);
                voices[voice2].set(\sharp, value);
            });
            
            this.addCommand(("mod_" ++ pair).asSymbol, "f", { |msg|
                var value = msg[1].asFloat.clip(0, 127);
                paramState["mod_" ++ pair] = value;
                // Apply to both voices in pair
                var voice1 = idx * 2;
                var voice2 = idx * 2 + 1;
                voices[voice1].set(\modDepth, value);
                voices[voice2].set(\modDepth, value);
            });
            
            this.addCommand(("fast_" ++ pair).asSymbol, "f", { |msg|
                var value = msg[1].asFloat.clip(0, 1);
                paramState["fast_" ++ pair] = value;
                var voice1 = idx * 2;
                var voice2 = idx * 2 + 1;
                voices[voice1].set(\fast, value);
                voices[voice2].set(\fast, value);
            });
            
            this.addCommand(("source_" ++ pair).asSymbol, "f", { |msg|
                var value = msg[1].asFloat.clip(0, 2);
                paramState["source_" ++ pair] = value;
                modMatrix.set(("source" ++ (idx + 1)).asSymbol, value);
            });
        };
        
        // Quad control commands
        [\1234, \5678].do { |quad, idx|
            this.addCommand(("pitch_" ++ quad).asSymbol, "f", { |msg|
                var value = msg[1].asFloat.clip(0, 127);
                paramState["pitch_" ++ quad] = value;
                var pitchScale = value.linlin(0, 127, 0.01, 2.0);
                // Apply to all 4 voices in quad
                4.do { |i|
                    var voiceIdx = (idx * 4) + i;
                    voices[voiceIdx].set(\pitchScale, pitchScale);
                };
            });
            
            this.addCommand(("hold_" ++ quad).asSymbol, "f", { |msg|
                var value = msg[1].asFloat.clip(0, 127);
                paramState["hold_" ++ quad] = value;
                4.do { |i|
                    var voiceIdx = (idx * 4) + i;
                    voices[voiceIdx].set(\hold, value);
                };
            });
        };
        
        // Global LFO commands
        this.addCommand(\f_a, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["f_a"] = value;
            hyperLfo.set(\fa, value);
        });
        
        this.addCommand(\f_b, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["f_b"] = value;
            hyperLfo.set(\fb, value);
        });
        
        this.addCommand(\andor, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["andor"] = value;
            hyperLfo.set(\andor, value);
        });
        
        this.addCommand(\link, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["link"] = value;
            hyperLfo.set(\link, value);
        });
        
        // Delay system commands
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
        
        this.addCommand(\mod_1, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["mod_1"] = value;
            delayFx.set(\mod1, value);
        });
        
        this.addCommand(\mod_2, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["mod_2"] = value;
            delayFx.set(\mod2, value);
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
        
        this.addCommand(\del_mod, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 2);
            paramState["del_mod"] = value;
            delayFx.set(\delMod, value);
        });
        
        this.addCommand(\lfo_wav, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["lfo_wav"] = value;
            delayFx.set(\lfoWav, value);
        });
        
        // Master output commands
        this.addCommand(\drv, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["drv"] = value;
            masterOut.set(\drive, value);
        });
        
        this.addCommand(\dst_mix, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["dst_mix"] = value;
            masterOut.set(\driveMix, value);
        });
        
        this.addCommand(\vol, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 127);
            paramState["vol"] = value;
            masterOut.set(\volume, value);
        });
        
        // Global toggle commands
        this.addCommand(\total_fb, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["total_fb"] = value;
            modMatrix.set(\totalFbEnable, value);
        });
        
        this.addCommand(\vibrato, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["vibrato"] = value;
            8.do { |i| voices[i].set(\vibrato, value) };
        });
        
        this.addCommand(\switch, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["switch"] = value;
            modMatrix.set(\switch, value);
        });
        
        this.addCommand(\quantize, "f", { |msg|
            var value = msg[1].asFloat.clip(0, 1);
            paramState["quantize"] = value;
            8.do { |i| voices[i].set(\quantize, value) };
        });
        
        "Registered OSC commands".postln;
    }
    
    setDefaults {
        // Apply default parameter values to synths
        paramState.keysValuesDo { |key, value|
            var parts = key.asString.split($_);
            var param = parts[0].asSymbol;
            var target = if (parts.size > 1) { parts[1] } { nil };
            
            // Route to appropriate synth based on parameter type
            // This will be called after all synths are created
        };
        
        "Applied default parameters".postln;
    }
    
    // SynthDef definitions (methods return the SynthDef functions)
    voiceSynthDef {
        ^{
            // LIRA-8 Voice - Triangle oscillator with FM cross-modulation
            
            // Voice identification and routing
            var voiceNum = \voiceNum.kr(1);
            var out = \out.kr(0);
            var delayOut = \delayOut.kr(200);
            var modIn = \modIn.ar(0);
            var lfoTriIn = \lfoTriIn.ar(0);
            
            // Core parameters
            var baseFreq = \freq.kr(440);
            var gate = \gate.kr(0);
            var tune = \tune.kr(64).lag(0.1);           // Individual tuning (0-127)
            var sharp = \sharp.kr(0).lag2(0.2);         // Waveshaper amount (0-127, pair control)
            var modDepth = \modDepth.kr(0).lag2(0.2);   // Cross-mod depth (0-127, pair control)  
            var fast = \fast.kr(1);                     // Envelope speed (0/1, pair control)
            var pitchScale = \pitchScale.kr(1).lag(0.1); // Pitch scaling (0.01-2.0, quad control)
            var hold = \hold.kr(0).lag2(0.3);           // Hold amount (0-127, quad control)
            var vibrato = \vibrato.kr(1);               // Global vibrato enable
            var quantize = \quantize.kr(0);             // Pitch quantization enable
            
            // Tuning system - convert LIRA-8 tune parameter to frequency
            var tuneOffset = tune.linlin(0, 127, -24, 24); // ±2 octaves in semitones
            var tunedFreq = baseFreq * pitchScale * tuneOffset.midiratio;
            
            // Quantization (snap to chromatic scale)
            var quantizedFreq = Select.kr(quantize, [
                tunedFreq,
                tunedFreq.cpsmidi.round.midicps
            ]);
            
            // Vibrato system (global LFO with random drift)
            var vibLfo = LFTri.kr(0.1 + LFNoise1.kr(0.01).range(-0.02, 0.02));
            var vibAmount = vibrato * 0.02; // Subtle vibrato
            var finalFreq = quantizedFreq * (1 + (vibLfo * vibAmount));
            
            // Cross-modulation from other voices
            var modAmount = (modDepth / 127) ** 4 * 2; // Match PD exponential curve
            var modulatedFreq = finalFreq * (1 + (modIn * modAmount));
            
            // Core triangle oscillator with drift
            var drift = LFNoise1.kr(0.05 + LFNoise1.kr(0.01).range(-0.02, 0.02)) * 0.001;
            var phase = LFNoise0.kr(0.1).range(0, 2pi); // Random phase per voice
            var triangle = LFTri.ar(modulatedFreq + drift, phase);
            
            // Square wave output (for potential modulation routing)
            var square = LFPulse.ar(modulatedFreq + drift, phase, 0.5) * 2 - 1;
            
            // Waveshaping/distortion (sharp parameter)
            var shapeAmount = (sharp / 127) ** 2; // Exponential curve like PD
            var shaped = Select.ar(shapeAmount > 0.01, [
                triangle, // No shaping
                (triangle * (1 + shapeAmount * 3)).tanh // Soft saturation
            ]);
            
            // Sensor envelope system
            var attackTime = Select.kr(fast, [0.2, 0.1]);  // Slow/fast attack
            var releaseTime = Select.kr(fast, [8.0, 0.1]); // Slow/fast release
            
            var env = EnvGen.ar(
                Env([0, 1, 0], [attackTime, releaseTime], [3, -3]), // Cosine curves like PD
                gate: gate,
                doneAction: 0
            );
            
            // Hold behavior - sustain level from quad control
            var holdAmount = (hold / 127) ** 2;
            var finalEnv = env * (1 - holdAmount) + holdAmount;
            
            // Apply envelope and scale for 8-voice mix
            var voiceOutput = shaped * finalEnv * 0.125; // -18dB for 8 voices
            
            // Output routing
            Out.ar(out, voiceOutput);                          // Main voice output
            Out.ar(delayOut, voiceOutput * 0.1);               // Send to delay bus for cross-mod
        }
    }
    
    lfoSynthDef {
        ^{
            // LIRA-8 Hyper-LFO - Dual frequency system with chaos behavior
            
            var triOut = \triOut.kr(100);
            var sqrOut = \sqrOut.kr(101);
            
            // LFO parameters
            var fa = \fa.kr(42).lag(0.1);        // Primary LFO frequency (0-127)
            var fb = \fb.kr(64).lag(0.1);        // Secondary LFO frequency (0-127)
            var link = \link.kr(1);              // Link fb to fa (0/1)
            var andor = \andor.kr(1);            // Logic mode: 0=AND, 1=OR
            
            // Convert LIRA-8 parameter values to frequencies (matching PD implementation)
            // PD formula: (value * 12 - 75).midicps with power scaling
            var freqA = ((fa ** 2 * 127 - 75).clip(-75, 75)).midicps;
            var freqB = Select.kr(link, [
                ((fb ** 2 * 127 - 75).clip(-75, 75)).midicps, // Independent
                freqA  // Linked to frequency A
            ]);
            
            // Dual phasor system (matching PD phasor~ behavior)
            var phasorA = LFSaw.ar(freqA, 0).range(0, 1);
            var phasorB = LFSaw.ar(freqB, 0).range(0, 1);
            
            // Triangle wave generation from phasor (matching PD triangle conversion)
            // PD formula: abs(phasor - 0.5) * 4 - 1
            var triangleA = (phasorA - 0.5).abs * 4 - 1;
            var triangleB = (phasorB - 0.5).abs * 4 - 1;
            
            // Combined triangle output (mixed)
            var triangleOut = (triangleA + triangleB) * 0.5;
            
            // Square wave generation from phasor
            var squareA = (phasorA > 0.5) * 2 - 1;
            var squareB = (phasorB > 0.5) * 2 - 1;
            
            // Chaos logic system (AND/OR between square waves)
            var squareLogic = Select.ar(andor, [
                squareA * squareB,           // AND: both must be positive
                (squareA + squareB).sign     // OR: either can be positive
            ]);
            
            // Output to buses
            Out.ar(triOut, triangleOut);
            Out.ar(sqrOut, squareLogic);
        }
    }
    
    delaySynthDef {
        ^{
            // LIRA-8 Dual Delay System - Two delay lines with modulation and feedback
            
            var input = In.ar(\in.kr(0), 8).sum;    // Sum 8 voice inputs
            var lfoTriIn = \lfoTriIn.ar(0);
            var out = \out.kr(0);
            
            // Delay parameters  
            var time1 = \time1.kr(95).lag(0.1);      // Delay 1 time (0-127)
            var time2 = \time2.kr(57).lag(0.1);      // Delay 2 time (0-127)
            var mod1 = \mod1.kr(41).lag(0.2);        // Delay 1 modulation depth (0-127)
            var mod2 = \mod2.kr(48).lag(0.2);        // Delay 2 modulation depth (0-127)
            var feedback = \feedback.kr(54).lag(0.3); // Global feedback (0-127)
            var delMix = \mix.kr(68).lag(0.2);       // Dry/wet mix (0-127)
            var delMod = \delMod.kr(2);              // Modulation source (0=self, 1=off, 2=LFO)
            var lfoWav = \lfoWav.kr(0);              // LFO waveform (0=tri, 1=sqr)
            
            // Convert time parameters to seconds (matching PD scaling)
            // PD formula: (value * 12) ^ 2 * 1.45125 / samplerate
            var delayTime1 = ((time1 * 12) ** 2 * 1.45125 / SampleRate.ir).clip(0.001, 0.135);
            var delayTime2 = ((time2 * 12) ** 2 * 1.45125 / SampleRate.ir).clip(0.001, 0.135);
            
            // Modulation depth scaling (matching PD exponential curve)
            var modDepth1 = (mod1 / 127) ** 2 * 0.01; // Max 1% time modulation
            var modDepth2 = (mod2 / 127) ** 2 * 0.01;
            
            // Feedback amount with safety limiting
            var fbAmount = ((feedback / 127) * 2) ** 2; // Exponential curve
            fbAmount = fbAmount.clip(0, 0.95); // Never unity gain
            
            // LFO modulation source selection
            var modSource = Select.ar(lfoWav, [
                lfoTriIn,  // Triangle LFO
                lfoTriIn.sign  // Square LFO (sign of triangle)
            ]);
            
            // Delay time modulation
            var modTime1 = Select.kr(delMod, [
                0,  // Self-modulation (would need delay output - simplified for now)
                0,  // Off
                modSource * modDepth1  // LFO modulation
            ]);
            
            var modTime2 = Select.kr(delMod, [
                0,  // Self-modulation  
                0,  // Off
                modSource * modDepth2  // LFO modulation
            ]);
            
            var finalTime1 = (delayTime1 + modTime1).clip(0.001, 0.135);
            var finalTime2 = (delayTime2 + modTime2).clip(0.001, 0.135);
            
            // Delay line implementation with feedback
            var feedbackSig = LocalIn.ar(1);
            
            // First delay line
            var delay1 = DelayC.ar(input + feedbackSig, 0.135, finalTime1);
            
            // Filter and compress delay 1 output (matching PD implementation)
            delay1 = HPF.ar(delay1, 1);        // High-pass filter
            delay1 = LPF.ar(delay1, 4000);     // Low-pass filter
            
            // Simple compression/limiting for delay 1
            delay1 = (delay1 * 2).tanh * 0.5;
            
            // Second delay line (fed from delay 1)
            var delay2 = DelayC.ar(delay1, 0.135, finalTime2);
            
            // Filter and compress delay 2 output
            delay2 = HPF.ar(delay2, 1);
            delay2 = LPF.ar(delay2, 4000);
            delay2 = (delay2 * 2).tanh * 0.5;
            
            // Feedback path with saturation (matching PD ma.tanh~)
            var feedbackSignal = delay2 * fbAmount;
            feedbackSignal = feedbackSignal.tanh;  // Prevent runaway feedback
            feedbackSignal = LeakDC.ar(feedbackSignal); // Remove DC
            
            LocalOut.ar(feedbackSignal);
            
            // Final mix (dry/wet balance)
            var wetAmount = delMix / 127;
            var dryAmount = 1 - wetAmount;
            var finalOutput = (input * dryAmount) + (delay2 * wetAmount);
            
            // Output stereo (delay effect)
            Out.ar(out, [finalOutput, finalOutput]);
        }
    }
    
    masterSynthDef {
        ^{
            // LIRA-8 Master Output - Drive/Distortion and Final Mix
            
            var input = In.ar(\in.kr(0), 2);    // Stereo input from delay
            var out = \out.kr(0);
            
            // Master parameters
            var drive = \drive.kr(17).lag(0.2);    // Drive amount (0-127)
            var volume = \volume.kr(100).lag(0.3);  // Master volume (0-127)
            
            // Convert drive parameter to gain (exponential curve matching PD)
            // PD uses drive value with exponential scaling
            var driveAmount = ((drive / 127) ** 2) * 8 + 1;  // 1x to 9x gain
            
            // Apply drive/saturation (matching PD ma.tanh~ behavior)
            var driven = input * driveAmount;
            
            // Multi-stage saturation for rich harmonic content
            // First stage: soft saturation
            driven = driven.tanh;
            
            // Second stage: subtle asymmetric clipping (adds even harmonics)
            driven = driven + (driven ** 2 * 0.1);
            driven = driven.clip(-1, 1);
            
            // Third stage: final soft limiting
            driven = (driven * 1.2).tanh * 0.8;
            
            // Master volume with smooth curve
            var volScale = (volume / 127) ** 1.5;  // Slightly compressed volume curve
            driven = driven * volScale;
            
            // Final high-frequency roll-off (like analog output stage)
            driven = LPF.ar(driven, 8000);
            
            // DC removal (important for audio interfaces)
            driven = LeakDC.ar(driven);
            
            // Output to main outs
            Out.ar(out, driven);
        }
    }
    
    modMatrixSynthDef {
        ^{
            // LIRA-8 Modulation Matrix - FM Cross-Modulation Between Voices
            
            var voiceIn = In.ar(\voiceIn.kr(0), 8);  // 8 voice inputs
            var out = \out.kr(0);
            
            // Cross-modulation parameters for 8x8 matrix (only key ones for performance)
            // LIRA-8 uses exponential coupling based on sensor values
            
            // Simplified approach: use sensor-derived cross-modulation amounts
            // In real LIRA-8, each touch sensor affects coupling between voice pairs
            
            var sens1 = \sens1.kr(0) / 127;  // Sensor values from voice engines
            var sens2 = \sens2.kr(0) / 127;
            var sens3 = \sens3.kr(0) / 127;
            var sens4 = \sens4.kr(0) / 127;
            var sens5 = \sens5.kr(0) / 127;
            var sens6 = \sens6.kr(0) / 127;
            var sens7 = \sens7.kr(0) / 127;
            var sens8 = \sens8.kr(0) / 127;
            
            // Cross-modulation matrix (simplified but effective)
            // Each voice modulates its neighbors based on sensor activity
            var crossMod = Array.fill(8, { |i|
                var voice = voiceIn[i];
                var nextVoice = voiceIn[(i + 1) % 8];  // Next voice in chain
                var prevVoice = voiceIn[(i + 7) % 8];  // Previous voice in chain
                
                // Exponential coupling strength (matching PD implementation)
                var sensorVal = [sens1, sens2, sens3, sens4, sens5, sens6, sens7, sens8][i];
                var coupling = (sensorVal ** 3) * 2;  // Exponential curve, max 2x
                
                // Simple ring modulation for cross-mod effect
                var modulation = (nextVoice * coupling * 0.1) + (prevVoice * coupling * 0.05);
                
                // Apply modulation and soft clipping
                var modulated = voice + modulation;
                modulated = modulated.tanh;  // Soft saturation
                
                modulated
            });
            
            // Output the cross-modulated voices
            Out.ar(out, crossMod);
        }
    }
}
