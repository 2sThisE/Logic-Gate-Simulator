package com.logicgate.editor.io;

import java.util.ArrayList;
import java.util.List;

public class ProjectConfig {
    public String name;
    public String version = "1.0";
    public List<String> loadedMods = new ArrayList<>();

    // 시뮬레이션 설정 ✨
    public double tickFrequencyHz = 60.0;

    // 에디터 및 그리드 설정 ✨
    public boolean showGrid = true;
    public boolean snapToGrid = false;
    public int gridSize = 20;
    public boolean showAlignmentGuides = true;

    // 시각적 설정 ✨
    public String wireStyle = "Curved"; // "Curved" 또는 "Orthogonal"
    public boolean showWireState = true;
    public String wireHighColor = "#FF3366";
    public String wireLowColor = "#555555";
    public boolean defaultShowLabel = false;

    // 편의성 설정 ✨
    public int autosaveIntervalMin = 0; // 0이면 꺼짐
    public double cameraZoomSensitivity = 1.1;

    public ProjectConfig() {}

    public ProjectConfig(String name) {
        this.name = name;
    }
}