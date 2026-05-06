package com.logicgate.ui;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.logicgate.Circuit;
import com.logicgate.editor.interaction.KeyboardInteractionHandler;
import com.logicgate.editor.interaction.MouseInteractionHandler;
import com.logicgate.editor.interaction.WiringManager;
import com.logicgate.editor.io.ProjectManager;
import com.logicgate.editor.mod.ModComponentInfo;
import com.logicgate.editor.mod.ModLoader;
import com.logicgate.editor.mod.NotificationApi;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.CanvasRenderer;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.gates.InputPin;
import com.logicgate.gates.Joint;
import com.logicgate.gates.Node;
import com.logicgate.gates.OutputPin;
import com.logicgate.ui.main.ComponentSearchController;
import com.logicgate.ui.main.ComponentTreeController;
import com.logicgate.ui.main.ConsoleLogController;
import com.logicgate.ui.main.EditorContextMenuController;
import com.logicgate.ui.main.NotificationController;
import com.logicgate.ui.main.PropertyPaneController;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML private ListView<String> consoleListView;
    @FXML private Button errorButton;
    @FXML private javafx.scene.control.Label versionLabel;
    @FXML private Pane canvasPane;
    @FXML private Canvas simulationCanvas;
    @FXML private TreeView<String> componentTreeView;
    @FXML private VBox propertyPane;
    @FXML private TextField searchTextField;
    @FXML private ListView<ComponentSearchController.SearchResult> searchResultsListView;
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;
    @FXML private VBox notificationPane;
    @FXML private MenuBar mainMenuBar;
    @FXML private Button btnPlayPause;
    @FXML private Button btnReset;

    private Circuit circuit;
    private EditorContext context;
    private ProjectManager projectManager;
    private WiringManager wiringManager;
    private CanvasRenderer renderer;
    private MouseInteractionHandler mouseHandler;
    private KeyboardInteractionHandler keyboardHandler;
    private AnimationTimer timer;
    private javafx.stage.Stage primaryStage;
    private javafx.animation.Timeline autosaveTimer;
    private org.kordamp.ikonli.javafx.FontIcon playPauseIcon;
    private boolean shutdownInProgress = false;

    private ComponentTreeController componentTreeController;
    private ComponentSearchController componentSearchController;
    private PropertyPaneController propertyPaneController;
    private EditorContextMenuController contextMenuController;
    private ConsoleLogController consoleLogController;
    private NotificationController notificationController;
    private Runnable restartCallback;

    private ResourceBundle bundle() {
        return ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
    }

    @FXML
    public void clearFocusFromCanvas(javafx.scene.input.MouseEvent event) {
        javafx.scene.Node target = (javafx.scene.Node) event.getTarget();

        javafx.scene.Node current = target;
        while (current != null && current != event.getSource()) {
            if (current instanceof Control) {
                return;
            }
            current = current.getParent();
        }

        Object source = event.getSource();
        if (source instanceof javafx.scene.Node node) {
            node.setFocusTraversable(true);
            node.requestFocus();
        }
    }

    @FXML
    public void initialize() {
        circuit = new Circuit();
        circuit.setTickFrequencyHz(60.0);

        context = new EditorContext(circuit);
        wiringManager = new WiringManager(context);
        projectManager = new ProjectManager(context);
        mouseHandler = new MouseInteractionHandler(context, wiringManager);
        keyboardHandler = new KeyboardInteractionHandler(context);
        renderer = new CanvasRenderer(simulationCanvas, context, wiringManager);

        setupChildControllers();
        setupCanvas();
        setupEditorCallbacks();
        startRenderLoop();

        circuit.startSimulation();
        setupSimulationButtons();
        setupVersionLabel();
        consoleLogController.setup();
        setupNotificationApi();
    }

    private void setupVersionLabel() {
        if (versionLabel == null) return;

        try {
            ResourceBundle appBundle = ResourceBundle.getBundle("com.logicgate.app");
            versionLabel.setText("v" + appBundle.getString("app.version"));
        } catch (Exception e) {
            String version = MainController.class.getPackage().getImplementationVersion();
            versionLabel.setText(version != null ? "v" + version : "");
        }
    }

    private void setupChildControllers() {
        componentTreeController = new ComponentTreeController(context, componentTreeView);
        componentSearchController = new ComponentSearchController(context, simulationCanvas, searchTextField, searchResultsListView);
        propertyPaneController = new PropertyPaneController(context, propertyPane);
        contextMenuController = new EditorContextMenuController(context, simulationCanvas, propertyPaneController);
        consoleLogController = new ConsoleLogController(consoleListView, errorButton);
        notificationController = new NotificationController(notificationPane);

        componentTreeController.setup();
        propertyPaneController.setup();
        contextMenuController.setup();
        componentSearchController.setup();
    }

    public void showNotification(NotificationController.Type type, String title, String body) {
        if (notificationController != null) {
            notificationController.show(type, title, body);
        }
    }

    private void setupNotificationApi() {
        NotificationApi.setHandler((type, title, body) -> showNotification(
            switch (type) {
                case INFO -> NotificationController.Type.INFO;
                case WARNING -> NotificationController.Type.WARNING;
                case ERROR -> NotificationController.Type.ERROR;
            },
            title,
            body
        ));
    }

    private void setupCanvas() {
        simulationCanvas.widthProperty().bind(canvasPane.widthProperty());
        simulationCanvas.heightProperty().bind(canvasPane.heightProperty());
        simulationCanvas.setFocusTraversable(true);

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
            simulationCanvas.requestFocus();
            contextMenuController.hide();
            mouseHandler.handleMousePressed(e);
        });
        simulationCanvas.setOnMouseDragged(mouseHandler::handleMouseDragged);
        simulationCanvas.setOnMouseReleased(mouseHandler::handleMouseReleased);
        simulationCanvas.setOnScroll(mouseHandler::handleMouseScrolled);
        simulationCanvas.setOnMouseMoved(mouseHandler::handleMouseMoved);

        simulationCanvas.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            keyboardHandler.handleKeyPressed(e);
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                componentSearchController.clear();
            }
            if (e.getCode().isArrowKey()) e.consume();
        });
        simulationCanvas.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, e -> {
            keyboardHandler.handleKeyReleased(e);
            if (e.getCode().isArrowKey()) e.consume();
        });

        javafx.event.EventHandler<javafx.scene.input.KeyEvent> consumeArrows = e -> {
            if (e.getCode().isArrowKey()) e.consume();
        };
        leftSidebar.setOnKeyPressed(consumeArrows);
        rightSidebar.setOnKeyPressed(consumeArrows);
        if (mainMenuBar != null) mainMenuBar.setOnKeyPressed(consumeArrows);
    }

    private void setupEditorCallbacks() {
        context.onCopyRequested = projectManager::copyToClipboard;
        context.onPasteRequested = projectManager::pasteFromClipboard;
        context.onNotificationRequested = (type, title, body) -> showNotification(
            switch (type) {
                case INFO -> NotificationController.Type.INFO;
                case WARNING -> NotificationController.Type.WARNING;
                case ERROR -> NotificationController.Type.ERROR;
            },
            title,
            body
        );
    }

    private void startRenderLoop() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                keyboardHandler.updateCamera();
                renderer.draw();
            }
        };
        timer.start();
    }

    private void setupSimulationButtons() {
        playPauseIcon = new org.kordamp.ikonli.javafx.FontIcon("mdi2p-pause");
        playPauseIcon.setIconSize(16);
        playPauseIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        btnPlayPause.setGraphic(playPauseIcon);
        updateSimButtonStates(true);
    }

    @FXML
    public void toggleConsole() {
        consoleLogController.toggleConsole();
    }

    public void initializeProject(java.io.File projectRoot, boolean isNewProject) {
        context.projectRoot = projectRoot;

        circuit.clear();
        context.visualNodes.clear();
        context.visualWires.clear();
        context.setSelectedNode(null);
        context.selectedWire = null;
        context.selectedWires.clear();
        context.historyManager.clear();

        if (isNewProject) {
            projectManager.initNewProject();
            loadModsAndUpdateTree();
        } else {
            projectManager.loadProjectConfigOnly();
            loadModsAndUpdateTree();
            projectManager.loadCircuitOnly();
            showProjectLoadWarnings();
        }
        applyProjectOptions();
        updateTitle();
    }

    private void applyProjectOptions() {
        if (context.projectConfig != null) {
            circuit.setTickFrequencyHz(context.projectConfig.tickFrequencyHz);

            if (autosaveTimer != null) autosaveTimer.stop();
            if (context.projectConfig.autosaveIntervalMin > 0) {
                autosaveTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                    javafx.util.Duration.minutes(context.projectConfig.autosaveIntervalMin),
                    e -> { if (context.isDirty()) saveProject(); }
                ));
                autosaveTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
                autosaveTimer.play();
            }

            if (renderer != null) renderer.draw();
        }
    }

    @FXML
    public void openOptions() {
        try {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("options.fxml"), bundle);
            javafx.scene.Parent root = loader.load();

            OptionsController controller = loader.getController();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(bundle.getString("options.title"));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(simulationCanvas.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));

            boolean hadUnsavedChanges = context.isDirty();
            controller.setContext(
                context,
                stage,
                languageChanged -> !languageChanged || !hadUnsavedChanges || confirmClose(),
                languageChanged -> {
                    applyProjectOptions();
                    projectManager.saveProjectConfig();
                    context.setDirty(!languageChanged && hadUnsavedChanges);
                    if (languageChanged && restartCallback != null) {
                        Platform.runLater(restartCallback);
                    }
                }
            );

            ResponsiveTextSupport.fitStageToContent(stage, root, 0.9, 0.9);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openHelp() {
        try {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("help.fxml"), bundle);
            javafx.scene.Parent root = loader.load();

            HelpController controller = loader.getController();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(bundle.getString("menu.help"));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(simulationCanvas.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));

            controller.setStage(stage);
            controller.setProjectRoot(context.projectRoot);
            ResponsiveTextSupport.fitStageToContent(stage, root, 0.9, 0.9);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadModsAndUpdateTree() {
        if (context.projectConfig == null) return;

        ModLoader modLoader = new ModLoader(context.projectRoot);
        List<ModComponentInfo> mods = modLoader.loadSpecificMods(context.projectConfig.loadedMods);
        componentTreeController.updateMods(mods);
    }

    public ProjectManager getProjectManager() {
        return projectManager;
    }

    public java.io.File getProjectRoot() {
        return context != null ? context.projectRoot : null;
    }

    public void setRestartCallback(Runnable restartCallback) {
        this.restartCallback = restartCallback;
    }

    public void prepareForRestart() {
        if (autosaveTimer != null) autosaveTimer.stop();
        if (timer != null) timer.stop();
        if (circuit != null) circuit.stopSimulation();
    }

    @FXML
    public void saveProject() {
        boolean saved = projectManager.saveCurrentProject();
        showNotification(
            saved ? NotificationController.Type.INFO : NotificationController.Type.ERROR,
            bundle().getString(saved ? "notification.save.success" : "notification.save.failure"),
            null
        );
    }

    @FXML
    public void newProject() {
        if (context.isDirty()) {
            ResourceBundle bundle = bundle();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(bundle.getString("alert.unsaved.title"));
            alert.setHeaderText(bundle.getString("alert.unsaved.header"));
            alert.setContentText(bundle.getString("alert.unsaved.content"));

            ButtonType btnSave = new ButtonType(bundle.getString("alert.unsaved.btn.save"));
            ButtonType btnDontSave = new ButtonType(bundle.getString("alert.unsaved.btn.dont_save"));
            ButtonType btnCancel = new ButtonType(bundle.getString("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(btnSave, btnDontSave, btnCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnSave) {
                    saveProject();
                } else if (result.get() == btnCancel) {
                    return;
                }
            }
        }

        LauncherController.ProjectResult res = projectManager.showLauncher(primaryStage, true);
        if (res != null) {
            initializeProject(res.root, res.isNew);
        }
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
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("mod_manager.fxml"), bundle);
            javafx.scene.Parent root = loader.load();

            ModManagerController controller = loader.getController();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(bundle.getString("mod_manager.title"));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(simulationCanvas.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));

            controller.setContext(context, projectManager);
            controller.setStage(stage);

            stage.showAndWait();

            if (controller.isChanged()) {
                handleModManagerChanges();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleModManagerChanges() {
        ResourceBundle bundle = bundle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(bundle.getString("alert.mod_reload.title"));
        alert.setHeaderText(bundle.getString("alert.mod_reload.header"));
        alert.setContentText(bundle.getString("alert.mod_reload.content"));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            reloadProjectAfterModChanges();
        } else {
            loadModsAndUpdateTree();
        }
    }

    private void reloadProjectAfterModChanges() {
        if (context.isDirty()) {
            ResourceBundle bundle = bundle();
            Alert saveAlert = new Alert(Alert.AlertType.CONFIRMATION);
            saveAlert.setTitle(bundle.getString("alert.unsaved.title"));
            saveAlert.setHeaderText(bundle.getString("alert.mod_save.header"));
            saveAlert.setContentText(bundle.getString("alert.mod_save.content"));

            ButtonType btnSave = new ButtonType(bundle.getString("alert.mod_save.btn.save"));
            ButtonType btnJustLoad = new ButtonType(bundle.getString("alert.mod_save.btn.reload"));
            ButtonType btnCancel = new ButtonType(bundle.getString("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

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

        initializeProject(context.projectRoot, false);
    }

    @FXML
    public void clearCircuit() {
        context.historyManager.saveState();
        circuit.clear();
        context.visualNodes.clear();
        context.visualWires.clear();
        context.selectedNodes.clear();
        context.setSelectedNode(null);
        context.selectedWire = null;
        context.selectedWires.clear();
        context.setDirty(true);
    }

    @FXML
    public void toggleSimulation() {
        if (circuit.isRunning()) {
            circuit.stopSimulation();
            updateSimButtonStates(false);
        } else {
            circuit.startSimulation();
            updateSimButtonStates(true);
        }
    }

    @FXML
    public void resetSimulation() {
        circuit.stopSimulation();

        for (com.logicgate.editor.model.VisualWire w : context.visualWires) {
            circuit.disconnectSpecific(w.from.node, w.outPin, w.to.node, w.inPin);
        }

        circuit.resetState();

        for (com.logicgate.editor.model.VisualWire w : context.visualWires) {
            circuit.connect(w.from.node, w.outPin, w.to.node, w.inPin);
            circuit.tick();
        }

        updateSimButtonStates(false);
        if (renderer != null) renderer.draw();
    }

    private void updateSimButtonStates(boolean isRunning) {
        if (isRunning) {
            btnPlayPause.setText("");
            btnPlayPause.getStyleClass().removeAll("sim-btn-start");
            btnPlayPause.getStyleClass().add("sim-btn-pause");
            if (playPauseIcon != null) playPauseIcon.setIconLiteral("mdi2p-pause");
        } else {
            btnPlayPause.setText("");
            btnPlayPause.getStyleClass().removeAll("sim-btn-pause");
            btnPlayPause.getStyleClass().add("sim-btn-start");
            if (playPauseIcon != null) playPauseIcon.setIconLiteral("mdi2p-play");
        }
    }

    @SuppressWarnings("unused")
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
        context.selectedWires.clear();
        context.setDirty(true);
    }

    public void setPrimaryStage(javafx.stage.Stage stage) {
        this.primaryStage = stage;
        context.onDirtyChanged = this::updateTitle;
        context.onSaveRequested = this::saveProject;
        updateTitle();

        stage.setOnCloseRequest(this::handleCloseRequest);
    }

    private void updateTitle() {
        if (primaryStage == null || context.projectRoot == null) return;
        String title = "Logic Gate Simulator - " + context.projectRoot.getName();
        if (context.isDirty()) title += " *";
        primaryStage.setTitle(title);
    }

    public void shutdown() {
        if (shutdownInProgress) return;

        if (!confirmClose()) return;
        shutdownInProgress = true;

        if (timer != null) timer.stop();
        if (circuit != null) circuit.stopSimulation();
        if (primaryStage != null) {
            primaryStage.close();
        } else {
            Platform.exit();
        }
    }

    private void handleCloseRequest(javafx.stage.WindowEvent event) {
        if (shutdownInProgress) return;

        if (!confirmClose()) {
            event.consume();
            return;
        }

        shutdownInProgress = true;
        if (timer != null) timer.stop();
        if (circuit != null) circuit.stopSimulation();
    }

    private boolean confirmClose() {
        if (!context.isDirty()) return true;

        ResourceBundle bundle = bundle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(bundle.getString("alert.unsaved.title"));
        alert.setHeaderText(bundle.getString("alert.confirm_close.header"));
        alert.setContentText(bundle.getString("alert.confirm_close.content"));

        ButtonType btnSave = new ButtonType(bundle.getString("alert.unsaved.btn.save"));
        ButtonType btnDontSave = new ButtonType(bundle.getString("alert.unsaved.btn.dont_save"));
        ButtonType btnCancel = new ButtonType(bundle.getString("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnSave, btnDontSave, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == btnCancel) {
            return false;
        }
        if (result.get() == btnSave) {
            saveProject();
        }
        return true;
    }

    private void showProjectLoadWarnings() {
        List<String> warnings = projectManager.consumeLoadWarnings();
        if (warnings.isEmpty()) return;

        ResourceBundle bundle = bundle();
        showNotification(
            NotificationController.Type.ERROR,
            bundle.getString("notification.load_warn.title"),
            bundle.getString("notification.load_warn.body") + "\n" + String.join("\n", warnings)
        );
    }
}
