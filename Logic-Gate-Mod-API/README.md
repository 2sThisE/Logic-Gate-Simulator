# Logic Gate Mod API

This module contains the JavaFX-independent contracts shared by Logic Gate Simulator and external
mods.

API packages:

- `com.logicgate.api.component`: nodes, properties, and component metadata
- `com.logicgate.api.rendering`: symbols, read-only symbol state, and drawing commands
- `com.logicgate.api.notification`: editor notification requests

The simulator owns circuit connections, signal propagation, JavaFX rendering adapters, registries,
class loading, and editor state. Mods must declare this artifact with Maven `provided` scope and
must not bundle API classes in their JAR.
