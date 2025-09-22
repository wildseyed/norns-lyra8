# LIRA-8 Pure Data Forensic Analysis - Module Documentation

## Signal Flow Overview

The LIRA-8 Pure Data implementation consists of several main components:

1. **8 Voice Oscillators** (`lira.voice` x8)
2. **Cross-Modulation Matrix** (`prm.source` x4)
3. **Hyper-LFO** (dual frequency, tri/square)
4. **Dual Delay** (modulated delays with feedback)
5. **Distortion/Drive** (tanh-based saturation)
6. **Master Output** (final mixing and limiting)

## Module Breakdown

### 1. Voice Oscillator Module (`lira.voice.pd`)

**Function**: Generates the primary oscillator for each voice with FM capabilities

**Inputs**:
- `I1`: Modulation source (audio rate, typically from neighbor voice)
- `I2`: Pitch control (from `prm.pitch`)
- `I3`: Hold amount (from `prm.hold`)

**Outputs**:
- Main audio output (to voice mixer)
- Delay send bus (`\$1-del-\$2\$3`)

**Internal Architecture**:
- **Core Oscillator**: `os.triangle~` - PolyBLEP-based triangle/square oscillator
  - Generates both triangle and square outputs
  - Built-in oversampling and anti-aliasing
  - Sample-and-hold for pitch drift/vibrato
- **Tuning System**: `prm.tune` 
  - MIDI note to frequency conversion
  - Quantization option
  - Stored tuning table lookup
- **FM Modulation**: Cross-voice frequency modulation
  - `prm.mod` controls modulation depth (0-127 → 0-2x, power curve ^4)
  - `prm.sharp` controls waveshaper amount (power curve ^2)
- **Sensor Gates**: `prm.sensor`
  - Triggered by MIDI C1-G1 or keys 1-8
  - Attack/release envelope with cosine curve
  - Fast/slow mode toggle affects envelope times
- **Vibrato**: Global vibrato LFO with random drift

**Key Parameters**:
- `tune-N` (0-127): Individual voice tuning
- `sharp-XY` (0-127): Pair waveshaping amount  
- `mod-XY` (0-127): Pair cross-modulation depth
- `fast-XY` (0/1): Pair envelope speed toggle
- `sensor-N` (0/1): Individual voice gate

### 2. Cross-Modulation Matrix (`prm.source.pd`)

**Function**: Routes modulation sources to voice pairs based on switch settings

**Inputs**:
- Delay output from neighbor voice pair
- LFO square wave
- Total feedback bus
- Delay output from target voice pair

**Parameters**:
- `source-XY` (0-2): Modulation source selection
  - 0: Neighbor delay output
  - 1: LFO/CV 
  - 2: Total feedback
- `switch` (0/1): Routing direction toggle
- `total-fb` (0/1): Global feedback enable

**Output**: Mixed modulation signal to voice FM input

### 3. Pitch Control (`prm.pitch.pd`, `prm.tune.pd`)

**Function**: Converts UI parameters to oscillator frequencies

**prm.pitch**: 
- Range: 0-127 → 0.01-2.0 (pitch multiplier)
- Applied to voice pairs (1234, 5678)

**prm.tune**:
- Individual voice tuning with stored table
- MIDI note → frequency conversion via `mtof`
- Optional quantization to chromatic scale
- Line smoothing (23.22ms) for parameter changes

### 4. Hyper-LFO (`hyper-lfo` subpatch)

**Function**: Dual-frequency LFO system with chaos and linking

**Architecture**:
- **F-A**: Primary LFO frequency (0-127 → MIDI notes)
- **F-B**: Secondary LFO frequency (independent)
- **Waveforms**: Triangle and square outputs
- **Chaos**: AND/OR logic between LFOs for complex patterns
- **Link**: Synchronizes F-B to F-A frequency

**Outputs**:
- `del-tri`: Triangle LFO for delay modulation
- `del-sqr`: Square LFO for delay modulation  
- `sqr-lfo`: Square wave for cross-modulation matrix

### 5. Delay System (`delay` subpatch)

**Function**: Dual delay lines with LFO modulation and feedback

**Architecture**:
- **Two Independent Delays**: 5944 sample buffers (~135ms at 44.1kHz)
- **Modulation Sources**:
  - Self-modulation (delay output → delay time)
  - LFO modulation (triangle/square from hyper-LFO)
  - Manual modulation (MOD-1, MOD-2 controls)
