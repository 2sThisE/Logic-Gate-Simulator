# Logic Gate Simulator Modding Guide

English | [한국어](MODDING.md)

Logic Gate Simulator supports a modding system that lets you add custom logic gates, displays, memory components, arithmetic units, and other circuit components through external JAR files.

This guide explains the basic structure of a mod component and the key rules to follow.

## 1. Modding System Overview

A mod component usually consists of two classes.

- **Node**: Defines pin counts, internal state, and simulation logic.
- **Symbol**: Defines how the component is drawn in the editor and where its pins are located.

The two classes are connected by the `typeId` in `@ComponentMeta`. The app scans mod JAR files and registers Node and Symbol classes that share the same `typeId`.

```java
@ComponentMeta(section = "Arithmetic", name = "Full Adder", typeId = "FULL_ADDER")
public class FullAdderNode extends Node {
    ...
}

@ComponentMeta(section = "Arithmetic", name = "Full Adder Symbol", typeId = "FULL_ADDER")
public class FullAdderSymbol extends AbstractGateSymbol {
    ...
}
```

## 2. Creating a Node

A Node contains the actual circuit simulation logic.

- Extend `com.logicgate.gates.Node`.
- Provide a no-argument constructor.
- Call `super(inputSize, outputSize)` in the constructor.
- Read input bits from `in` and write output bits to `out` in `compute()`.
- `in` and `out` are `int` bit fields. The current structure can represent up to 32 input/output bits.

```java
package com.example.logicgate.mods;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(section = "Logic", name = "My AND", typeId = "MY_AND")
public class MyAndNode extends Node {

    public MyAndNode() {
        super(2, 1);
    }

    @Override
    public void compute() {
        int a = in & 1;
        int b = (in >> 1) & 1;
        out = a & b;
    }
}
```

## 3. Creating a Symbol

A Symbol defines the component shape, size, pin positions, and tooltip names.

- Extend `com.logicgate.editor.rendering.symbol.AbstractGateSymbol`.
- Add `@ComponentMeta` with the same `typeId` as the Node.
- Return an SVG path string from `getSvgPathData()`.
- If the default pin placement is enough, you do not need to override `getInPinX/Y` or `getOutPinX/Y`.

```java
package com.example.logicgate.mods;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;

@ComponentMeta(section = "Logic", name = "My AND Symbol", typeId = "MY_AND")
public class MyAndSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format(
            "M 0 0 L %f 0 L %f %f L 0 %f Z",
            vn.width, vn.width, vn.height, vn.height
        );
    }

    @Override
    public String getDefaultLabel() {
        return "MY AND";
    }

    @Override
    public String getInPinName(int index) {
        return index == 0 ? "A" : "B";
    }

    @Override
    public String getOutPinName(int index) {
        return "OUT";
    }

    @Override
    public int getUnitWidth() {
        return 8;
    }

    @Override
    public int getUnitHeight() {
        return 6;
    }
}
```

## 4. Adding Properties

Component properties are shown in the right-side property panel. If a value must be saved, loaded, and restored through undo/redo, store it in the `properties` map as well.

Supported types:

- `COLOR`
- `BOOLEAN`
- `INTEGER`
- `STRING`
- `CHOICE`

```java
import com.logicgate.editor.model.Property;
import java.util.List;

private String color = "#336699";
private boolean inverted = false;

@Override
public List<Property<?>> getComponentProperties() {
    List<Property<?>> props = super.getComponentProperties();

    props.add(new Property<>("Color", color, Property.Type.COLOR, value -> {
        color = (String) value;
        properties.put("color", color);
    }));

    props.add(new Property<>("Inverted", inverted, Property.Type.BOOLEAN, value -> {
        inverted = (Boolean) value;
        properties.put("inverted", Boolean.toString(inverted));
    }));

    return props;
}

@Override
protected void applyProperties() {
    if (properties.containsKey("color")) {
        color = properties.get("color");
    }
    if (properties.containsKey("inverted")) {
        inverted = Boolean.parseBoolean(properties.get("inverted"));
    }
}
```

