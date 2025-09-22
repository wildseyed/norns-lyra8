# LIRA-8 Parameter Surface Documentation

## Parameter Mapping Overview

The LIRA-8 has 47 total parameters organized in a hierarchical control structure that mirrors the physical hardware layout. Parameters are controlled via GUI elements in Pure Data and can be automated via the parameter storage system.

## Control Hierarchy

### Level 1: Individual Voice Parameters (8 voices)

#### Voice Tuning (`tune-1` through `tune-8`)
- **GUI**: Vertical sliders (positions 1-8)
- **Range**: 0-127 
- **Curve**: Linear → Table lookup
- **Function**: Individual voice frequency tuning
- **Implementation**: Stored in `$0-tune` table, MIDI note conversion
- **Default**: Varies per voice (from stored table)

#### Voice Sensors (`sensor-1` through `sensor-8`)  
- **GUI**: Toggle buttons (sensor pads)
- **MIDI**: C1-G1 (notes 24-31)
- **Keys**: 1-8 (standalone version)
- **Range**: 0/1 (binary)
- **Function**: Voice gate triggers
- **Envelope**: Cosine-shaped attack/release

### Level 2: Pair Parameters (4 pairs: 12, 34, 56, 78)

#### Sharp Controls (`sharp-12`, `sharp-34`, `sharp-56`, `sharp-78`)
- **GUI**: Horizontal sliders  
- **Range**: 0-127
- **Curve**: Power ^2 (exponential)
- **Function**: Waveshaper/distortion amount per voice pair
- **Implementation**: Applied to oscillator shaping

#### Modulation Depth (`mod-12`, `mod-34`, `mod-56`, `mod-78`)
- **GUI**: Horizontal sliders
- **Range**: 0-127  
- **Curve**: Power ^4, then ×2 (steep exponential)
- **Function**: Cross-modulation (FM) depth between voice pairs
- **Max Depth**: 2× frequency deviation

#### Fast Mode (`fast-12`, `fast-34`, `fast-56`, `fast-78`)
- **GUI**: Toggle buttons
- **Range**: 0/1
- **Function**: Switches envelope attack/release times
- **Times**: Fast (100ms/100ms), Slow (200ms/8000ms)

#### Modulation Source (`source-12`, `source-34`, `source-56`, `source-78`)
- **GUI**: Radio buttons (3-way)
- **Range**: 0-2
- **Options**:
  - 0: Neighbor voice delay output (34→56, 78→12)
  - 1: LFO/CV (square wave or off)  
  - 2: Total feedback bus

### Level 3: Quad Parameters (2 quads: 1234, 5678)

#### Pitch Scaling (`pitch-1234`, `pitch-5678`)
- **GUI**: Horizontal sliders
- **Range**: 0-127
- **Curve**: ×1.99 + 0.01 (0.01 to 2.0 multiplier)
- **Function**: Overall pitch scaling for voice groups
- **Default**: ~64 (1.0× multiplier)

#### Hold Amount (`hold-1234`, `hold-5678`)  
- **GUI**: Horizontal sliders
- **Range**: 0-127
- **Curve**: Power ^2 (exponential)
- **Function**: Envelope sustain/hold behavior
- **Implementation**: Controls sensor envelope shape

### Level 4: Global Parameters

#### Hyper-LFO Controls

**Primary Frequency (`f-a`)**
- **GUI**: Vertical slider
- **Range**: 0-127
- **Curve**: MIDI note conversion (power ^2, ×127, offset -75, mtof)
- **Function**: Primary LFO frequency
- **Frequency Range**: ~0.1Hz - ~1kHz

**Secondary Frequency (`f-b`)**
- **GUI**: Vertical slider  
- **Range**: 0-127
- **Curve**: Same as f-a
- **Function**: Secondary LFO frequency (independent unless linked)

**LFO Logic (`andor`)**
- **GUI**: Radio button (2-way)
- **Range**: 0/1 (AND/OR)
- **Function**: Logic operation between dual LFOs
- **Output**: Combined square wave pattern

**Frequency Link (`link`)**
- **GUI**: Toggle button
- **Range**: 0/1
- **Function**: Synchronizes f-b to f-a frequency

#### Delay System

**Delay Time 1 (`time-1`)**
- **GUI**: Horizontal slider
- **Range**: 0-127
- **Curve**: ×12, power ^2, ×1.45125
- **Function**: Primary delay time
- **Time Range**: ~0ms - ~135ms

**Delay Time 2 (`time-2`)**
- **GUI**: Horizontal slider
- **Range**: 0-127  
- **Curve**: Same as time-1
- **Function**: Secondary delay time

