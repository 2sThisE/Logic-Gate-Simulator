package com.logicgate.editor.io;

import java.util.ArrayList;
import java.util.List;

public class ProjectConfig {
    public String name;
    public String version = "1.0";
    public List<String> loadedMods = new ArrayList<>();

    public ProjectConfig() {}

    public ProjectConfig(String name) {
        this.name = name;
    }
}