## 5. Showing Notifications

Mods can use `NotificationApi` to show info, warning, and error notifications in the editor's bottom-right corner. Use notifications for short status updates or issues the user should notice.

```java
import com.logicgate.editor.mod.NotificationApi;

NotificationApi.info("Output complete", "The 16-bit computer wrote a value.");
NotificationApi.warning("Input out of range", "The input value is outside the 16-bit range.");
NotificationApi.error("Execution error", "The instruction could not be decoded.");
```

You can also pass the type explicitly.

```java
NotificationApi.show(
    NotificationApi.Type.WARNING,
    "Check required",
    "Some input pins are not connected."
);
```

The title and body are passed as plain strings. If your mod needs localization, include your own `ResourceBundle` in the mod JAR and pass strings for the current `Locale`.

```java
import java.util.Locale;
import java.util.ResourceBundle;

ResourceBundle bundle = ResourceBundle.getBundle("com.example.my_mod.strings", Locale.getDefault());

NotificationApi.warning(
    bundle.getString("warning.title"),
    bundle.getString("warning.body")
);
```

`compute()` is called very frequently, so do not show a notification every time it runs. Track a flag or the last state inside your mod so the same condition only notifies once.

```java
private boolean warnedMissingInput = false;

@Override
public void compute() {
    boolean missingInput = (in & 1) == 0;
    if (missingInput && !warnedMissingInput) {
        NotificationApi.warning("Missing input", "IN0 is LOW.");
        warnedMissingInput = true;
    } else if (!missingInput) {
        warnedMissingInput = false;
    }
}
```

## 6. Adding Help Documents

If your mod JAR includes Markdown files in the expected location, the app can load them into the Help window.

1. Create `src/main/resources/META-INF/logicgate/help/`.
2. Add `.md` files inside that directory.
3. The first `# Heading` is used as the help item title.

Example:

```markdown
# My AND

My AND is an example component whose output is HIGH when both inputs are HIGH.
```

## 7. Maven Project Setup

A mod project should depend on the separately packaged Mod API with `provided` scope. The app supplies API classes at runtime, so do not bundle them in the mod JAR. Starting from one of the example mod `pom.xml` files is recommended.

```xml
<dependency>
  <groupId>com.logicgate</groupId>
  <artifactId>logicgate-api</artifactId>
  <version>1.1.1-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```

The custom Symbol API still depends on the simulator's `VisualNode`. Mods that provide custom symbols must temporarily add the following compatibility dependency in addition to the API:
The mod API and simulator rendering classes are both built for Java 21, so all mods must use JDK 21.

```xml
<dependency>
  <groupId>com.logicgate</groupId>
  <artifactId>logicgate</artifactId>
  <version>1.1.1-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>

<dependency>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-controls</artifactId>
  <version>21</version>
  <scope>provided</scope>
</dependency>
```

For a logic-only component, install just the API into your local Maven repository from the repository root:

```bash
mvn install -pl Logic-Gate-API
```

If the mod also defines a custom JavaFX symbol, install the simulator too because it is currently a temporary compatibility dependency:

```bash
mvn install -pl Logic-Gate-Simulator -am -DskipTests
```

Build:

```bash
mvn clean package
```

Then load the generated JAR file from the app's **Mod Manager**.

## 8. Example Mods

Example mods are included under `Logic-Gate-Mods/`.

- `FullAdder-Mod`
- `Bus-Mod`
- `RAM-Mod`
- `Seven-Segment-Mod`

Use their structure and `pom.xml` files as references when creating a new mod.

## Notes

- Node and Symbol classes both need no-argument constructors.
- `compute()` is called repeatedly from the simulation thread. Do not directly manipulate JavaFX UI objects there.
- If you use notifications from `compute()`, add deduplication logic so the same state does not show repeated notifications.
- Notification localization should be handled inside your mod with a `ResourceBundle`, then passed as strings.
- `typeId` must be unique. Use a prefix to avoid collisions with built-in components or other mods.
- Values that need persistence must also be written to the `properties` map.
- Do not run untrusted JAR mods. Mods load external code into the app.
