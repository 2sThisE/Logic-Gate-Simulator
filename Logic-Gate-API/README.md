# Logic Gate Mod API

This module contains the compile-time contracts shared by Logic Gate Simulator and external mods.
Its package names intentionally match the legacy simulator packages so existing mod source code can
move to the API artifact without import changes.

Current API surface:

- `com.logicgate.gates.Node`
- `com.logicgate.editor.model.Property`
- `com.logicgate.editor.mod.ComponentMeta`
- `com.logicgate.editor.mod.NotificationApi`

Rendering contracts remain in the simulator temporarily because they still depend on the editor's
`VisualNode`. Mods that provide custom symbols therefore need both `logicgate-api` and the simulator
artifact until the rendering API is separated.

Mods must declare this artifact with Maven `provided` scope. Do not bundle API classes inside a mod
JAR; the simulator supplies the API at runtime.
