# LIRA-8 → Norns Project Scope

## Overview
This project implements a SuperCollider engine and Norns Lua interface recreating the SOMA Labs Lyra-8 synthesizer, based on forensic analysis of the MikeMorenoDSP LIRA-8 Pure Data emulation.

## Source Material
- **Repository**: [MikeMorenoDSP/LIRA-8](https://github.com/MikeMorenoDSP/LIRA-8)
- **Reference Implementation**: `LIRA-8_Pd_Standalone/` folder
- **Backup Reference**: `LIRA-8_PlugData/` folder

## License & Attribution
The source LIRA-8 project uses the **Standard Improved BSD License** by Miguel Moreno.

**Key Requirements:**
1. ✅ **Attribution Required**: Must retain copyright notice and credit Miguel Moreno
2. ✅ **Redistribution Allowed**: Can redistribute in source/binary forms with modifications
3. ✅ **Disclaimer Required**: Must include license disclaimer in documentation
4. ❌ **No Endorsement**: Cannot use author's name to endorse derived products

**Our Implementation:**
- Clean-room SuperCollider re-implementation (not a port)
- Full attribution to Miguel Moreno and original LIRA-8 project
- BSD-compatible license for our Norns implementation
- Clear disclaimer that this is not affiliated with SOMA laboratories

## Deliverables

### Phase 1: Forensic Analysis
- [ ] **Signal Flow Diagram** (`docs/01_pd_forensics/signalflow.svg`)
  - Complete audio/control pipeline mapping
  - Feedback paths and cross-modulation routes
  - Block-level architecture

- [ ] **Module Documentation** (`docs/01_pd_forensics/modules.md`)
  - Per-module spec sheets (I/O, ranges, math, timing)
  - Voice architecture (8 voices, FM/AM/cross-mod)
  - Effects chain (dual delay, distortion, hyper-LFO)
  - Control systems (sensor gates, hold logic, shared controls)

- [ ] **Parameter Surface** (`docs/01_pd_forensics/params.md`)
  - Complete parameter map (MIDI C1-G1 / keys 1-8)
  - Range/curve/taper documentation
  - Control hierarchy (individual → pair → quad → global)

### Phase 2: Golden Reference Set
- [ ] **Test Scenarios** (`tests/gold/`)
  - 10-12 automated parameter sweep scenes
  - Audio stems (48kHz/24-bit) + metrics (RMS, THD, etc.)
  - Parameter snapshots for A/B validation

### Phase 3: SuperCollider Engine
- [ ] **Engine Design** (`docs/02_sc_design.md`)
  - UGen mapping (PD blocks → SC SynthDefs)
  - Performance budget for Norns
  - Modulation routing architecture

- [ ] **Implementation** (`engine/Engine_Lyra8.sc`)
  - Voice management (8 voices)
  - Effects processing (drive, dual delay)
  - Parameter control with de-zippering
  - Safety limits (feedback clamps, DC blocks)

### Phase 4: Norns Interface
- [ ] **UI Design** (`docs/03_norns_ui.md`)
  - 4-level hierarchy: Voices → Pairs → Quads → Global
  - K1/K2/K3 + E1/E2/E3 mapping
  - Sensor-like interaction paradigm

- [ ] **Lua Implementation** (`lua/lyra8.lua`)
  - Intuitive navigation matching hardware workflow
  - Preset management
  - MIDI/OSC integration

### Phase 5: Validation
- [ ] **Cross-Validation Report** (`tests/reports/compare.md`)
  - PD vs SC metrics comparison
  - Tolerance analysis (±0.5dB RMS, ±3% spectral)
  - ABX listening test results

### Phase 6: Release Package
- [ ] **Documentation** (`README.md`, installation guide)
- [ ] **Performance Optimization** (CPU profiling, voice limiting)
- [ ] **Proper Attribution** (credits, license compliance)

## Success Criteria
1. **Sonic Fidelity**: SC engine matches PD reference within agreed tolerances
2. **Playability**: Norns interface feels intuitive to Lyra-8 users
3. **Performance**: Stable operation on Norns hardware 
4. **Legal Compliance**: Proper attribution and license requirements met

## Technical Constraints
- **Norns Hardware**: ARM CPU, limited DSP resources
- **SuperCollider**: Native UGens preferred over custom DSP
- **Interface**: 3 keys + 3 encoders for full control surface
- **Memory**: Reasonable buffer sizes for delay effects

## Project Timeline
- **Phase 1-2**: Forensic analysis + golden reference (current focus)
- **Phase 3**: SuperCollider engine implementation
- **Phase 4**: Norns Lua interface
- **Phase 5**: Validation and optimization
- **Phase 6**: Documentation and packaging

---
*This implementation respects the original LIRA-8 BSD license and includes full attribution to Miguel Moreno. Not affiliated with SOMA laboratories.*
