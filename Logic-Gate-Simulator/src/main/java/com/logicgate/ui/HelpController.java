package com.logicgate.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.concurrent.Worker;

public class HelpController {

    @FXML private ListView<String> helpCategoryList;
    @FXML private ScrollPane helpScrollPane;
    @FXML private StackPane helpContentArea;
    
    @FXML private VBox introPane;
    @FXML private VBox componentPane;
    @FXML private VBox wiringPane;
    @FXML private VBox simControlPane;
    @FXML private VBox gateHelpPane;
    @FXML private VBox shortcutPane;
    @FXML private TextField gateHelpSearchField;
    @FXML private ListView<GateHelpDoc> gateHelpListView;
    @FXML private WebView gateHelpWebView;

    private Stage stage;
    private File projectRoot;
    private Parser markdownParser;
    private HtmlRenderer markdownRenderer;
    private final ObservableList<GateHelpDoc> allGateHelpDocs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initializeMarkdownRenderer();
        initializeGateHelpDocs();

        helpCategoryList.getItems().addAll(
            "시뮬레이터 소개",
            "부품 조작 가이드",
            "전선 연결 및 관리",
            "시뮬레이션 제어",
            "게이트 도움말",
            "단축키 일람"
        );

        helpCategoryList.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            updateContent(newVal.intValue());
        });

        gateHelpSearchField.textProperty().addListener((obs, oldText, newText) -> filterGateHelpDocs(newText));
        gateHelpListView.getSelectionModel().selectedItemProperty().addListener((obs, oldDoc, newDoc) -> {
            if (newDoc != null) {
                renderGateHelpDoc(newDoc);
            }
        });
        gateHelpWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                fitGateHelpWebViewToContent();
            }
        });
        gateHelpWebView.addEventFilter(ScrollEvent.SCROLL, this::scrollHelpPaneFromWebView);
        hideGateHelpResults();
        renderDefaultGateHelpDoc();
        filterGateHelpDocs("");
        helpCategoryList.getSelectionModel().select(0);
    }

    private void updateContent(int index) {
        introPane.setVisible(index == 0);
        componentPane.setVisible(index == 1);
        wiringPane.setVisible(index == 2);
        simControlPane.setVisible(index == 3);
        gateHelpPane.setVisible(index == 4);
        shortcutPane.setVisible(index == 5);
    }

    private void initializeMarkdownRenderer() {
        List<Extension> extensions = List.of(TablesExtension.create());
        markdownParser = Parser.builder()
            .extensions(extensions)
            .build();
        markdownRenderer = HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(true)
            .build();
    }

    private void initializeGateHelpDocs() {
        allGateHelpDocs.addAll(
            builtInDoc("AND Gate", "and.md"),
            builtInDoc("OR Gate", "or.md"),
            builtInDoc("NOT Gate", "not.md"),
            builtInDoc("XOR Gate", "xor.md"),
            builtInDoc("NAND Gate", "nand.md"),
            builtInDoc("NOR Gate", "nor.md"),
            builtInDoc("XNOR Gate", "xnor.md"),
            builtInDoc("InputPin / Switch", "input-pin.md"),
            builtInDoc("OutputPin / LED", "output-pin.md"),
            builtInDoc("Joint (2~8)", "joint.md")
        );
    }

    private void filterGateHelpDocs(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            hideGateHelpResults();
            gateHelpListView.setItems(FXCollections.observableArrayList());
            return;
        }

        List<GateHelpDoc> filtered = allGateHelpDocs.stream()
            .filter(doc -> doc.title().toLowerCase().contains(normalized)
                || doc.source().toLowerCase().contains(normalized)
                || doc.markdown().toLowerCase().contains(normalized))
            .toList();
        showGateHelpResults();
        gateHelpListView.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            gateHelpListView.getSelectionModel().selectFirst();
        } else {
            gateHelpWebView.getEngine().loadContent(buildHtml("<h2>검색 결과 없음</h2><p>다른 이름이나 핀 이름으로 검색해보세요.</p>"));
        }
    }

    private void hideGateHelpResults() {
        gateHelpListView.setVisible(false);
        gateHelpListView.setManaged(false);
    }

    private void showGateHelpResults() {
        gateHelpListView.setVisible(true);
        gateHelpListView.setManaged(true);
    }

    private void renderDefaultGateHelpDoc() {
        if (!allGateHelpDocs.isEmpty()) {
            renderGateHelpDoc(allGateHelpDocs.get(0));
        }
    }

    private void renderGateHelpDoc(GateHelpDoc doc) {
        Node document = markdownParser.parse(doc.markdown());
        gateHelpWebView.getEngine().loadContent(buildHtml(markdownRenderer.render(document)));
    }

    private void fitGateHelpWebViewToContent() {
        Object height = gateHelpWebView.getEngine().executeScript(
            "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)"
        );
        if (height instanceof Number number) {
            double contentHeight = Math.max(420, number.doubleValue() + 8);
            gateHelpWebView.setMinHeight(contentHeight);
            gateHelpWebView.setPrefHeight(contentHeight);
            gateHelpWebView.setMaxHeight(contentHeight);
        }
    }

    private void scrollHelpPaneFromWebView(ScrollEvent event) {
        if (helpScrollPane == null) {
            return;
        }

        double contentHeight = helpScrollPane.getContent().getBoundsInLocal().getHeight();
        double viewportHeight = helpScrollPane.getViewportBounds().getHeight();
        double scrollableHeight = Math.max(1, contentHeight - viewportHeight);
        double nextValue = helpScrollPane.getVvalue() - event.getDeltaY() / scrollableHeight;
        helpScrollPane.setVvalue(Math.max(0, Math.min(1, nextValue)));
        event.consume();
    }

    private GateHelpDoc builtInDoc(String title, String resourcePath) {
        return new GateHelpDoc(title, readBuiltInGateHelpMarkdown(resourcePath), "Built-in");
    }

    private String readBuiltInGateHelpMarkdown(String resourcePath) {
        String fullPath = "/com/logicgate/help/gates/" + resourcePath;
        try (InputStream inputStream = getClass().getResourceAsStream(fullPath)) {
            if (inputStream == null) {
                return "# 문서를 찾을 수 없음\n\n리소스 경로: `" + fullPath + "`";
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "# 문서를 읽을 수 없음\n\n" + e.getMessage();
        }
    }

    private void reloadModHelpDocs() {
        allGateHelpDocs.removeIf(GateHelpDoc::modDoc);
        allGateHelpDocs.addAll(loadModHelpDocs());
        filterGateHelpDocs(gateHelpSearchField == null ? "" : gateHelpSearchField.getText());
    }

    private List<GateHelpDoc> loadModHelpDocs() {
        List<GateHelpDoc> docs = new ArrayList<>();
        if (projectRoot == null) {
            return docs;
        }

        File modsDir = new File(projectRoot, "mods");
        File[] jarFiles = modsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null) {
            return docs;
        }

        for (File jarFile : jarFiles) {
            docs.addAll(loadModHelpDocs(jarFile));
        }
        docs.sort(Comparator.comparing(GateHelpDoc::title));
        return docs;
    }

    private List<GateHelpDoc> loadModHelpDocs(File jarFile) {
        List<GateHelpDoc> docs = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !isModHelpEntry(entry)) {
                    continue;
                }
                try (InputStream inputStream = jar.getInputStream(entry)) {
                    String markdown = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    docs.add(new GateHelpDoc(readMarkdownTitle(markdown, entry.getName()), markdown, jarFile.getName()));
                }
            }
        } catch (IOException e) {
            System.err.println("[HelpController] 모드 도움말을 읽을 수 없습니다: " + jarFile.getName());
        }
        return docs;
    }

    private boolean isModHelpEntry(JarEntry entry) {
        String name = entry.getName();
        return name.startsWith("META-INF/logicgate/help/") && name.toLowerCase().endsWith(".md");
    }

    private String readMarkdownTitle(String markdown, String fallbackPath) {
        for (String line : markdown.split("\\R")) {
            if (line.startsWith("# ")) {
                return "Mod: " + line.substring(2).trim();
            }
        }
        String fileName = fallbackPath.substring(fallbackPath.lastIndexOf('/') + 1).replace(".md", "");
        return "Mod: " + fileName;
    }

    private String buildHtml(String body) {
        return """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body {
                  margin: 0;
                  padding: 18px 20px 28px;
                  color: #202124;
                  background: #ffffff;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans KR", sans-serif;
                  font-size: 14px;
                  line-height: 1.62;
                }
                h1 { font-size: 24px; margin: 0 0 12px; }
                h2 { font-size: 18px; margin: 24px 0 8px; border-bottom: 1px solid #e5e7eb; padding-bottom: 6px; }
                h3 { font-size: 15px; margin: 18px 0 6px; }
                p { margin: 8px 0; }
                ul, ol { padding-left: 24px; }
                li { margin: 4px 0; }
                code {
                  background: #f3f4f6;
                  border: 1px solid #e5e7eb;
                  border-radius: 4px;
                  padding: 1px 5px;
                  font-family: "SFMono-Regular", Consolas, monospace;
                }
                pre {
                  background: #f8fafc;
                  border: 1px solid #e5e7eb;
                  border-radius: 6px;
                  padding: 12px;
                  overflow-x: auto;
                }
                pre code { border: 0; background: transparent; padding: 0; }
                table { border-collapse: collapse; width: 100%; margin: 12px 0; }
                th, td { border: 1px solid #e5e7eb; padding: 7px 9px; text-align: left; }
                th { background: #f8fafc; }
              </style>
            </head>
            <body>
            """ + body + """
            </body>
            </html>
            """;
    }

    private record GateHelpDoc(String title, String markdown, String source) {
        boolean modDoc() {
            return !"Built-in".equals(source);
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setProjectRoot(File projectRoot) {
        this.projectRoot = projectRoot;
        reloadModHelpDocs();
    }

    @FXML
    public void close() {
        if (stage != null) stage.close();
    }
}
