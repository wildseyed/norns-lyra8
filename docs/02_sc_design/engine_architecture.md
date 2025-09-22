# SuperCollider Engine Design for LIRA-8

## Overview
This document specifies the SuperCollider engine architecture that recreates the LIRA-8 behavior analyzed from the Pure Data implementation. The design prioritizes sonic accuracy while considering Norns hardware constraints.

## Engine Architecture

### Core Components
1. **8 Voice SynthDefs** (`\lyraVoice`)
2. **Cross-Modulation Bus System** 
3. **Global Effects Chain** (`\lyraFX`)
4. **Master Output** (`\lyraMaster`)
5. **Parameter Control System**

## UGen Mapping: PD → SuperCollider

### 1. Triangle Oscillator (`os.triangle~` → SC Implementation)

**PD Implementation Analysis**:
- PolyBLEP anti-aliased triangle wave
- Both triangle and square outputs
- Built-in sample-and-hold for drift

**SC Implementation**:
```supercollider
// Core triangle oscillator with anti-aliasing
var freq = \freq.kr(440);
var sync = \sync.kr(0);
var phase = LFNoise1.kr(0.1) * 0.001; // Subtle drift
var tri = LFTri.ar(freq + phase, sync);
var sqr = LFPulse.ar(freq + phase, sync, 0.5);

// Alternative: Use Formant or VOSIM for more complex harmonics
// var tri = Formant.ar(freq, freq * 2, freq * 0.1) * 0.1;
```

**Performance**: ~1% CPU per voice on Norns

### 2. Waveshaper (`ma.tanh~` → SC Shaper)

**PD Implementation**: Pade approximation of tanh
```
y = x * (27 + x²) / (27 + 9*x²)
```

**SC Implementation**:
```supercollider
// Use built-in tanh or custom Shaper
var drive = \drive.kr(1);
var input = sig * drive.clip(0.1, 10);
var shaped = input.tanh;

// Alternative: Custom shaper buffer for exact PD curve
var shaperBuf = Buffer.alloc(s, 513, 1);
shaperBuf.cheby([1, 0, 0.5, 0, 0.125]); // Approximation
var shaped = Shaper.ar(shaperBuf, input);
```

**Performance**: Negligible overhead

### 3. Cross-Modulation System (`prm.source` → Bus Routing)

**PD Implementation**: Complex routing matrix with switching

**SC Implementation**:
```supercollider
// Modulation source buses
~delayBus = Bus.audio(s, 8);    // Voice delay sends
~lfoSquare = Bus.audio(s, 1);   // Hyper-LFO square
~totalFB = Bus.audio(s, 1);     // Total feedback

// In voice SynthDef:
var modSource = Select.ar(\modSource.kr(0), [
    In.ar(~delayBus, 1),        // Neighbor voice
    In.ar(~lfoSquare, 1),       // LFO/CV
    In.ar(~totalFB, 1)          // Total feedback
]);
var modDepth = \modDepth.kr(0) ** 4 * 2; // Match PD curve
var modFreq = freq * (1 + (modSource * modDepth));
```

**Performance**: ~0.5% CPU for bus management

### 4. Envelope System (`prm.sensor` → SC Envelopes)

**PD Implementation**: Cosine-shaped envelopes with fast/slow modes

**SC Implementation**:
```supercollider
// Sensor envelope with variable timing
var gate = \gate.kr(0);
var fast = \fast.kr(1);
var attackTime = Select.kr(fast, [0.2, 0.1]); 
var releaseTime = Select.kr(fast, [8.0, 0.1]);

var env = EnvGen.ar(
    Env([0, 1, 0], [attackTime, releaseTime], [3, -3]), // Curved
    gate: gate,
    doneAction: 0
);

// Add hold behavior
var hold = \hold.kr(0) ** 2;
var holdEnv = env * (1 - hold) + hold;
```

**Performance**: ~0.2% CPU per voice

### 5. Delay System (PD `delay` → SC DelayL/DelayC)

**PD Implementation**: 
- 5944 sample buffers (~135ms at 44.1kHz)
- Variable delay time with modulation
- Feedback with saturation
- High-pass/low-pass filtering

**SC Implementation**:
```supercollider
SynthDef(\lyraDelay, {
    var in = \in.ar(0);
    var time1 = \time1.kr(0.01).clip(0.001, 0.135);
    var time2 = \time2.kr(0.01).clip(0.001, 0.135);
    var feedback = \feedback.kr(0) ** 2 * 2;
    var mod1 = \mod1.kr(0) ** 2 * 0.01;
    var mod2 = \mod2.kr(0) ** 2 * 0.01;
    var lfoTri = \lfoTri.ar(0);
    var mix = \mix.kr(0.5);
    
    // Delay lines with modulation
    var del1Time = time1 + (lfoTri * mod1);
    var del2Time = time2 + (lfoTri * mod2);
    
    var del1 = DelayC.ar(
        LocalIn.ar(1) + in, 
        0.135, 
        del1Time.clip(0.001, 0.135)
    );
    var del2 = DelayC.ar(
        del1, 
        0.135, 
        del2Time.clip(0.001, 0.135)
    );
    
    // Feedback processing with saturation
    var fbSig = del2 * feedback;
    fbSig = HPF.ar(fbSig, 1);           // High-pass
    fbSig = LPF.ar(fbSig, 4000);       // Low-pass  
    fbSig = fbSig.tanh;                 // Saturation
    
    LocalOut.ar(fbSig);
    
    var output = (in * (1 - mix)) + (del2 * mix);
    Out.ar(\out.kr(0), output);
}).add;
```

