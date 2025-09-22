# LYRA-8 User Manual

## Table of Contents

1. [Getting Started](#getting-started)
2. [Understanding the Interface](#understanding-the-interface)
3. [Sound Design Guide](#sound-design-guide)
4. [Parameter Reference](#parameter-reference)
5. [Performance Tips](#performance-tips)
6. [Troubleshooting](#troubleshooting)

## Getting Started

### First Steps

1. **Load the script**: Select "LYRA-8" from the Norns script menu
2. **Check the engine**: You should see "LYRA-8: Engine loaded successfully" in the console
3. **Navigate to Voice level**: Turn E1 until you see "Voices" 
4. **Trigger a voice**: Turn E2 to select "sens", then turn E3 to increase the sensor value
5. **Adjust pitch**: Turn E2 to select "pitch", then turn E3 to change frequency
6. **Add character**: Navigate to Global level (E1) and increase "drive"

### Basic Sound Creation

The LYRA-8 works by triggering voices with sensor values and shaping them with various parameters:

1. **Trigger**: Set `sens` > 0 to activate a voice
2. **Tune**: Adjust `pitch` for base frequency, `fine` for micro-tuning
3. **Shape**: Use `velA`/`velR` for envelope, `dist` for saturation
4. **Modulate**: Increase `mod` for cross-modulation between voices
5. **Effect**: Add delay and feedback at Global level

## Understanding the Interface

### Navigation Philosophy

The interface follows a hierarchical approach inspired by the original Lyra-8 hardware:

- **Global**: Overall character and effects
- **Quads**: Groupings of 4 voices (1-4, 5-8)  
- **Pairs**: Adjacent voice pairs (1-2, 3-4, 5-6, 7-8)
- **Voices**: Individual voice control

This hierarchy lets you efficiently control complex multi-voice textures while maintaining direct access to detailed parameters.

### Control Mapping

| Control | Function | Secondary (K1 held) |
|---------|----------|---------------------|
| E1 | Navigate levels | - |
| E2 | Select parameter | - |
| E3 | Adjust value | - |
| K1 | Hold for secondary | - |
| K2 | Edit mode (fine adjust) | Change voice |
| K3 | Reset to default | Reset level |

### Visual Elements

- **Level indicators**: 4 numbered boxes showing current hierarchy level
- **Parameter list**: Current level's parameters with names and values
- **Value bars**: Graphical representation of parameter positions
- **Voice meters**: Real-time activity display for all 8 voices
- **Edit blink**: "EDIT" indicator flashes when in fine-adjustment mode

## Sound Design Guide

### Creating Basic Tones

1. **Single Voice Drone**:
   - Voice level: Set `sens` to 100, `pitch` to 64 (middle)
   - Global level: `drive` to 30, `volume` to 80
   - Result: Clean sine-like tone

2. **Harmonic Intervals**:
   - Voice 1: `pitch` 64, `sens` 100
   - Voice 2: `pitch` 84 (perfect fifth), `sens` 100
   - Adjust `fine` parameters for exact tuning

3. **Modulated Texture**:
   - Voice 1: `pitch` 50, `sens` 80, `mod` 40
   - Voice 2: `pitch` 60, `sens` 70, `mod` 60
   - Cross-modulation creates complex harmonics

### Rhythmic Patterns

1. **Pulsing Effect**:
   - Set LFO: Global level, `lfoRate` 40-80
   - Use Quad level: `delMod` to LFO
   - Modulates delay time for rhythmic effect

2. **Voice Sequencing**:
   - Set different `velA`/`velR` values per voice
   - Trigger multiple voices with varying `sens` levels
   - Use `hold` mode for sustained notes

### Ambient Soundscapes

1. **Deep Space**:
   - Low `pitch` values (20-40)
   - High `feedback` (80-100) 
   - `delMix` at 70-90
   - Slow LFO modulation

2. **Organic Texture**:
   - Multiple voices with close `pitch` values
   - Moderate `mod` settings (30-60)
   - Add `vib` for movement
   - Use `dist` sparingly for grit

### Chaotic Explorations

1. **Full Chaos**:
   - All voices active (`sens` > 50)
   - High `mod` values (80-127)
   - Maximum `feedback` 
   - Fast LFO with square wave

2. **Controlled Chaos**:
   - Use Pairs level to group behaviors
   - Set different `delMod` sources per quad
   - Balance with master `volume` and `drive`

## Parameter Reference

### Global Parameters

| Parameter | Range | Default | Description |
|-----------|-------|---------|-------------|
| `drive` | 0-127 | 17 | Output saturation/distortion |
| `volume` | 0-127 | 100 | Master output level |
| `delMix` | 0-127 | 68 | Dry/wet delay balance |
| `feedback` | 0-127 | 54 | Global delay feedback |
| `lfoRate` | 0-127 | 23 | Master LFO frequency |
| `lfoWav` | 0-1 | 0 | LFO waveform (0=tri, 1=sqr) |

### Voice Parameters

| Parameter | Range | Default | Description |
|-----------|-------|---------|-------------|
| `pitch` | 0-127 | 64 | Base frequency |
| `mod` | 0-127 | 0 | Cross-modulation amount |
| `fine` | 0-127 | 64 | Fine frequency adjustment |
| `sens` | 0-127 | 0 | Sensor/trigger level |
| `velA` | 0-127 | 64 | Velocity attack scaling |
| `velR` | 0-127 | 64 | Velocity release scaling |
| `dist` | 0-127 | 0 | Voice distortion |
| `hold` | 0-1 | 0 | Sustain mode (0=off, 1=on) |
| `vib` | 0-127 | 0 | Vibrato amount |

### Scaling Notes

- **Pitch**: Exponential scaling, 64 ≈ 261 Hz (middle C)
- **Modulation**: Linear scaling, affects FM depth
- **Sensor**: 0 = voice off, >0 = trigger/sustain level
- **Delay times**: Exponential scaling, musical intervals
- **LFO rate**: Exponential scaling, 0.1 Hz to 20 Hz range

## Performance Tips

### Live Control Strategies

1. **Level Switching**: Use E1 frequently to access different control layers
2. **Edit Mode**: Hold K2 for fine adjustments during performance
3. **Voice Selection**: Use K1+K2 to quickly jump between voices
4. **Reset Safety**: K3 for quick parameter resets, K1+K3 for level resets

### CPU Management

- The engine uses ~22% CPU, leaving room for other Norns processes
- High `feedback` values can increase CPU usage
- Multiple active voices add computational load
- Use `volume` to prevent digital clipping

### Creative Techniques

1. **Parameter Sweeps**: Use edit mode for slow parameter transitions
2. **Level Jumping**: Quick E1 movements for dramatic changes
3. **Voice Layering**: Build complexity by adding voices incrementally
4. **Feedback Riding**: Dynamic `feedback` control for texture evolution

## Troubleshooting

### Common Issues

**No Sound Output**:
- Check `volume` and `sens` parameters
- Verify Norns audio output configuration
- Try resetting to defaults (K1+K3 at Global level)

**Distorted/Harsh Audio**:
- Reduce `drive` and `feedback` levels
- Lower individual voice `sens` values
- Check master `volume` setting

**Interface Not Responding**:
- Restart the script from the Norns menu
- Check maiden console for error messages
- Verify engine loaded successfully

**Parameter Changes Not Audible**:
- Ensure voices are triggered (`sens` > 0)
- Check you're at the correct hierarchy level
- Some parameters only affect new voice triggers

### Advanced Diagnostics

**Console Commands** (in maiden):
```lua
-- Check engine status
engine.print_status()

-- Reset everything
params:default()

-- Emergency stop
engine.emergency_reset()
```

**Parameter Verification**:
- All parameters should show current values in the interface
- Value bars should move with E3 adjustments
- Voice meters should show activity when `sens` > 0

### Performance Optimization

If experiencing audio dropouts:
1. Reduce number of active voices
2. Lower `feedback` amounts
3. Avoid extreme `mod` values with multiple voices
4. Check system CPU usage in maiden

---

*Remember: The LYRA-8 is designed for exploration. Don't be afraid to push parameters to extremes and discover unexpected sonic territories!*