**Modulation 1 (`mod-1`)**
- **GUI**: Horizontal slider
- **Range**: 0-127
- **Curve**: Power ^2, ×10
- **Function**: Delay 1 modulation depth

**Modulation 2 (`mod-2`)**
- **GUI**: Horizontal slider
- **Range**: 0-127
- **Curve**: Same as mod-1  
- **Function**: Delay 2 modulation depth

**Delay Modulation Source (`del-mod`)**
- **GUI**: Radio buttons (3-way)
- **Range**: 0-2
- **Options**:
  - 0: Self-modulation (delay output → delay time)
  - 1: Off
  - 2: LFO modulation

**LFO Waveform (`lfo-wav`)**
- **GUI**: Radio buttons (2-way)  
- **Range**: 0/1 (Triangle/Square)
- **Function**: Selects LFO waveform for delay modulation

**Global Feedback (`feedback`)**
- **GUI**: Horizontal slider
- **Range**: 0-127
- **Curve**: ×2, power ^2
- **Function**: Delay feedback amount with saturation

**Delay Mix (`del-mix`)**
- **GUI**: Horizontal slider
- **Range**: 0-127
- **Curve**: Linear (0-100% wet)
- **Function**: Delay effect dry/wet balance

#### Distortion/Drive System

**Drive Amount (`drv`)**
- **GUI**: Vertical slider
- **Range**: 0-127  
- **Curve**: (×2 + 1), power ^3 (exponential)
- **Function**: Saturation drive level
- **Max Gain**: ~1000× (very aggressive)

**Drive Mix (`dst-mix`)**
- **GUI**: Vertical slider
- **Range**: 0-127
- **Curve**: Linear (0-100% wet)
- **Function**: Drive effect dry/wet blend

#### Global Controls

**Master Volume (`vol`)**
- **GUI**: Vertical slider
- **Range**: 0-127
- **Curve**: Power ^2
- **Function**: Final output level

**Global Toggles**:
- `total-fb` (0/1): Enables total feedback bus
- `vibrato` (0/1): Enables global vibrato LFO  
- `switch` (0/1): Reverses modulation routing direction
- `quantize` (0/1): Quantizes tuning to chromatic scale

#### Utility Parameters
- `dsp` (0/1): DSP on/off toggle
- Internal parameters for envelope times, random seeds

## Parameter Storage Format

Parameters are stored in `parameters.txt` as a simple list of 47 float values corresponding to the gui.link parameter IDs 1-47. The storage system uses Pure Data's `text define` objects for persistence.

## Default Parameter Set

The following represents a typical "starting patch" configuration:

```
Position | Parameter | Value | Notes
---------|-----------|-------|-------
1-8      | tune-N    | Varies| From stored tuning table  
9-12     | fast-XY   | 1     | Fast envelopes enabled
13-16    | sharp-XY  | ~30   | Light waveshaping
17-20    | mod-XY    | 0     | No cross-modulation
21-24    | source-XY | 0     | Neighbor routing
25-26    | pitch-XY  | 65    | Unity pitch scaling
27-28    | hold-XY   | 0     | No hold
29       | switch    | 0     | Normal routing
30       | total-fb  | 0     | Feedback off
31       | vibrato   | 1     | Vibrato enabled
32       | drv       | 86    | Medium drive
33       | dst-mix   | 69    | Mixed drive
34       | vol       | 127   | Full volume
35       | f-a       | 42    | Low LFO frequency
36       | f-b       | 64    | Medium LFO frequency  
37       | andor     | 1     | OR logic
38       | link      | 1     | Frequencies linked
39       | time-1    | 95    | Long delay time
40       | time-2    | 57    | Medium delay time
41       | feedback  | 54    | Moderate feedback
42       | del-mix   | 68    | Delay mixed in
43       | mod-1     | 41    | Light modulation
44       | mod-2     | 48    | Light modulation  
45       | del-mod   | 2     | LFO modulation
46       | lfo-wav   | 0     | Triangle wave
47       | quantize  | 0     | Free tuning
```

## MIDI Control Implementation

While the base patch doesn't include MIDI CC mapping, the parameter structure is designed for external control:

- **Sensors**: MIDI notes C1-G1 (24-31) → binary triggers
- **Continuous Parameters**: Could be mapped to CC 1-47 
- **Suggested CC Mapping**: 
  - CC 1-8: Voice tuning
  - CC 9-16: Pair controls (sharp, mod)
  - CC 17-24: Global LFO/delay controls
  - CC 25-32: Drive/mix controls

---

*Complete parameter specification for LIRA-8 Pure Data implementation*
