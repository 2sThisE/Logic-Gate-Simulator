package com.logicgate.editor.state;

import com.logicgate.Circuit;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.io.ProjectData;
import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditorContext {
    private Circuit circuit;
    public List<VisualNode> visualNodes = new ArrayList<>();
    public List<VisualWire> visualWires = new ArrayList<>();
    
    // Camera
    public double cameraX = 0;
    public double cameraY = 0;
    public double zoom = 1.0;
    
    // Mouse
    public double screenMouseX, screenMouseY;
    public double worldMouseX, worldMouseY;

    // Hover state
    public VisualNode hoveredNode = null;
    public int hoveredInPin = -1;
    public int hoveredOutPin = -1;
    
    // Selection state
    public VisualNode selectedNode = null;
    public VisualWire selectedWire = null;

    // Pan state
    public boolean isPanning = false;
    public double panStartX, panStartY;

    // Drag state
    public VisualNode draggingNode = null;
    public double dragOffsetX, dragOffsetY;

    // Wiring state
    public boolean isWiring = false;
    public boolean isWiringFromOut = true;
    public VisualNode wiringNode = null;
    public int wiringPin = -1;

    // Placing state
    public boolean isPlacingImport = false;
    public ProjectData pendingProjectData = null;

    public final Set<KeyCode> activeKeys = new HashSet<>();

    public EditorContext(Circuit circuit) {
        this.circuit = circuit;
    }

    public Circuit getCircuit() { return circuit; }

    public void updateWorldCoordinates() {
        worldMouseX = (screenMouseX - cameraX) / zoom;
        worldMouseY = (screenMouseY - cameraY) / zoom;
    }
}