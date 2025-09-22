# LIRA-8 Golden Reference Test Scenarios

## Overview
This document defines 12 test scenarios designed to capture the essential behaviors of the LIRA-8 for validation against our SuperCollider implementation. Each test focuses on specific aspects of the synthesis engine.

## Test Methodology
- **Duration**: 10 seconds per test
- **Sample Rate**: 48kHz, 24-bit
- **Triggers**: Automated sensor activation at specific times
- **Parameters**: Set via parameter storage system
- **Outputs**: Stereo stems + analysis metrics

## Test Scenarios

### Test 1: Basic Voice Tuning
**Objective**: Validate individual voice tuning and basic oscillator behavior

**Parameters**:
```
tune-1: 64    # C4 reference
tune-2: 76    # C5 (octave up)  
tune-3: 52    # C3 (octave down)
tune-4: 67    # Eb4 (minor 3rd)
tune-5-8: 64  # All C4
sharp-12: 0   # No waveshaping
mod-12: 0     # No cross-mod
All other params: Default
```

**Sequence**:
- t=0s: Trigger sensor-1 (hold for 2s)
- t=2s: Trigger sensor-2 (hold for 2s)  
- t=4s: Trigger sensor-3 (hold for 2s)
- t=6s: Trigger sensor-4 (hold for 2s)
- t=8s: All sensors off

**Expected**: Clean triangle waves at specified pitches, no modulation

### Test 2: Cross-Modulation Sweep
**Objective**: Test FM cross-modulation between voice pairs

**Parameters**:
```
tune-1,2: 64  # C4 base frequency
mod-12: 0→127 # Sweep cross-mod over 8 seconds
sharp-12: 0   # No waveshaping
source-12: 0  # Neighbor routing (voice 4→voice 1)
```

**Sequence**:
- t=0s: Trigger sensor-1 and sensor-2
- t=1-9s: Linear ramp mod-12 from 0 to 127
- t=9s: Release sensors

**Expected**: Increasing FM depth, complex harmonic development

### Test 3: Waveshaper Response  
**Objective**: Test distortion/waveshaping characteristics

**Parameters**:
```
tune-1: 64    # C4
sharp-12: 0→127 # Sweep waveshaping
mod-12: 0     # No cross-mod
```

**Sequence**:
- t=0s: Trigger sensor-1
- t=1-9s: Linear ramp sharp-12 from 0 to 127
- t=9s: Release sensor

**Expected**: Progressive harmonic distortion, increasing THD

### Test 4: Delay System - Time Sweep
**Objective**: Test delay time response and feedback

**Parameters**:
```
tune-1: 64    # C4
time-1: 20→120 # Sweep delay time
feedback: 60  # Moderate feedback
del-mix: 80   # Wet signal prominent
del-mod: 0    # Self-modulation off
```

**Sequence**:
- t=0s: Trigger sensor-1 (short burst)
- t=0.5s: Release sensor-1
- t=1-8s: Ramp time-1 from 20 to 120
- Monitor delay feedback decay

**Expected**: Changing delay time, stable feedback, spectral analysis

### Test 5: Hyper-LFO Behavior
**Objective**: Test LFO system and modulation routing

**Parameters**:
```
f-a: 80       # ~2Hz LFO
f-b: 90       # ~4Hz LFO  
andor: 0      # AND logic
link: 0       # Independent frequencies
lfo-wav: 0    # Triangle wave
del-mod: 2    # LFO modulation
mod-1: 80     # Strong modulation depth
time-1: 60    # Medium delay time
```

**Sequence**:
- t=0s: Trigger sensor-1
- t=0-10s: Hold sensor, monitor LFO modulation
- t=9s: Release sensor

**Expected**: Rhythmic delay time modulation, complex patterns

### Test 6: Drive System Response
**Objective**: Test distortion drive and saturation

**Parameters**:
```
tune-1: 64    # C4
drv: 0→127    # Sweep drive amount
dst-mix: 100  # Full wet signal
vol: 80       # Moderate volume
```

**Sequence**:
- t=0s: Trigger sensor-1  
- t=1-9s: Linear ramp drv from 0 to 127
- t=9s: Release sensor

**Expected**: Progressive saturation, tanh characteristic curve

### Test 7: Envelope Response - Fast/Slow
**Objective**: Test sensor envelope behavior and timing

**Parameters**:
```
tune-1,2: 64  # C4
fast-12: 0    # Slow envelopes initially
hold-1234: 40 # Moderate hold amount
```

**Sequence**:
- t=0s: Trigger sensor-1 (release at t=1s)
- t=2s: Set fast-12: 1 (fast envelopes)
- t=3s: Trigger sensor-2 (release at t=4s)
- Compare envelope shapes

**Expected**: Slow vs fast attack/release times, hold behavior

### Test 8: Complex Modulation Matrix
**Objective**: Test all modulation sources and routing