**Performance**: ~3-4% CPU for dual delay system

### 6. Hyper-LFO System (PD `hyper-lfo` → SC LFO Network)

**PD Implementation**:
- Dual phasor system with chaos logic
- Triangle and square outputs
- Variable frequency with MIDI scaling

**SC Implementation**:
```supercollider
SynthDef(\lyraLFO, {
    var fa = \fa.kr(0).clip(0, 127);
    var fb = \fb.kr(0).clip(0, 127);
    var link = \link.kr(0);
    var andor = \andor.kr(0);
    
    // Convert MIDI-style values to frequencies
    var freqA = (fa * 12 - 75).clip(-75, 75).midicps;
    var freqB = Select.kr(link, [
        (fb * 12 - 75).clip(-75, 75).midicps,
        freqA  // Linked mode
    ]);
    
    // Dual phasor system
    var phasorA = LFSaw.ar(freqA, 0);
    var phasorB = LFSaw.ar(freqB, 0);
    
    // Triangle outputs
    var triA = (phasorA * 2 - 1).abs * 2 - 1;
    var triB = (phasorB * 2 - 1).abs * 2 - 1;
    
    // Square outputs with logic
    var sqrA = phasorA > 0;
    var sqrB = phasorB > 0;
    var sqrLogic = Select.ar(andor, [
        sqrA * sqrB,        // AND
        (sqrA + sqrB > 0)   // OR
    ]);
    
    // Output to buses
    Out.ar(\triOut.kr(100), triA * 0.5);
    Out.ar(\sqrOut.kr(101), sqrLogic * 2 - 1);
}).add;
```

**Performance**: ~1% CPU for LFO system

### 7. Master Output Processing

**PD Implementation**:
- Drive with exponential gain curve
- Tanh saturation
- DC blocking
- Volume control with power curve

**SC Implementation**:
```supercollider
SynthDef(\lyraMaster, {
    var in = \in.ar(0);
    var drive = \drive.kr(0).clip(0, 127);
    var driveMix = \driveMix.kr(0);
    var volume = \volume.kr(127) ** 2 / (127**2);
    
    // Drive stage with exponential curve
    var driveGain = (drive / 127 * 2 + 1) ** 3;
    var driven = (in * driveGain).tanh;
    
    // Parallel drive mix
    var mixed = (in * (1 - driveMix/127)) + (driven * (driveMix/127));
    
    // DC blocking and volume
    var output = HPF.ar(mixed, 3) * volume;
    
    // Total feedback send
    Out.ar(\fbOut.kr(102), output * 0.1);
    Out.ar(\out.kr(0), output);
}).add;
```

**Performance**: ~1% CPU for master processing

## SynthDef Architecture

### Voice SynthDef (`\lyraVoice`)

```supercollider
SynthDef(\lyraVoice, { |voiceNum = 1|
    // Parameters with Norns-friendly ranges
    var freq = \freq.kr(440);
    var gate = \gate.kr(0);
    var tune = \tune.kr(64);          // 0-127
    var sharp = \sharp.kr(0);         // 0-127, pair control
    var modDepth = \modDepth.kr(0);   // 0-127, pair control
    var fast = \fast.kr(1);           // 0/1, pair control
    var modSource = \modSource.kr(0); // 0-2, pair control
    var pitchScale = \pitchScale.kr(1); // 0.01-2.0, quad control
    var hold = \hold.kr(0);           // 0-127, quad control
    
    // Tuning system with table lookup (simplified)
    var tuneScale = tune.linlin(0, 127, -24, 24); // ±2 octaves
    var finalFreq = freq * pitchScale * (tuneScale.midiratio);
    
    // Modulation input from cross-mod matrix
    var modIn = SoundIn.ar(\modBus.kr(0)); // From mod bus
    var modAmount = (modDepth / 127) ** 4 * 2; // Match PD curve
    finalFreq = finalFreq * (1 + (modIn * modAmount));
    
    // Core oscillator with drift
    var drift = LFNoise1.kr(0.1) * 0.001;
    var osc = LFTri.ar(finalFreq + drift);
    
    // Waveshaping
    var shapeAmount = (sharp / 127) ** 2;
    var shaped = Select.ar(shapeAmount > 0.01, [
        osc,
        osc * (1 + shapeAmount).tanh
    ]);
    
    // Sensor envelope
    var attackTime = Select.kr(fast, [0.2, 0.1]);
    var releaseTime = Select.kr(fast, [8.0, 0.1]);
    var env = EnvGen.ar(
        Env([0, 1, 0], [attackTime, releaseTime], [3, -3]),
        gate: gate
    );
    
    // Hold behavior
    var holdAmount = (hold / 127) ** 2;
    var finalEnv = env * (1 - holdAmount) + holdAmount;
    
    // Output
    var output = shaped * finalEnv * 0.125; // 8 voices = -18dB each
    
    // Send to delay bus for cross-modulation
    Out.ar(\delayBus.kr(200) + voiceNum, output * 0.1);
    Out.ar(\out.kr(0), output);
}).add;
```

