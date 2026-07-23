package com.logicgate.editor.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.logicgate.Circuit;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import javafx.geometry.Point2D;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    public LauncherController.ProjectResult showLauncher(Stage owner, boolean projectInitialized) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
            URL launcherFxml = getClass().getResource("/com/logicgate/ui/launcher.fxml");
            FXMLLoader loader = new FXMLLoader(launcherFxml, bundle);
            Parent root = loader.load();

            Stage launcherStage = new Stage();
            launcherStage.initModality(Modality.APPLICATION_MODAL);
            launcherStage.initOwner(owner);
            launcherStage.setTitle(bundle.getString("launcher.title"));

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

            launcherStage.showAndWait();

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
        context.setDirty(false);
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
            if (!loadBinaryCircuit(lgsFile)) {
                loadWarnings.add("회로 파일을 읽지 못해 기존 회로를 유지했습니다.");
                return;
            }
        }
        context.historyManager.clear();
        context.setDirty(false);
    }

    public java.util.List<String> consumeLoadWarnings() {
        java.util.List<String> copy = new java.util.ArrayList<>(loadWarnings);
        loadWarnings.clear();
        return copy;
    }

    public boolean saveCurrentProject() {
        if (context.projectRoot == null) return false;

        File lgsFile = new File(context.projectRoot, "circuit.lgs");
        boolean saved = saveBinaryCircuit(lgsFile) && saveProjectConfig();
        if (!saved) return false;
        System.out.println("프로젝트 전체 저장 완료: " + context.projectRoot.getAbsolutePath());
        context.setDirty(false);
        return true;
    }

    public boolean saveProjectConfig() {
        if (context.projectRoot == null || context.projectConfig == null) return false;
        File prjFile = new File(context.projectRoot, "project.prj");
        try {
            writeStringAtomically(prjFile.toPath(), gson.toJson(context.projectConfig));
            System.out.println("프로젝트 설정 저장 완료: " + prjFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean saveBinaryCircuit(File file) {
        Path target = file.toPath();
        Path parent = target.toAbsolutePath().getParent();
        Path temporary = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            temporary = Files.createTempFile(parent, file.getName(), ".tmp");
            try (java.io.DataOutputStream dos = new java.io.DataOutputStream(Files.newOutputStream(temporary))) {
                dos.writeInt(0x4C475321); // Magic Number
                dos.writeInt(8);          // Version 8 (선 고정 속성 추가)

                dos.writeInt(context.visualNodes.size());
                for (VisualNode vn : context.visualNodes) {
                    dos.writeUTF(vn.node.getTypeId());
                    dos.writeDouble(vn.x);
                    dos.writeDouble(vn.y);
                    dos.writeDouble(vn.rotation);
                    dos.writeUTF(vn.label != null ? vn.label : "");
                    dos.writeBoolean(vn.showLabel);
                    dos.writeUTF(vn.group != null ? vn.group : "");
                    dos.writeBoolean(vn.locked);

                    Map<String, String> properties = vn.node.getProperties();
                    dos.writeInt(properties.size());
                    for (Map.Entry<String, String> entry : properties.entrySet()) {
                        dos.writeUTF(entry.getKey() != null ? entry.getKey() : "");
                        dos.writeUTF(entry.getValue() != null ? entry.getValue() : "");
                    }
                }

                dos.writeInt(context.visualWires.size());
                for (VisualWire vw : context.visualWires) {
                    dos.writeInt(context.visualNodes.indexOf(vw.from));
                    dos.writeInt(vw.outPin);
                    dos.writeInt(context.visualNodes.indexOf(vw.to));
                    dos.writeInt(vw.inPin);
                    dos.writeBoolean(vw.locked);

                    dos.writeUTF(vw.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null).name());
                    dos.writeInt(vw.bendPoints.size());
                    for (Point2D point : vw.bendPoints) {
                        dos.writeDouble(point.getX());
                        dos.writeDouble(point.getY());
                    }
                }
            }
            replaceAtomically(temporary, target);
            temporary = null;
            return true;
        } catch (Exception e) {
            System.err.println("회로 파일 저장 실패: " + e.getClass().getSimpleName());
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean loadBinaryCircuit(File file) {
        try (java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.FileInputStream(file))) {
            if (dis.readInt() != 0x4C475321) throw new IOException("유효하지 않은 LGS 파일입니다.");
            int version = dis.readInt();

            Circuit loadedCircuit = new Circuit();
            java.util.List<VisualNode> loadedNodes = new java.util.ArrayList<>();
            java.util.List<VisualWire> loadedWires = new java.util.ArrayList<>();
            int nodeCount = dis.readInt();
            java.util.List<VisualNode> nodesByOriginalIndex = new java.util.ArrayList<>(nodeCount);
            for (int i = 0; i < nodeCount; i++) {
                String type = dis.readUTF();
                double x = dis.readDouble();
                double y = dis.readDouble();
                double rotation = (version >= 3) ? dis.readDouble() : 0;
                String label = dis.readUTF();
                boolean showLabel = dis.readBoolean();
                String group = null;
                if (version >= 2) {
                    group = dis.readUTF();
                    if (group.isEmpty()) group = null;
                }
                boolean locked = false;
                if (version >= 7) {
                    locked = dis.readBoolean();
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

                com.logicgate.api.component.Node logicNode = com.logicgate.editor.utils.NodeFactory.createNodeByType(type);
                if (logicNode != null) {
                    logicNode.setProperties(properties);
                    loadedCircuit.addNode(logicNode);
                    VisualNode vn = new VisualNode(logicNode, x, y, label);
                    vn.showLabel = showLabel;
                    vn.locked = locked;
                    vn.rotation = rotation;
                    vn.group = group;
                    loadedNodes.add(vn);
                    nodesByOriginalIndex.add(vn);
                } else {
                    nodesByOriginalIndex.add(null);
                    loadWarnings.add("노드 타입을 찾을 수 없어 건너뜀: " + type);
                }
            }

            int wireCount = dis.readInt();
            for (int i = 0; i < wireCount; i++) {
                int fromIdx = dis.readInt();
                int outPin = dis.readInt();
                int toIdx = dis.readInt();
                int inPin = dis.readInt();
                boolean wireLocked = false;
                if (version >= 8) {
                    wireLocked = dis.readBoolean();
                }
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

                if (fromIdx >= 0 && fromIdx < nodesByOriginalIndex.size() &&
                    toIdx >= 0 && toIdx < nodesByOriginalIndex.size()) {

                    VisualNode fromVn = nodesByOriginalIndex.get(fromIdx);
                    VisualNode toVn = nodesByOriginalIndex.get(toIdx);
                    if (fromVn == null || toVn == null ||
                        outPin < 0 || outPin >= fromVn.node.getOutputSize() ||
                        inPin < 0 || inPin >= toVn.node.getInputSize()) {
                        loadWarnings.add("유효하지 않은 연결을 건너뜀: " + fromIdx + ":" + outPin +
                            " -> " + toIdx + ":" + inPin);
                        continue;
                    }

                    loadedCircuit.connect(fromVn.node, outPin, toVn.node, inPin);
                    VisualWire vw = new VisualWire(fromVn, outPin, toVn, inPin);
                    if (routeMode != null) {
                        vw.routeMode = routeMode;
                    } else {
                        vw.setRouteModeFromProjectStyle(context.projectConfig != null ? context.projectConfig.wireStyle : null);
                    }
                    vw.locked = wireLocked;
                    vw.bendPoints.addAll(bendPoints);

                    loadedWires.add(vw);
                    // Advance once after each restored connection to reduce synchronized oscillator artifacts.
                    loadedCircuit.tick();
                }
            }

            context.getCircuit().replaceContentsFrom(loadedCircuit);
            context.visualNodes.clear();
            context.visualNodes.addAll(loadedNodes);
            context.visualWires.clear();
            context.visualWires.addAll(loadedWires);
            return true;
        } catch (Exception e) {
            System.err.println("회로 파일 로드 실패: " + e.getMessage());
            return false;
        }
    }

    private void writeStringAtomically(Path target, String content) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, content);
            replaceAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void replaceAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void exportJson(Window window) {
        if (context.visualNodes.isEmpty()) return;

        ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("dialog.export.title"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("file_filter.logicgate_json"), "*.json"));
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try {
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
                        vn.x - cx, vn.y - cy, vn.rotation, vn.label, vn.showLabel, vn.locked, vn.group
                    );
                    nd.properties.putAll(vn.node.getProperties());
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("file_filter.logicgate_json"), "*.json"));
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                ProjectData data = gson.fromJson(json, ProjectData.class);
                if (!isValidProjectData(data)) {
                    notifyError(
                        bundle.getString("notification.import_fail.title"),
                        bundle.getString("notification.import_fail.format")
                    );
                    return;
                }
                normalizeProjectData(data);
                context.pendingProjectData = data;
                context.isPlacingImport = true;
                context.placingRotation = 0;
            } catch (IOException | JsonSyntaxException e) {
                notifyError(
                    bundle.getString("notification.import_fail.title"),
                    bundle.getString("notification.import_fail.read") + "\n" + e.getMessage()
                );
            }
        }
    }

    public void copyToClipboard() {
        if (context.selectedNodes.isEmpty()) return;

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
                vn.x - cx, vn.y - cy, vn.rotation, vn.label, vn.showLabel, vn.locked, vn.group
            );
            nd.properties.putAll(vn.node.getProperties());
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
                    context.placingRotation = 0;
                }
            } catch (JsonSyntaxException e) {
                notifyError(
                    getNotificationText("notification.paste_fail.title", "Paste failed"),
                    getNotificationText("notification.paste_fail.body", "Clipboard data is not a valid LogicGate circuit.")
                );
            }
        }
    }

    private boolean isValidProjectData(ProjectData data) {
        return data != null && data.nodes != null;
    }

    private void notifyError(String title, String body) {
        context.notify(EditorContext.NotificationType.ERROR, title, body);
    }

    private String getNotificationText(String key, String fallback) {
        try {
            return ResourceBundle
                .getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault())
                .getString(key);
        } catch (Exception e) {
            return fallback;
        }
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
        data.routeMode = wire.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null).name();
        data.locked = wire.locked;
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