**Parameters**:
```
tune-1,2,3,4: 64,67,71,76  # Chord (C-Eb-G-C)
source-12: 1  # LFO/CV
source-34: 2  # Total feedback  
mod-12: 60    # Moderate cross-mod
mod-34: 80    # Strong cross-mod
f-a: 75       # LFO frequency
total-fb: 1   # Enable total feedback
```

**Sequence**:
- t=0s: Trigger all sensors 1-4
- t=0-8s: Hold all sensors
- t=8s: Release all

**Expected**: Complex harmonic interaction, chaos development

### Test 9: Dual Delay Interaction
**Objective**: Test both delay lines and their interaction

**Parameters**:
```
tune-1: 64    # C4
time-1: 40    # Short delay
time-2: 80    # Long delay  
feedback: 70  # Strong feedback
del-mix: 90   # Very wet
mod-1: 50     # Delay 1 modulation
mod-2: 30     # Delay 2 modulation
del-mod: 2    # LFO modulation
f-a: 85       # LFO rate
```

**Sequence**:
- t=0s: Short trigger sensor-1 (0.1s burst)
- t=0.1s: Release sensor
- t=0.1-10s: Monitor dual delay interaction

**Expected**: Complex rhythmic patterns, delay coupling

### Test 10: Extreme Parameter Values
**Objective**: Test stability at parameter extremes

**Parameters**:
```
tune-1: 0     # Lowest frequency
tune-2: 127   # Highest frequency
mod-12: 127   # Maximum cross-mod
sharp-12: 127 # Maximum waveshaping
drv: 127      # Maximum drive
feedback: 120 # Near-maximum feedback
```

**Sequence**:
- t=0s: Trigger sensor-1 and sensor-2
- t=0-8s: Hold sensors
- Monitor for instability, clipping, runaway feedback

**Expected**: Extreme sounds but stable operation, no NaN/overflow

### Test 11: Vibrato and Pitch Modulation
**Objective**: Test global vibrato system and pitch scaling

**Parameters**:
```
tune-1,2,3,4: 64  # All C4 initially
pitch-1234: 0→127 # Sweep pitch scaling  
vibrato: 1        # Enable vibrato
quantize: 0       # Free tuning
```

**Sequence**:
- t=0s: Trigger sensors 1-4
- t=1-8s: Ramp pitch-1234 from 0 to 127
- t=8s: Release sensors

**Expected**: Pitch scaling with vibrato modulation, smooth transitions

### Test 12: Real-Time Performance Test
**Objective**: Test rapid parameter changes and sensor activity

**Parameters**: Start with defaults

**Sequence**:
- t=0-10s: Rapid sensor triggering (1/4 note patterns)
- t=0-10s: Simultaneous parameter automation:
  - mod-12: Random walk
  - feedback: Sine wave 0.1Hz
  - f-a: Step changes every 2s
  - sharp-12: Sawtooth wave

**Expected**: Stable performance under load, no dropouts or artifacts

## Test Output Requirements

### Audio Files
For each test, record:
- `test_N_master.wav`: Stereo master output
- `test_N_dry.wav`: Dry voice sum (pre-effects)
- `test_N_delay.wav`: Delay output only
- `test_N_drive.wav`: Drive output only

### Analysis Metrics
For each test, compute:
- **RMS Level**: Overall and per-second
- **Peak Level**: Maximum sample value
- **THD**: Total harmonic distortion (fundamental + 5 harmonics)
- **Spectral Centroid**: Frequency center of mass
- **Spectral Rolloff**: 95% energy frequency
- **Zero Crossing Rate**: For waveform analysis
- **Crest Factor**: Peak-to-RMS ratio

### Parameter Snapshots
For each test, save:
- `test_N_params.json`: All 47 parameter values at start/end
- `test_N_automation.csv`: Time-series data for automated parameters

## Analysis Tools

### Pure Data Analysis Patch
Create companion analysis patch with:
- Spectrum analyzer (FFT-based)
- RMS/peak meters with logging
- THD calculator
- Export utilities for CSV data

### Post-Processing Scripts
Python scripts for:
- Metric calculation and aggregation
- Statistical analysis of parameter sweeps
- Comparison tools for PD vs SC validation
- Automated report generation

## Validation Tolerances

When comparing PD reference to SC implementation:
- **RMS Level**: ±0.5 dB
- **Peak Level**: ±1.0 dB  
- **THD**: ±5% relative
- **Spectral Centroid**: ±3% relative
- **Timing**: ±1 sample accuracy
- **Frequency Response**: ±0.5 dB 20Hz-20kHz

## Test Environment Setup

### Pure Data Configuration
- Sample rate: 48kHz
- Block size: 64 samples
- Audio driver: ASIO/CoreAudio for low latency
- Parameter automation via message scheduling

### Recording Setup
- Internal PD recording (no external interfaces)
- 24-bit depth minimum
- Automated test runner with parameter loading
- Consistent gain staging across all tests

---

*This test suite provides comprehensive coverage of LIRA-8 behaviors for faithful SuperCollider recreation*
