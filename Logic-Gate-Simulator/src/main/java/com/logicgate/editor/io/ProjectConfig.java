package com.logicgate.editor.io;

import java.util.ArrayList;
import java.util.List;

public class ProjectConfig {
    public String name;
    public String version = "1.0";
    public List<String> loadedMods = new ArrayList<>();

    public double tickFrequencyHz = 60.0;

    public boolean showGrid = true;
    public boolean snapToGrid = false;
    public int gridSize = 10;
    public boolean showAlignmentGuides = true;

    public String wireStyle = "Curved";
    public boolean showWireState = true;
    public String wireHighColor = "#FF3366";
    public String wireLowColor = "#555555";
    public boolean defaultShowLabel = false;

    public int autosaveIntervalMin = 0;
    public double cameraZoomSensitivity = 1.1;

    public ProjectConfig() {}

    public ProjectConfig(String name) {
        this.name = name;
    }
}
