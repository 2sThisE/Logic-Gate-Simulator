package com.logicgate.ui;

import com.logicgate.Circuit;
import com.logicgate.editor.interaction.KeyboardInteractionHandler;
import com.logicgate.editor.interaction.MouseInteractionHandler;
import com.logicgate.editor.interaction.WiringManager;
import com.logicgate.editor.io.ProjectManager;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.CanvasRenderer;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.gates.*;

import com.logicgate.editor.mod.ModLoader;
import com.logicgate.editor.mod.ModComponentInfo;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainController {

    @FXML private ListView<String> consoleListView;
    @FXML private Button errorButton;

    private ObservableList<String> consoleMessages = FXCollections.observableArrayList();
    private int errorCount = 0;

    @FXML
    private Pane canvasPane;
    @FXML
    private Canvas simulationCanvas;
    @FXML
    private TreeView<String> componentTreeView;
    @FXML
    private VBox propertyPane;
    @FXML
    private TextField labelTextField;
    @FXML
    private CheckBox showLabelCheckBox;

    private TextField groupNameField;
    private javafx.scene.control.ContextMenu contextMenu;

    private Circuit circuit;
    private EditorContext context;
    private ProjectManager projectManager;
    private WiringManager wiringManager;
    private CanvasRenderer renderer;
    private MouseInteractionHandler mouseHandler;
    private KeyboardInteractionHandler keyboardHandler;
    private AnimationTimer timer;
    
    private Map<String, String> customComponentMap = new HashMap<>();
    private javafx.stage.Stage primaryStage;

    @FXML
    public void initialize() {
        circuit = new Circuit();
        circuit.setTickDelayMs(16);

        context = new EditorContext(circuit);
        wiringManager = new WiringManager(context);
        projectManager = new ProjectManager(context);
        mouseHandler = new MouseInteractionHandler(context, wiringManager);
        keyboardHandler = new KeyboardInteractionHandler(context);
        renderer = new CanvasRenderer(simulationCanvas, context, wiringManager);

        setupComponentTreeView();
        setupPropertyPane();
        setupContextMenu();

        simulationCanvas.widthProperty().bind(canvasPane.widthProperty());
        simulationCanvas.heightProperty().bind(canvasPane.heightProperty());

        simulationCanvas.setFocusTraversable(true);
        simulationCanvas.setOnMouseEntered(e -> simulationCanvas.requestFocus());

        simulationCanvas.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) context.activeKeys.clear();
        });
        simulationCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.focusedProperty().addListener((obs3, oldF, newF) -> {
                            if (!newF) context.activeKeys.clear();
                        });
                    }
                });
            }
        });

        simulationCanvas.setOnMousePressed(e -> {
            if (contextMenu != null && contextMenu.isShowing()) contextMenu.hide();
            mouseHandler.handleMousePressed(e);
        });
        simulationCanvas.setOnMouseDragged(mouseHandler::handleMouseDragged);
        simulationCanvas.setOnMouseReleased(mouseHandler::handleMouseReleased);
        simulationCanvas.setOnScroll(mouseHandler::handleMouseScrolled);
        simulationCanvas.setOnMouseMoved(mouseHandler::handleMouseMoved);

        simulationCanvas.setOnKeyPressed(keyboardHandler::handleKeyPressed);
        simulationCanvas.setOnKeyReleased(keyboardHandler::handleKeyReleased);

        context.onContextMenuRequested = this::updateAndShowContextMenu;
        context.onCopyRequested = projectManager::copyToClipboard;
        context.onPasteRequested = projectManager::pasteFromClipboard;
        
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                keyboardHandler.updateCamera();
                renderer.draw();
            }
        };
        timer.start();

        circuit.startSimulation();

        // 💖 로그 패널 설정
        if (consoleListView != null) {
            consoleListView.setItems(consoleMessages);
            redirectSystemOutAndErr();
            setupLogContextMenu();
        }
    }

    private void setupLogContextMenu() {
        if (errorButton == null) return;

        javafx.scene.control.ContextMenu logMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem clearItem = new javafx.scene.control.MenuItem("로그 지우기");
        clearItem.setOnAction(e -> clearLogs());
        logMenu.getItems().add(clearItem);

        errorButton.setContextMenu(logMenu);
    }

    private void clearLogs() {
        consoleMessages.clear();
        errorCount = 0;
        if (errorButton != null) {
            errorButton.setText("0 로그");
            errorButton.getStyleClass().remove("status-btn-error");
        }
    }

    private void redirectSystemOutAndErr() {
        PrintStream originalErr = System.err;

        System.setErr(new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();
            @Override
            public void write(int b) {
                originalErr.write(b);
                if (b == '\n') {
                    String msg = buffer.toString();
                    buffer.setLength(0);
                    Platform.runLater(() -> addLog("[ERROR] " + msg, true));
                } else if (b != '\r') {
                    buffer.append((char) b);
                }
            }
        }, true));

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Platform.runLater(() -> addLog("[FATAL] Uncaught Exception in thread " + t.getName() + ": " + e.toString(), true));
            e.printStackTrace();
        });
    }

    private void addLog(String message, boolean isError) {
        consoleMessages.add(message);
        if (isError) {
            errorCount++;
            if (errorButton != null) {
                errorButton.setText(errorCount + " 오류/경고");
                if (!errorButton.getStyleClass().contains("status-btn-error")) {
                    errorButton.getStyleClass().add("status-btn-error");
                }
            }
        } else {
            if (errorCount == 0 && errorButton != null) {
                errorButton.setText(consoleMessages.size() + " 로그");
            }
        }
        if (consoleListView != null) {
            consoleListView.scrollTo(consoleMessages.size() - 1);
        }
    }

    @FXML
    public void toggleConsole() {
        if (consoleListView == null) return;
        boolean isVisible = consoleListView.isVisible();
        consoleListView.setVisible(!isVisible);
        consoleListView.setManaged(!isVisible);
    }

    private void setupContextMenu() {
        contextMenu = new javafx.scene.control.ContextMenu();
    }

    private void updateAndShowContextMenu(double screenX, double screenY) {
        if (contextMenu.isShowing()) contextMenu.hide();
        contextMenu.getItems().clear();

        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("삭제");

        if (!context.selectedNodes.isEmpty()) {
            javafx.scene.control.MenuItem groupItem = new javafx.scene.control.MenuItem("그룹화");
            javafx.scene.control.MenuItem ungroupItem = new javafx.scene.control.MenuItem("그룹화 취소");

            boolean hasUngrouped = false;
            boolean hasGrouped = false;

            for (VisualNode vn : context.selectedNodes) {
                if (vn.group == null) {
                    hasUngrouped = true;
                } else {
                    hasGrouped = true;
                }
            }

            if (hasUngrouped) {
                contextMenu.getItems().add(groupItem);
                groupItem.setOnAction(e -> {
                    context.historyManager.saveState();
                    String targetGroup = null;
                    for (VisualNode vn : context.selectedNodes) {
                        if (vn.group != null) {
                            targetGroup = vn.group;
                            break;
                        }
                    }
                    if (targetGroup == null) {
                        int n = 1;
                        while (true) {
                            targetGroup = "Group " + n;
                            if (!isGroupExists(targetGroup, null)) break;
                            n++;
                        }
                    }
                    for (VisualNode vn : context.selectedNodes) {
                        vn.group = targetGroup;
                    }
                    context.setDirty(true);
                    updatePropertyPane();
                });
            } else if (hasGrouped) {
                contextMenu.getItems().add(ungroupItem);
                ungroupItem.setOnAction(e -> {
                    context.historyManager.saveState();
                    for (VisualNode vn : context.selectedNodes) {
                        vn.group = null;
                    }
                    context.setDirty(true);
                    updatePropertyPane();
                });
            }
            
            contextMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            contextMenu.getItems().add(deleteItem);
            deleteItem.setOnAction(e -> {
                context.historyManager.saveState();
                for (VisualNode vn : new java.util.ArrayList<>(context.selectedNodes)) {
                    removeNode(vn);
                }
                context.selectedNodes.clear();
                context.setSelectedNode(null);
            });
        } else if (context.selectedWire != null) {
            contextMenu.getItems().add(deleteItem);
            deleteItem.setOnAction(e -> {
                context.historyManager.saveState();
                context.getCircuit().disconnect(context.selectedWire.from.node, context.selectedWire.outPin);
                context.visualWires.remove(context.selectedWire);
                context.selectedWire = null;
                context.setDirty(true);
            });
        } else {
            return; // 아무것도 선택되지 않았으면 표시 안 함
        }

        contextMenu.show(simulationCanvas, screenX, screenY);
    }

    private void removeNode(VisualNode vn) {
        context.getCircuit().removeNode(vn.node);
        context.visualNodes.remove(vn);
        context.visualWires.removeIf(w -> {
            boolean related = w.from == vn || w.to == vn;
            if (related && w == context.selectedWire) context.selectedWire = null;
            return related;
        });
        context.setDirty(true);
    }

    private boolean isGroupExists(String name, String excludeGroup) {
        if (name == null || name.isEmpty()) return false;
        for (VisualNode vn : context.visualNodes) {
            if (name.equals(vn.group) && (excludeGroup == null || !excludeGroup.equals(vn.group))) {
                return true;
            }
        }
        return false;
    }

    private void setupPropertyPane() {
        context.onSelectionChanged = this::updatePropertyPane;
        
        labelTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            VisualNode selected = context.getSelectedNode();
            if (selected != null && !newVal.equals(selected.label)) {
                context.historyManager.saveState();
                selected.label = newVal;
                context.setDirty(true);
            }
        });
        
        showLabelCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            VisualNode selected = context.getSelectedNode();
            if (selected != null && selected.showLabel != newVal) {
                context.historyManager.saveState();
                selected.showLabel = newVal;
                context.setDirty(true);
            }
        });
        
        propertyPane.getChildren().add(new javafx.scene.control.Label("Group Name:"));
        groupNameField = new TextField();
        propertyPane.getChildren().add(groupNameField);
        
        groupNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            VisualNode selected = context.getSelectedNode();
            if (selected != null && selected.group != null && !newVal.equals(selected.group)) {
                context.historyManager.saveState();
                String oldGroup = selected.group;
                
                String targetName = newVal;
                int suffix = 1;
                while (isGroupExists(targetName, oldGroup)) {
                    targetName = newVal + "-" + suffix;
                    suffix++;
                }

                for (VisualNode vn : context.visualNodes) {
                    if (oldGroup.equals(vn.group)) {
                        vn.group = targetName;
                    }
                }
                
                if (!targetName.equals(newVal)) {
                    final String finalName = targetName;
                    javafx.application.Platform.runLater(() -> {
                        groupNameField.setText(finalName);
                        groupNameField.positionCaret(finalName.length());
                    });
                }
                
                context.setDirty(true);
            }
        });
        
        updatePropertyPane();
    }

    private void updatePropertyPane() {
        VisualNode selected = context.getSelectedNode();
        if (selected == null) {
            propertyPane.setDisable(true);
            labelTextField.setText("");
            showLabelCheckBox.setSelected(false);
            if (groupNameField != null) {
                groupNameField.setText("");
                groupNameField.setDisable(true);
            }
        } else {
            propertyPane.setDisable(false);
            labelTextField.setText(selected.label);
            showLabelCheckBox.setSelected(selected.showLabel);
            if (groupNameField != null) {
                if (selected.group != null) {
                    groupNameField.setText(selected.group);
                    groupNameField.setDisable(false);
                } else {
                    groupNameField.setText("");
                    groupNameField.setDisable(true);
                }
            }
        }
    }

    private void setupComponentTreeView() {
        TreeItem<String> root = new TreeItem<>("Root");
        
        TreeItem<String> gates = new TreeItem<>("Gate");
        gates.getChildren().addAll(
            new TreeItem<>("AND Gate"),
            new TreeItem<>("OR Gate"),
            new TreeItem<>("NOT Gate"),
            new TreeItem<>("XOR Gate"),
            new TreeItem<>("NOR Gate"),
            new TreeItem<>("NAND Gate"),
            new TreeItem<>("XNOR Gate")
        );
        
        TreeItem<String> inputItem = new TreeItem<>("Input");
        inputItem.getChildren().addAll(
            new TreeItem<>("Switch")
        );
        
        TreeItem<String> outputItem = new TreeItem<>("Output");
        outputItem.getChildren().addAll(
            new TreeItem<>("LED")
        );
        
        TreeItem<String> etc = new TreeItem<>("Etc");
        etc.getChildren().addAll(
            new TreeItem<>("Joint (1:4)")
        );
        
        root.getChildren().addAll(gates, inputItem, outputItem, etc);
        componentTreeView.setRoot(root);
        componentTreeView.setShowRoot(false);
        gates.setExpanded(true);
        inputItem.setExpanded(true);
        outputItem.setExpanded(true);
        etc.setExpanded(true);

        componentTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = componentTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.isLeaf()) {
                    handleComponentCreation(selectedItem.getValue());
                }
            }
        });
    }

    public void initializeProject(java.io.File projectRoot, boolean isNewProject) {
        context.projectRoot = projectRoot;
        if (isNewProject) {
            projectManager.initNewProject();
            loadModsAndUpdateTree();
        } else {
            projectManager.loadProjectConfigOnly();
            loadModsAndUpdateTree();
            projectManager.loadCircuitOnly();
        }
    }

    private void loadModsAndUpdateTree() {
        if (context.projectConfig == null) return;
        
        ModLoader modLoader = new ModLoader(context.projectRoot);
        List<ModComponentInfo> mods = modLoader.loadSpecificMods(context.projectConfig.loadedMods);
        
        TreeItem<String> root = componentTreeView.getRoot();
        
        // 커스텀 부품들만 트리에서 제거하고 다시 로드 🔪💕
        root.getChildren().removeIf(item -> !List.of("Gate", "Input", "Output", "Etc").contains(item.getValue()));
        customComponentMap.clear();

        for (ModComponentInfo mod : mods) {
            customComponentMap.put(mod.name, mod.fqn);
            
            TreeItem<String> sectionItem = null;
            for (TreeItem<String> item : root.getChildren()) {
                if (item.getValue().equals(mod.section)) {
                    sectionItem = item;
                    break;
                }
            }
            
            if (sectionItem == null) {
                sectionItem = new TreeItem<>(mod.section);
                sectionItem.setExpanded(true);
                root.getChildren().add(sectionItem);
            }
            
            sectionItem.getChildren().add(new TreeItem<>(mod.name));
        }
    }

    private void handleComponentCreation(String value) {
        String typeId = switch (value) {
            case "AND Gate" -> "And";
            case "OR Gate" -> "Or";
            case "NOT Gate" -> "Not";
            case "XOR Gate" -> "Xor";
            case "NOR Gate" -> "Nor";
            case "NAND Gate" -> "Nand";
            case "XNOR Gate" -> "Xnor";
            case "Switch" -> "InputPin";
            case "LED" -> "OutputPin";
            case "Joint (1:4)" -> "Joint";
            default -> customComponentMap.get(value);
        };

        if (typeId != null) {
            context.placingNodeTypeId = typeId;
            context.setSelectedNode(null);
            context.selectedNodes.clear();
        }
    }

    @FXML
    public void saveProject() {
        projectManager.saveCurrentProject();
    }

    @FXML
    public void exportJson() {
        projectManager.exportJson(simulationCanvas.getScene().getWindow());
    }

    @FXML
    public void importJson() {
        projectManager.importJson(simulationCanvas.getScene().getWindow());
    }

    @FXML
    public void openModManager() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("mod_manager.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ModManagerController controller = loader.getController();
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("모드 관리자");
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(simulationCanvas.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));
            
            controller.setContext(context, projectManager);
            controller.setStage(stage);
            
            stage.showAndWait();
            
            if (controller.isChanged()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("프로젝트 재로드");
                alert.setHeaderText("모드 설정이 변경되었습니다.");
                alert.setContentText("변경 사항을 적용하려면 프로젝트를 다시 불러와야 합니다. 지금 다시 불러오시겠습니까?");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    if (context.isDirty()) {
                        Alert saveAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        saveAlert.setTitle("저장되지 않은 변경 사항");
                        saveAlert.setHeaderText("현재 프로젝트에 저장되지 않은 내용이 있습니다.");
                        saveAlert.setContentText("재로드하기 전에 저장하시겠습니까?");
                        
                        ButtonType btnSave = new ButtonType("저장 후 재로드");
                        ButtonType btnJustLoad = new ButtonType("저장 없이 재로드");
                        ButtonType btnCancel = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
                        
                        saveAlert.getButtonTypes().setAll(btnSave, btnJustLoad, btnCancel);
                        Optional<ButtonType> saveResult = saveAlert.showAndWait();
                        
                        if (saveResult.isPresent()) {
                            if (saveResult.get() == btnSave) {
                                saveProject();
                            } else if (saveResult.get() == btnCancel) {
                                return;
                            }
                        }
                    }
                    
                    // 프로젝트 재로드 🔪💕
                    initializeProject(context.projectRoot, false);
                } else {
                    // 리로드를 안 하더라도 일단 트리는 새로고침 (새로 추가된 모드가 트리에는 보일 수 있게)
                    loadModsAndUpdateTree();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void clearCircuit() {
        circuit.clear();
        context.visualNodes.clear();
        context.visualWires.clear();
        context.setSelectedNode(null);
        context.selectedWire = null;
        context.setDirty(true);
    }

    private void spawnNode(Node logicNode, String label) {
        context.historyManager.saveState();
        circuit.addNode(logicNode);
        
        double spawnX = ((simulationCanvas.getWidth() / 2) - context.cameraX) / context.zoom;
        double spawnY = ((simulationCanvas.getHeight() / 2) - context.cameraY) / context.zoom;
        
        double nodeWidth = 80;
        double nodeHeight = 50;
        if (logicNode instanceof InputPin || logicNode instanceof OutputPin) {
            nodeWidth = 50;
        } else if (logicNode instanceof Joint) {
            nodeWidth = 30;
            nodeHeight = 30;
        }
        
        VisualNode newNode = new VisualNode(logicNode, spawnX - (nodeWidth / 2), spawnY - (nodeHeight / 2), label);
        context.visualNodes.add(newNode);
        
        context.setSelectedNode(newNode);
        context.selectedWire = null;
        context.setDirty(true);
    }

    public void setPrimaryStage(javafx.stage.Stage stage) {
        this.primaryStage = stage;
        context.onDirtyChanged = this::updateTitle;
        context.onSaveRequested = this::saveProject;
        updateTitle();
        
        stage.setOnCloseRequest(event -> {
            if (context.isDirty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                alert.setTitle("저장되지 않은 변경 사항");
                alert.setHeaderText("프로젝트에 저장되지 않은 변경 사항이 있습니다.");
                alert.setContentText("변경 사항을 저장하시겠습니까?");
                
                javafx.scene.control.ButtonType btnSave = new javafx.scene.control.ButtonType("저장");
                javafx.scene.control.ButtonType btnDontSave = new javafx.scene.control.ButtonType("저장 안 함");
                javafx.scene.control.ButtonType btnCancel = new javafx.scene.control.ButtonType("취소", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                
                alert.getButtonTypes().setAll(btnSave, btnDontSave, btnCancel);
                
                java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == btnSave) {
                        saveProject();
                        shutdown();
                    } else if (result.get() == btnCancel) {
                        event.consume();
                    } else {
                        shutdown();
                    }
                }
            } else {
                shutdown();
            }
        });
    }

    private void updateTitle() {
        if (primaryStage == null || context.projectRoot == null) return;
        String title = "Logic Gate Simulator - " + context.projectRoot.getName();
        if (context.isDirty()) title += " *";
        primaryStage.setTitle(title);
    }

    public void shutdown() {
        if (timer != null) timer.stop();
        if (circuit != null) circuit.stopSimulation();
    }
}