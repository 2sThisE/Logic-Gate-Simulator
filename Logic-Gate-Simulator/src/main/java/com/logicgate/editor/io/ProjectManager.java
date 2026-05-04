package com.logicgate.editor.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import javafx.geometry.Point2D;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.logicgate.ui.MainApp;
import com.logicgate.ui.LauncherController;
import java.net.URL;

public class ProjectManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final EditorContext context;
    private final java.util.List<String> loadWarnings = new java.util.ArrayList<>();

    public ProjectManager(EditorContext context) {
        this.context = context;
    }

    /**
     * 프로젝트 매니저(런처) 창을 띄우고 사용자의 선택 결과를 반환합니다. ✨
     */
    public LauncherController.ProjectResult showLauncher(Stage owner, boolean projectInitialized) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
            URL launcherFxml = getClass().getResource("/com/logicgate/ui/launcher.fxml");
            FXMLLoader loader = new FXMLLoader(launcherFxml, bundle);
            Parent root = loader.load();
            
            Stage launcherStage = new Stage();
            launcherStage.initModality(Modality.APPLICATION_MODAL);
            launcherStage.initOwner(owner);
            launcherStage.setTitle("Project Manager");
            
            launcherStage.setOnCloseRequest(event -> {
                if (!projectInitialized) {
                    javafx.application.Platform.exit();
                    System.exit(0);
                }
            });
            
            LauncherController controller = loader.getController();
            controller.setStage(launcherStage);

            Scene scene = new Scene(root, 800, 500);
            launcherStage.setScene(scene);
            launcherStage.setResizable(true);
            
            // 창이 닫힐 때까지 대기 ✨
            launcherStage.showAndWait();
            
            // 컨트롤러로부터 결과 받아오기 ✨
            return controller.getResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void initNewProject() {
        if (context.projectRoot == null) return;

        File prjFile = new File(context.projectRoot, "project.prj");
        File modsDir = new File(context.projectRoot, "mods");
        
        if (!modsDir.exists()) {
            modsDir.mkdirs();
        }

        ProjectConfig config = new ProjectConfig(context.projectRoot.getName());
        context.projectConfig = config;
        try {
            Files.writeString(prjFile.toPath(), gson.toJson(config));
            File lgsFile = new File(context.projectRoot, "circuit.lgs");
            if (!lgsFile.exists()) {
                lgsFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        context.historyManager.clear();
        context.setDirty(false); // 초기화 ✨
    }

    public void loadProjectConfigOnly() {
        if (context.projectRoot == null) return;

        File prjFile = new File(context.projectRoot, "project.prj");
        if (prjFile.exists()) {
            try {
                String json = Files.readString(prjFile.toPath());
                ProjectConfig config = gson.fromJson(json, ProjectConfig.class);
                context.projectConfig = config;
                System.out.println("프로젝트 설정 로드: " + config.name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadCircuitOnly() {
        if (context.projectRoot == null) return;
        loadWarnings.clear();
        File lgsFile = new File(context.projectRoot, "circuit.lgs");
        if (lgsFile.exists() && lgsFile.length() > 0) {
            loadBinaryCircuit(lgsFile);
        }
        context.historyManager.clear();
        context.setDirty(false); // 초기화 ✨
    }

    public java.util.List<String> consumeLoadWarnings() {
        java.util.List<String> copy = new java.util.ArrayList<>(loadWarnings);
        loadWarnings.clear();
        return copy;
    }

    public void saveCurrentProject() {
        if (context.projectRoot == null) return;
        
        File lgsFile = new File(context.projectRoot, "circuit.lgs");
        saveBinaryCircuit(lgsFile);
        saveProjectConfig();
        System.out.println("프로젝트 전체 저장 완료: " + context.projectRoot.getAbsolutePath());
        context.setDirty(false); // 초기화 ✨
    }

    public void saveProjectConfig() {
        if (context.projectRoot == null || context.projectConfig == null) return;
        File prjFile = new File(context.projectRoot, "project.prj");
        try {
            Files.writeString(prjFile.toPath(), gson.toJson(context.projectConfig));
            System.out.println("프로젝트 설정 저장 완료: " + prjFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveBinaryCircuit(File file) {
        try (java.io.DataOutputStream dos = new java.io.DataOutputStream(new java.io.FileOutputStream(file))) {
            // 1. Header
            dos.writeInt(0x4C475321); // Magic Number
            dos.writeInt(6);          // Version 6 (Wire route/bend points 추가)

            // 2. Nodes
            dos.writeInt(context.visualNodes.size());
            for (VisualNode vn : context.visualNodes) {
                dos.writeUTF(vn.node.getTypeId());
                dos.writeDouble(vn.x);
                dos.writeDouble(vn.y);
                dos.writeDouble(vn.rotation); // 회전각 추가 ✨
                dos.writeUTF(vn.label != null ? vn.label : "");
                dos.writeBoolean(vn.showLabel);
                dos.writeUTF(vn.group != null ? vn.group : "");

                Map<String, String> properties = vn.node.getProperties();
                dos.writeInt(properties.size());
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    dos.writeUTF(entry.getKey() != null ? entry.getKey() : "");
                    dos.writeUTF(entry.getValue() != null ? entry.getValue() : "");
                }
            }

            // 3. Wires
            dos.writeInt(context.visualWires.size());
            for (VisualWire vw : context.visualWires) {
                dos.writeInt(context.visualNodes.indexOf(vw.from));
                dos.writeInt(vw.outPin);
                dos.writeInt(context.visualNodes.indexOf(vw.to));
                dos.writeInt(vw.inPin);
                
                dos.writeUTF(vw.routeMode != null ? vw.routeMode.name() : "");
                dos.writeInt(vw.bendPoints.size());
                for (Point2D point : vw.bendPoints) {
                    dos.writeDouble(point.getX());
                    dos.writeDouble(point.getY());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBinaryCircuit(File file) {
        try (java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.FileInputStream(file))) {
            if (dis.readInt() != 0x4C475321) throw new IOException("유효하지 않은 LGS 파일입니다.");
            int version = dis.readInt();

            context.visualNodes.clear();
            context.visualWires.clear();
            context.getCircuit().clear(); // 회로 엔진도 초기화

            int nodeCount = dis.readInt();
            for (int i = 0; i < nodeCount; i++) {
                String type = dis.readUTF();
                double x = dis.readDouble();
                double y = dis.readDouble();
                double rotation = (version >= 3) ? dis.readDouble() : 0; // 버전 3부터 회전각 ✨
                String label = dis.readUTF();
                boolean showLabel = dis.readBoolean();
                String group = null;
                if (version >= 2) {
                    group = dis.readUTF();
                    if (group.isEmpty()) group = null;
                }

                Map<String, String> properties = new HashMap<>();
                if (version >= 5) {
                    int propertyCount = dis.readInt();
                    for (int j = 0; j < propertyCount; j++) {
                        String key = dis.readUTF();
                        String value = dis.readUTF();
                        if (!key.isEmpty()) {
                            properties.put(key, value);
                        }
                    }
                }

                com.logicgate.gates.Node logicNode = com.logicgate.editor.utils.NodeFactory.createNodeByType(type);
                if (logicNode != null) {
                    logicNode.setProperties(properties);
                    context.getCircuit().addNode(logicNode);
                    VisualNode vn = new VisualNode(logicNode, x, y, label);
                    vn.showLabel = showLabel;
                    vn.rotation = rotation;
                    vn.group = group;
                    context.visualNodes.add(vn);
                } else {
                    loadWarnings.add("노드 타입을 찾을 수 없어 건너뜀: " + type);
                }
            }

            int wireCount = dis.readInt();
            for (int i = 0; i < wireCount; i++) {
                int fromIdx = dis.readInt();
                int outPin = dis.readInt();
                int toIdx = dis.readInt();
                int inPin = dis.readInt();
                VisualWire.RouteMode routeMode = null;
                java.util.List<Point2D> bendPoints = new java.util.ArrayList<>();

                if (version >= 6) {
                    String routeModeName = dis.readUTF();
                    if (!routeModeName.isEmpty()) {
                        try {
                            routeMode = VisualWire.RouteMode.valueOf(routeModeName);
                        } catch (IllegalArgumentException ignored) {
                            routeMode = null;
                        }
                    }
                    int bendCount = dis.readInt();
                    for (int j = 0; j < bendCount; j++) {
                        bendPoints.add(new Point2D(dis.readDouble(), dis.readDouble()));
                    }
                } else if (version >= 4) {
                    int wpCount = dis.readInt();
                    for (int j = 0; j < wpCount; j++) {
                        bendPoints.add(new Point2D(dis.readDouble(), dis.readDouble()));
                    }
                }

                if (fromIdx >= 0 && fromIdx < context.visualNodes.size() &&
                    toIdx >= 0 && toIdx < context.visualNodes.size()) {
                    
                    VisualNode fromVn = context.visualNodes.get(fromIdx);
                    VisualNode toVn = context.visualNodes.get(toIdx);
                    
                    context.getCircuit().connect(fromVn.node, outPin, toVn.node, inPin);
                    VisualWire vw = new VisualWire(fromVn, outPin, toVn, inPin);
                    vw.routeMode = routeMode;
                    vw.bendPoints.addAll(bendPoints);
                    
                    context.visualWires.add(vw);
                    // 인위적인 틱을 발생시켜 완벽한 동기화로 인한 발진(Ring Oscillator) 방지 ✨
                    context.getCircuit().tick();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportJson(Window window) {
        if (context.visualNodes.isEmpty()) return;

        ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("dialog.export.title"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicGate Files", "*.json"));
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try {
                // 중앙 좌표 기준 계산 🔪💕
                double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                for (VisualNode vn : context.visualNodes) {
                    minX = Math.min(minX, vn.x); minY = Math.min(minY, vn.y);
                    maxX = Math.max(maxX, vn.x + vn.width); maxY = Math.max(maxY, vn.y + vn.height);
                }
                double cx = (minX + maxX) / 2;
                double cy = (minY + maxY) / 2;

                ProjectData data = new ProjectData();
                for (VisualNode vn : context.visualNodes) {
                    NodeData nd = new NodeData(
                        vn.node.getTypeId(),
                        vn.x - cx, vn.y - cy, vn.rotation, vn.label, vn.showLabel, vn.group
                    );
                    nd.properties.putAll(vn.node.getProperties()); // 속성 포함 ✨
                    data.nodes.add(nd);
                }
                for (VisualWire vw : context.visualWires) {
                    WireData wd = new WireData(
                        context.visualNodes.indexOf(vw.from),
                        vw.outPin,
                        context.visualNodes.indexOf(vw.to),
                        vw.inPin
                    );
                    copyWireRouteToData(vw, wd, -cx, -cy, 0);
                    data.wires.add(wd);
                }
                String json = gson.toJson(data);
                Files.writeString(file.toPath(), json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void importJson(Window window) {
        ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("dialog.import.title"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicGate Files", "*.json"));
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                ProjectData data = gson.fromJson(json, ProjectData.class);
                if (!isValidProjectData(data)) {
                    showError(bundle.getString("alert.import_fail.title"), bundle.getString("alert.import_fail.format"));
                    return;
                }
                normalizeProjectData(data);
                context.pendingProjectData = data;
                context.isPlacingImport = true;
                context.placingRotation = 0; // 붙여넣기 모드 진입 시 회전각 초기화 ✨
            } catch (IOException | JsonSyntaxException e) {
                showError(bundle.getString("alert.import_fail.title"), bundle.getString("alert.import_fail.read") + "\n" + e.getMessage());
            }
        }
    }

    public void copyToClipboard() {
        if (context.selectedNodes.isEmpty()) return;

        // 선택 영역의 중앙점 찾기 🔪💕
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (VisualNode vn : context.selectedNodes) {
            minX = Math.min(minX, vn.x); minY = Math.min(minY, vn.y);
            maxX = Math.max(maxX, vn.x + vn.width); maxY = Math.max(maxY, vn.y + vn.height);
        }
        double cx = (minX + maxX) / 2;
        double cy = (minY + maxY) / 2;

        ProjectData data = new ProjectData();
        java.util.List<VisualNode> copiedNodes = new java.util.ArrayList<>(context.selectedNodes);
        
        for (VisualNode vn : copiedNodes) {
            NodeData nd = new NodeData(
                vn.node.getTypeId(),
                vn.x - cx, vn.y - cy, vn.rotation, vn.label, vn.showLabel, vn.group
            );
            nd.properties.putAll(vn.node.getProperties()); // 속성 복사 추가 💖
            data.nodes.add(nd);
        }

        for (VisualWire vw : context.visualWires) {
            int fromIdx = copiedNodes.indexOf(vw.from);
            int toIdx = copiedNodes.indexOf(vw.to);
            if (fromIdx != -1 && toIdx != -1) {
                WireData wd = new WireData(fromIdx, vw.outPin, toIdx, vw.inPin);
                copyWireRouteToData(vw, wd, -cx, -cy, 0);
                data.wires.add(wd);
            }
        }

        String json = gson.toJson(data);
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(json);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
    }

    public void pasteFromClipboard() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String json = clipboard.getString();
            try {
                ProjectData data = gson.fromJson(json, ProjectData.class);
                if (isValidProjectData(data)) {
                    normalizeProjectData(data);
                    context.pendingProjectData = data;
                    context.isPlacingImport = true;
                    context.placingRotation = 0; // 초기화 ✨
                }
            } catch (JsonSyntaxException e) {
                // JSON 파싱 실패 시 무시 (외부 텍스트 복사 등)
                System.out.println("붙여넣기 데이터가 올바른 JSON 회로 형식이 아닙니다.");
            }
        }
    }

    private boolean isValidProjectData(ProjectData data) {
        return data != null && data.nodes != null;
    }

    private void showError(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void normalizeProjectData(ProjectData data) {
        if (data.wires == null) {
            data.wires = new java.util.ArrayList<>();
        }
        for (NodeData node : data.nodes) {
            if (node.properties == null) {
                node.properties = new HashMap<>();
            }
        }
        for (WireData wire : data.wires) {
            if (wire.bendPoints == null) {
                wire.bendPoints = new java.util.ArrayList<>();
            }
        }
    }

    private void copyWireRouteToData(VisualWire wire, WireData data, double offsetX, double offsetY, double rotationDegrees) {
        if (wire.routeMode != null) {
            data.routeMode = wire.routeMode.name();
        }
        double rad = Math.toRadians(rotationDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        for (Point2D point : wire.bendPoints) {
            double translatedX = point.getX() + offsetX;
            double translatedY = point.getY() + offsetY;
            double rotatedX = translatedX * cos - translatedY * sin;
            double rotatedY = translatedX * sin + translatedY * cos;
            data.bendPoints.add(new WireData.PointData(rotatedX, rotatedY));
        }
    }
}
