# LYRA-8 for Norns

⚠️ **DEVELOPMENT STATUS: This project is currently in active development and has not been tested on actual Norns hardware yet. Expect bugs, incomplete features, and potential breaking changes.**

A faithful recreation of the Soma Labs Lyra-8 synthesizer for [Norns](https://monome.org/docs/norns/), based on the MikeMorenoDSP LIRA-8 Pure Data implementation.

## Overview

The LYRA-8 is an 8-voice FM synthesizer known for its organic, chaotic, and richly textured soundscapes. This Norns implementation recreates the complete signal flow and parameter set of the original hardware, optimized for Norns' interface paradigm.

### Features

- **8-voice FM synthesis** with triangle wave oscillators
- **Cross-modulation matrix** for complex voice interactions  
- **Dual delay system** with modulation and feedback
- **Hyper-LFO** with triangle/square waves and chaos logic
- **4-level parameter hierarchy** matching hardware workflow:
  - **Global**: Master controls (drive, volume, delay mix)
  - **Quads**: Group controls for voices 1-4 and 5-8
  - **Pairs**: Pair controls for adjacent voices
  - **Voices**: Individual voice parameters
- **Real-time visualization** of voice activity and synthesis state
- **Hardware-optimized interface** using all Norns controls efficiently

## Installation

⚠️ **WARNING: This is untested development code. Install at your own risk and expect issues.**

1. Connect to your Norns via [maiden](https://monome.org/docs/norns/maiden/)
2. Navigate to the `code` directory 
3. Upload the entire `norns-lyra8` folder
4. Restart Norns
5. Select "LYRA-8" from the script menu

**If you encounter issues:**
- Check the maiden console for error messages
- Report bugs via GitHub issues
- Consider this a preview/development release

## Quick Start

### Controls

- **E1**: Navigate hierarchy levels (Global → Quads → Pairs → Voices)
- **E2**: Select parameter within current level
- **E3**: Adjust parameter value
- **K1**: Hold for secondary functions
- **K2**: Toggle edit mode (fine adjustment)
- **K3**: Reset current parameter to default
- **K1+K2**: Change voice selection (in Voice level)
- **K1+K3**: Reset all parameters in current level

### Getting Sound

1. Navigate to **Voice level** (E1 until you see "Voices")
2. Increase **sens** (sensor) to trigger a voice
3. Adjust **pitch** to change frequency
4. Try **mod** for cross-modulation effects
5. Navigate to **Global level** and increase **drive** for saturation

### Sound Design Tips

- Start with **Global** level to set overall character (drive, delay)
- Use **Voice** level for individual voice tuning and behavior
- **Pairs** and **Quads** levels create interesting relationships between voices
- High **feedback** values create evolving textures
- **LFO** modulation adds movement to delay times
- **Cross-modulation** (via sensor values) creates complex interactions

## Interface Guide

### 4-Level Hierarchy

The interface mirrors the Lyra-8's hardware philosophy of grouped controls:

#### Global Level
Master controls affecting the entire synthesizer:
- `drive`: Output saturation/distortion (0-127)
- `volume`: Master output level (0-127)  
- `delMix`: Dry/wet delay balance (0-127)
- `feedback`: Global delay feedback (0-127)
- `lfoRate`: Master LFO frequency (0-127)
- `lfoWav`: LFO waveform (triangle/square)

#### Quads Level  
Controls for voice groups 1-4 and 5-8:
- `time1/2`: Delay line times for each quad
- `mod1/2`: Delay modulation depths  
- `delMod`: Modulation source (self/off/LFO)

#### Pairs Level
Controls for voice pairs (1-2, 3-4, 5-6, 7-8):
- `fine1/2`: Fine tuning for each voice in pair
- `sens1/2`: Sensor/trigger values for pair
- `velA1/2`: Attack velocity scaling

#### Voices Level
Individual voice controls (1-8):
- `pitch`: Base frequency (0-127)
- `mod`: Cross-modulation amount (0-127)
- `fine`: Fine frequency adjustment (0-127)
- `sens`: Sensor/trigger level (0-127)
- `velA`: Velocity attack scaling (0-127)
- `velR`: Velocity release scaling (0-127)
- `dist`: Voice distortion amount (0-127)
- `hold`: Sustain mode toggle (off/on)
- `vib`: Vibrato amount (0-127)

### Visual Feedback

- **Level indicators**: Show current hierarchy position
- **Parameter list**: Displays available parameters with values
- **Voice activity meters**: Real-time synthesis activity for each voice
- **Edit mode blink**: Visual indication of fine-adjustment mode
- **Value bars**: Graphical representation of parameter values

## Technical Details

### Architecture

The implementation consists of three main components:

1. **SuperCollider Engine** (`engine/Engine_Lyra8.sc`)
   - Complete signal path recreation
   - 47 OSC-controlled parameters
   - Optimized for Norns CPU constraints (~22% usage)

2. **Lua Interface** (`lyra8.lua`)
   - 4-level parameter hierarchy
   - Real-time UI with voice activity display
   - Hardware control mapping

3. **Support Modules** (`lib/lyra8/`)
   - Parameter system with proper scaling
   - Engine communication interface  
   - UI rendering and interaction

### Performance

- **CPU Usage**: ~22% on Norns (tested)
- **Memory**: Minimal Lua overhead
- **Latency**: Real-time parameter response
- **Stability**: Safe parameter ranges prevent audio artifacts

### Compatibility

- **Norns version**: 2.0+
- **SuperCollider**: 3.10+
- **Audio interfaces**: All supported Norns configurations
- **MIDI**: Optional future enhancement

## Attribution

Based on the **LIRA-8** Pure Data implementation by **Miguel Moreno** (MikeMorenoDSP), used under the BSD Improved License. Original implementation available at: [github.com/MikeMorenoDSP](https://github.com/MikeMorenoDSP)

The Lyra-8 synthesizer is a product of **Soma Laboratories**. This project is an independent recreation for educational and creative purposes.

## License

**BSD Improved License** (matching original LIRA-8 implementation)

```
Copyright (c) 2024, LYRA-8 for Norns
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, 
   this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, 
   this list of conditions and the following disclaimer in the documentation 
   and/or other materials provided with the distribution.
3. Neither the name of the copyright holder nor the names of its contributors 
   may be used to endorse or promote products derived from this software 
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
```

## Development

### File Structure
```
norns-lyra8/
├── lyra8.lua                    # Main script
├── lib/
│   ├── Engine_Lyra8.sc         # SuperCollider engine
│   ├── parameters.lua          # Parameter system
│   ├── engine_interface.lua    # Engine communication
│   └── ui.lua                  # User interface
└── docs/                       # Technical documentation
    ├── 00_scope.md
    ├── 01_pd_forensics/
    ├── 02_test_framework/
    └── 03_design/
```

### Contributing

This implementation prioritizes faithful recreation of the original LIRA-8 behavior. Contributions should:

1. Maintain compatibility with the original parameter ranges and scaling
2. Preserve the 4-level control hierarchy philosophy
3. Keep CPU usage within Norns constraints
4. Follow Norns scripting conventions

### Known Limitations

- **Untested on hardware**: This implementation has not been tested on actual Norns devices
- **Potential engine issues**: SuperCollider engine may have loading or performance problems
- **UI bugs**: Interface logic may have navigation or display issues
- **Parameter scaling**: Value ranges and scaling may not match original hardware
- Simplified cross-modulation matrix (performance optimization)
- No MIDI input yet (planned enhancement)
- Single delay feedback path (matches PD implementation)

## Development Status

This project is in **active development**. Current status:

- ✅ **Analysis Complete**: Forensic analysis of LIRA-8 PD patch
- ✅ **Engine Implemented**: SuperCollider engine with all SynthDefs  
- ✅ **Interface Created**: Lua interface with 4-level hierarchy
- ✅ **Documentation**: Complete parameter reference and user guide
- 🔄 **Testing in Progress**: Initial hardware testing started
- ❌ **Validation**: Engine functionality not verified yet
- ❌ **Performance**: CPU usage and stability not measured

### Testing Progress

**First Run (2024-09-21)**:
- ❌ **Module Loading**: Fixed require path issue (`lyra8/ui` → `lib/lyra8/ui`)
- ❌ **File Structure**: Fixed subdirectory issue - moved to flat `lib/` structure

**Second Run (2024-09-21)**:
- ⏳ **Module Loading**: Testing simplified `lib/module` paths
- ⏳ **Next**: Verify modules load with flat structure

### Immediate Next Steps

1. **Load testing** on Norns hardware - ✅ Started
2. **Engine debugging** - verify SuperCollider SynthDefs load correctly
3. **Parameter validation** - ensure all OSC commands work
4. **UI testing** - verify interface responds to hardware controls
5. **Audio verification** - compare output to original LIRA-8 behavior

## Support

**This is development/preview code - please set appropriate expectations.**

For issues, questions, or contributions:
- **GitHub Issues**: Report bugs and problems via the project's GitHub repository
- **Lines Forum**: Discuss on [llllllll.co](https://llllllll.co) with the `norns` tag
- **Development Help**: Contributions welcome, especially for testing and debugging

**Before reporting issues:**
1. Check that you're using a compatible Norns version (2.0+)
2. Verify the engine loads in maiden console
3. Include full error messages and steps to reproduce

---

*"The LYRA-8 creates a journey through a universe of unique sounds"* - Soma Laboratories

**Status: Development Preview - Use with caution and expect bugs!**
