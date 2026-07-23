package com.logicgate.ui.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.logicgate.editor.mod.ModComponentInfo;
import com.logicgate.editor.state.EditorContext;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class ComponentTreeController {
    private static final List<String> BUILT_IN_SECTIONS = List.of("Gate", "Input", "Output", "Etc");

    private final EditorContext context;
    private final TreeView<String> componentTreeView;
    private final Map<String, String> customComponentMap = new HashMap<>();

    public ComponentTreeController(EditorContext context, TreeView<String> componentTreeView) {
        this.context = context;
        this.componentTreeView = componentTreeView;
    }

    @SuppressWarnings("unchecked")
    public void setup() {
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
        inputItem.getChildren().addAll(new TreeItem<>("Switch"));

        TreeItem<String> outputItem = new TreeItem<>("Output");
        outputItem.getChildren().addAll(new TreeItem<>("LED"));

        TreeItem<String> etc = new TreeItem<>("Etc");
        etc.getChildren().addAll(new TreeItem<>("Joint (1:4)"));

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

    public void updateMods(List<ModComponentInfo> mods) {
        TreeItem<String> root = componentTreeView.getRoot();
        root.getChildren().removeIf(item -> !BUILT_IN_SECTIONS.contains(item.getValue()));
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
}