- **Time Control**: Exponential scaling (1.45125 factor, ^2 curve)
- **Feedback**: Saturated feedback with compressor/expander
- **Filtering**: High-pass (1Hz) and low-pass (4kHz) in feedback path
- **Mixing**: Dry/wet control with final dry/wet blend

**Parameters**:
- `time-1`, `time-2`: Delay times
- `mod-1`, `mod-2`: Modulation depths  
- `del-mod`: Modulation source select (self/off/LFO)
- `lfo-wav`: LFO waveform select (tri/sqr)
- `feedback`: Global feedback amount
- `del-mix`: Dry/wet balance

### 6. Distortion (`ma.tanh~.pd`)

**Function**: Pade approximation of tanh() for saturation

**Formula**: `y = x * (27 + x²) / (27 + 9*x²)`
- Input clipped to ±3
- Smooth saturation curve
- Used in both drive section and feedback limiting

### 7. Dynamics Processing

**Compressor** (`compressor~.pd`):
- Envelope follower with attack/release
- Threshold and ratio controls
- Used in delay feedback path

**Expander** (`expander~.pd`):
- Downward expansion below threshold
- Used for noise gate behavior in delays

### 8. Master Output (`master_output` subpatch)

**Function**: Final mixing, drive, and output limiting

**Signal Path**:
1. **Drive Stage**: Exponential gain (3^ratio) → tanh saturation
2. **Mix Control**: Parallel dry/wet blend of drive effect
3. **DC Blocking**: High-pass filter (3Hz)
4. **Volume**: Power-of-2 scaling
5. **Total Feedback**: Send to feedback bus

**Parameters**:
- `drv`: Drive amount (exponential)
- `dst-mix`: Drive dry/wet mix
- `vol`: Master volume

## Control Hierarchy

The LIRA-8 implements a 4-level control hierarchy matching the hardware:

### Individual Voice Controls (8x)
- `tune-N`: Per-voice tuning
- `sensor-N`: Per-voice gate/trigger

### Pair Controls (4x) 
- `sharp-XY`: Waveshaper amount for voice pairs
- `mod-XY`: Cross-modulation depth for pairs
- `fast-XY`: Envelope speed toggle for pairs
- `source-XY`: Modulation source routing for pairs

### Quad Controls (2x)
- `pitch-XYZW`: Pitch scaling for voice quads
- `hold-XYZW`: Hold envelope amount for quads

### Global Controls
- `f-a`, `f-b`: Hyper-LFO frequencies
- `andor`: LFO logic mode
- `link`: LFO frequency linking
- `time-1`, `time-2`: Delay times
- `mod-1`, `mod-2`: Delay modulation depths
- `feedback`: Global delay feedback
- `del-mix`: Delay dry/wet
- `drv`: Drive amount
- `dst-mix`: Drive dry/wet
- `vol`: Master volume
- Global toggles: `total-fb`, `vibrato`, `switch`, `quantize`

## Parameter Ranges and Curves

| Parameter | Range | Curve | Function |
|-----------|-------|-------|----------|
| `tune-N` | 0-127 | Linear | Stored table lookup |
| `sharp-XY` | 0-127 | Power ^2 | Waveshaper depth |
| `mod-XY` | 0-127 | Power ^4, x2 | FM depth |
| `pitch-XYZW` | 0-127 | x1.99 + 0.01 | Pitch multiplier |
| `hold-XYZW` | 0-127 | Power ^2 | Envelope sustain |
| `f-a`, `f-b` | 0-127 | MIDI note | LFO frequency |
| `time-1`, `time-2` | 0-127 | x12, ^2, x1.45125 | Delay time |
| `mod-1`, `mod-2` | 0-127 | Power ^2, x10 | Mod depth |
| `feedback` | 0-127 | x2, ^2 | Feedback amount |
| `drv` | 0-127 | (x2+1), ^3 | Drive gain |
| `vol` | 0-127 | Power ^2 | Output level |

## MIDI Implementation

- **Note Input**: C1-G1 (MIDI notes 24-31) trigger sensors 1-8
- **Key Input**: Number keys 1-8 trigger sensors (standalone version)
- **Velocity**: Not used (binary on/off triggers)
- **Continuous Controllers**: Not implemented in base patch

## Audio Specifications

- **Sample Rate**: 44.1kHz (typical)
- **Bit Depth**: 32-bit float (Pure Data internal)
- **Delay Buffer**: 5944 samples (~135ms max delay)
- **Frequency Range**: ~20Hz-20kHz (limited by oscillator design)
- **Dynamic Range**: ~90dB (limited by tanh saturation)

---

*Analysis based on MikeMorenoDSP LIRA-8 v2020, Pure Data standalone implementation*
