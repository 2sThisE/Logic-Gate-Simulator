# Logic Gate Simulator

English | [한국어](README.md)

Logic Gate Simulator is a Java and JavaFX desktop application for designing and simulating digital logic circuits. You can place logic gates, connect wires, save projects, and inspect signal flow in real time.

![Java](https://img.shields.io/badge/Java-21-blue.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)
![Build](https://img.shields.io/badge/build-Maven-C71A36.svg)
![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)

## Features

- **Interactive circuit editor**: Place gates, pins, joints, and wires on a canvas.
- **Real-time simulation**: Simulate signal states and highlight active wires.
- **Built-in components**: AND, OR, NOT, NAND, NOR, XOR, XNOR, Input Pin, Output Pin, and Joint.
- **Project save/load**: Save circuits and project settings, then open them again later.
- **Mod support**: Load external JAR mods to add custom components.
- **Bilingual UI**: Korean and English are supported.
- **Built-in help**: Read usage guides and gate documentation inside the app.

## Download

Installers are available on [GitHub Releases](https://github.com/2sThisE/Logic-Gate-Simulator/releases).

- Windows: `LogicGateSimulator-x.y.z.exe`
- macOS: `LogicGateSimulator-x.y.z.dmg`
- Linux: `logicgatesimulator_x.y.z_amd64.deb`

### Windows Installation Note

The current release is not code-signed. Windows SmartScreen, Microsoft Defender, or Smart App Control may show warnings. If installation is blocked, you may need to turn Smart App Control off in Windows Security settings.

## Repository Layout

- `Logic-Gate-Mod-API/`: JavaFX-independent Java 21 mod contracts shared by the app and mods.
- `Logic-Gate-Simulator/`: Main simulator application, including the JavaFX UI, simulation engine, persistence, and built-in components.
- `Logic-Gate-Mods/`: Example and official mods.
  - `Bus-Mod/`: Adds an 8-bit bus integrator and tri-state buffer.
  - `FullAdder-Mod/`: Adds a full adder component.
  - `RAM-Mod/`: Adds a 256x8-bit RAM component.
  - `Seven-Segment-Mod/`: Adds a seven-segment display and decoder.

## Build and Run

### Requirements

- JDK 21 or later
- Maven 3.8 or later

### Build

```bash
mvn clean package -pl Logic-Gate-Simulator -am
```

### Run

```bash
mvn install -pl Logic-Gate-Mod-API -DskipTests
mvn -f Logic-Gate-Simulator/pom.xml javafx:run
```

You can also run `com.logicgate.ui.MainApp` from your IDE.

## Mod Development

Mods extend the simulator with custom components. A mod can define custom logic, symbols, and configurable properties, then package them as a JAR file that can be loaded from the app's Mod Manager.

Basic flow:

1. Create a Maven project.
2. Implement a component class extending `com.logicgate.api.component.Node`.
3. Define component metadata with `@ComponentMeta`.
4. Add a symbol based on `AbstractGateSymbol` if needed.
5. Build the mod as a JAR and load it in the app.

See the [modding guide](Logic-Gate-Mods/MODDING.md) for details.

### Mod Safety

Mods load external code into the application. Untrusted mod files may cause security risks or unexpected behavior, so only use mods from sources you trust.

## Tech Stack

- Java 21
- JavaFX 21
- Maven
- Gson
- CommonMark
- Ikonli / Material Design Icons

## License

This project is licensed under the [Apache License 2.0](LICENSE).

Third-party library and resource notices are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
