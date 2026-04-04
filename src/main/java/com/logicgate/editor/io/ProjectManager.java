package com.logicgate.editor.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProjectManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final EditorContext context;

    public ProjectManager(EditorContext context) {
        this.context = context;
    }

    public void saveProject(Window window) {
        if (context.visualNodes.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("회로 저장 (상대 좌표)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicGate Files", "*.json"));
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try {
                double minX = Double.MAX_VALUE;
                double minY = Double.MAX_VALUE;
                for (VisualNode vn : context.visualNodes) {
                    minX = Math.min(minX, vn.x);
                    minY = Math.min(minY, vn.y);
                }

                ProjectData data = new ProjectData();
                for (VisualNode vn : context.visualNodes) {
                    data.nodes.add(new NodeData(
                        vn.node.getClass().getSimpleName(),
                        vn.x - minX, vn.y - minY, vn.label
                    ));
                }
                for (VisualWire vw : context.visualWires) {
                    data.wires.add(new WireData(
                        context.visualNodes.indexOf(vw.from),
                        vw.outPin,
                        context.visualNodes.indexOf(vw.to),
                        vw.inPin
                    ));
                }
                String json = gson.toJson(data);
                Files.writeString(file.toPath(), json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadProject(Window window) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("붙여넣을 회로 선택");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicGate Files", "*.json"));
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                context.pendingProjectData = gson.fromJson(json, ProjectData.class);
                context.isPlacingImport = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}