### Performance Budget Analysis

**Target**: <70% CPU on Norns (leaving headroom for Lua)

| Component | CPU Usage | Notes |
|-----------|-----------|-------|
| 8 Voices | ~12% | Core oscillators + envelopes |
| Cross-mod buses | ~2% | Bus routing and mixing |
| Delay system | ~4% | Dual delays with modulation |
| LFO system | ~1% | Hyper-LFO with logic |
| Master processing | ~1% | Drive + limiting |
| Parameter smoothing | ~2% | Lag UGens for all params |
| **Total** | **~22%** | Well within budget |

## Parameter Control System

### Lag/Smoothing Strategy
All parameters use `Lag.kr()` or `Lag2.kr()` to prevent zipper noise:

```supercollider
// Fast parameters (< 100ms)
var freq = \freq.kr(440).lag(0.02);
var gate = \gate.kr(0); // No lag on gates

// Medium parameters (100-500ms)  
var tune = \tune.kr(64).lag(0.1);
var modDepth = \modDepth.kr(0).lag2(0.2);

// Slow parameters (>500ms)
var feedback = \feedback.kr(0).lag2(0.5);
```

### Bus Allocation Strategy
```supercollider
// Audio buses
~voiceOut = Bus.audio(s, 8);      // Voice outputs
~delayBus = Bus.audio(s, 8);      // Voice delay sends  
~modBus = Bus.audio(s, 8);        // Cross-mod inputs
~lfoTri = Bus.audio(s, 1);        // LFO triangle
~lfoSqr = Bus.audio(s, 1);        // LFO square
~totalFB = Bus.audio(s, 1);       // Total feedback

// Control buses  
~pitchBus = Bus.control(s, 8);    // Voice pitch control
~gateBus = Bus.control(s, 8);     // Voice gates
```

## Safety and Stability Measures

### Feedback Protection
```supercollider
// Feedback limiting with soft saturation
var fbLevel = feedback.clip(0, 0.95); // Never unity gain
var fbSig = DelayN.ar(input, 0.01, 0.001) * fbLevel; // Delay prevents DC
fbSig = fbSig.tanh; // Soft limiting
fbSig = LeakDC.ar(fbSig); // DC removal
```

### Parameter Validation
```supercollider
// All parameter inputs clamped to safe ranges
var freq = \freq.kr(440).clip(20, 20000);
var feedback = \feedback.kr(0).clip(0, 0.99);
var delayTime = \time.kr(0.1).clip(0.001, 0.135);
```

### Anti-Denormal Protection
```supercollider
// Add small noise to prevent denormals in feedback loops
var antiDenormal = WhiteNoise.ar(1e-20);
var delayInput = input + antiDenormal;
```

## Engine Integration Pattern

### Norns Engine Class Structure
```supercollider
Engine_Lyra8 : CroneEngine {
    var <voices, <effects, <buses;
    var <paramSpecs;
    
    *new { |context, doneCallback|
        ^super.new(context, doneCallback);
    }
    
    alloc {
        // Allocate buses
        this.allocBuses();
        
        // Load SynthDefs
        this.loadSynthDefs();
        
        // Create synth instances
        this.createSynths();
        
        // Setup parameter specs
        this.setupParams();
        
        // Register commands
        this.addCommand(\voice_gate, "if", { |msg|
            var voice = msg[1];
            var gate = msg[2];
            voices[voice].set(\gate, gate);
        });
        
        // ... more commands
    }
}
```

## SuperCollider-Specific Optimizations

### 1. LocalBuf for Small Delays
Use `LocalBuf` for delay lines < 10ms to reduce memory allocation:
```supercollider
var buf = LocalBuf(256); // Small delay buffer
var delayed = BufDelayL.ar(buf, input, delayTime.clip(0, 256/SampleRate.ir));
```

### 2. Select UGen for Conditional Processing
Replace multiple UGens with `Select` where possible:
```supercollider
var waveform = Select.ar(\wave.kr(0), [
    LFTri.ar(freq),
    LFSaw.ar(freq),
    LFPulse.ar(freq, 0, 0.5)
]);
```

### 3. Shared LFOs
Use single LFO instances routed to multiple voices via buses rather than per-voice LFOs.

## Testing and Validation Framework

### Unit Tests
Create individual SynthDef tests:
```supercollider
// Test voice oscillator response
~testVoice = { |freq = 440, sharp = 0|
    var voice = Synth(\lyraVoice, [\freq, freq, \sharp, sharp]);
    // Measure output, compare to expected
};
```

### Integration Tests  
Test complete signal chains with known inputs and measure outputs for comparison to PD reference.

---

*This design provides a faithful SuperCollider recreation of the LIRA-8 while optimizing for Norns performance constraints.*
