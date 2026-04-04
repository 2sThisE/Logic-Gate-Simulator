package com.logicgate.editor.state;

import com.logicgate.Circuit;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.io.ProjectConfig;
import com.logicgate.editor.io.ProjectData;
import javafx.scene.input.KeyCode;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditorContext {
    private Circuit circuit;
    public File projectRoot;
    public ProjectConfig projectConfig;
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
    public String hoveredPinName = null;
    public double tooltipX = 0;
    public double tooltipY = 0;
    
    // Selection state
    private VisualNode selectedNode = null;
    public VisualWire selectedWire = null;
    public Runnable onSelectionChanged;

    // Dirty state (저장 여부 표시용 ✨)
    public Runnable onDirtyChanged;
    public Runnable onSaveRequested;
    private boolean isDirty = false;

    public boolean isDirty() { return isDirty; }
    
    public void setDirty(boolean dirty) {
        if (this.isDirty != dirty) {
            this.isDirty = dirty;
            if (onDirtyChanged != null) {
                onDirtyChanged.run();
            }
        }
    }

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

    public VisualNode getSelectedNode() { return selectedNode; }

    public void setSelectedNode(VisualNode node) {
        if (this.selectedNode != node) {
            this.selectedNode = node;
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        }
    }

    public void updateWorldCoordinates() {
        worldMouseX = (screenMouseX - cameraX) / zoom;
        worldMouseY = (screenMouseY - cameraY) / zoom;
    }
}