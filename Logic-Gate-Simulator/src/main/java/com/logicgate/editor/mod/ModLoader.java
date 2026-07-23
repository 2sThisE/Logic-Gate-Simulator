package com.logicgate.editor.mod;

import com.logicgate.gates.Node;
import com.logicgate.editor.rendering.symbol.GateSymbol;
import com.logicgate.editor.rendering.symbol.SymbolRegistry;
import com.logicgate.editor.utils.NodeFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.lang.reflect.Modifier;

/**
 * 외부 JAR 파일에서 커스텀 컴포넌트를 동적으로 불러오는 모드 로더입니다.
 */
public class ModLoader {

    private final File modsDir;
    private final List<ModLoadDiagnostic> diagnostics = new ArrayList<>();

    public ModLoader(File projectRoot) {
        this.modsDir = new File(projectRoot, "mods");
        if (!modsDir.exists()) {
            modsDir.mkdirs();
        }
    }

    /**
     * 지정된 리스트의 모드 파일들을 불러옵니다.
     * @param modFileNames 불러올 JAR 파일 이름 리스트 (예: ["custom_gates.jar"])
     * @return 성공적으로 로드된 커스텀 게이트들의 메타데이터 리스트
     */
    public List<ModComponentInfo> loadSpecificMods(List<String> modFileNames) {
        diagnostics.clear();
        List<ModComponentInfo> loadedMods = new ArrayList<>();
        if (modFileNames == null) return loadedMods;

        for (String fileName : modFileNames) {
            File jarFile = resolveModFile(fileName);
            if (jarFile == null) continue;
            if (jarFile.exists()) {
                loadedMods.addAll(loadJar(jarFile));
            } else {
                addDiagnostic(
                    ModLoadDiagnostic.Severity.ERROR,
                    ModLoadDiagnostic.Stage.FILE_ACCESS,
                    fileName,
                    null,
                    "모드 파일을 찾을 수 없습니다."
                );
            }
        }
        return loadedMods;
    }

    /**
     * 단일 모드 파일을 불러옵니다. (나중에 설정에서 추가할 때 사용)
     */
    public List<ModComponentInfo> loadSingleMod(String fileName) {
        diagnostics.clear();
        File jarFile = resolveModFile(fileName);
        if (jarFile == null) return new ArrayList<>();
        if (jarFile.exists()) {
            return loadJar(jarFile);
        }
        addDiagnostic(
            ModLoadDiagnostic.Severity.ERROR,
            ModLoadDiagnostic.Stage.FILE_ACCESS,
            fileName,
            null,
            "모드 파일을 찾을 수 없습니다."
        );
        return new ArrayList<>();
    }

    public List<ModLoadDiagnostic> consumeDiagnostics() {
        List<ModLoadDiagnostic> result = new ArrayList<>(diagnostics);
        diagnostics.clear();
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ModComponentInfo> loadJar(File jarFile) {
        List<ModComponentInfo> infos = new ArrayList<>();
        // try-with-resources를 사용하여 로딩 후 클래스 로더를 닫음
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{ jarFile.toURI().toURL() }, this.getClass().getClassLoader())) {
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                    String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    if (className.equals("module-info") || className.endsWith(".package-info")) continue;

                    Class<?> clazz;
                    try {
                        clazz = classLoader.loadClass(className);
                    } catch (Exception | LinkageError e) {
                        addDiagnostic(
                            ModLoadDiagnostic.Severity.ERROR,
                            ModLoadDiagnostic.Stage.CLASS_LOADING,
                            jarFile.getName(),
                            className,
                            ModLoadDiagnostic.describe(e)
                        );
                        continue;
                    }

                    if (Node.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                        try {
                            ComponentMeta meta = clazz.getAnnotation(ComponentMeta.class);
                            if (meta == null) {
                                addDiagnostic(
                                    ModLoadDiagnostic.Severity.WARNING,
                                    ModLoadDiagnostic.Stage.NODE_REGISTRATION,
                                    jarFile.getName(),
                                    className,
                                    "@ComponentMeta가 없어 노드 등록을 건너뜁니다."
                                );
                            } else {
                                var constructor = clazz.getDeclaredConstructor();
                                if (!Modifier.isPublic(constructor.getModifiers()) ||
                                    !Modifier.isPublic(clazz.getModifiers())) {
                                    throw new NoSuchMethodException("public 기본 생성자가 필요합니다.");
                                }
                                reportNodeTypeCollision(
                                    clazz.getSimpleName(),
                                    (Class<? extends Node>) clazz,
                                    jarFile.getName(),
                                    className
                                );
                                NodeFactory.register(clazz.getSimpleName(), (Class<? extends Node>) clazz);
                                if (!meta.typeId().isEmpty()) {
                                    reportNodeTypeCollision(
                                        meta.typeId(),
                                        (Class<? extends Node>) clazz,
                                        jarFile.getName(),
                                        className
                                    );
                                    NodeFactory.register(meta.typeId(), (Class<? extends Node>) clazz);
                                }
                                infos.add(new ModComponentInfo(meta.section(), meta.name(), clazz.getName()));
                            }
                        } catch (Exception | LinkageError e) {
                            addDiagnostic(
                                ModLoadDiagnostic.Severity.ERROR,
                                ModLoadDiagnostic.Stage.NODE_REGISTRATION,
                                jarFile.getName(),
                                className,
                                ModLoadDiagnostic.describe(e)
                            );
                        }
                    }

                    if (GateSymbol.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                        try {
                            GateSymbol symbol = (GateSymbol) clazz.getDeclaredConstructor().newInstance();
                            ComponentMeta meta = clazz.getAnnotation(ComponentMeta.class);
                            String symbolTypeId;
                            if (meta != null && !meta.typeId().isEmpty()) {
                                symbolTypeId = meta.typeId();
                            } else {
                                symbolTypeId = clazz.getSimpleName().replace("Symbol", "");
                            }
                            GateSymbol registeredSymbol = SymbolRegistry.getSymbol(symbolTypeId);
                            if (registeredSymbol != null &&
                                !registeredSymbol.getClass().getName().equals(clazz.getName())) {
                                addDiagnostic(
                                    ModLoadDiagnostic.Severity.WARNING,
                                    ModLoadDiagnostic.Stage.SYMBOL_REGISTRATION,
                                    jarFile.getName(),
                                    className,
                                    "심볼 typeId '" + symbolTypeId + "'의 기존 등록을 덮어씁니다."
                                );
                            }
                            SymbolRegistry.registerExternalSymbol(symbolTypeId, symbol);
                        } catch (Exception | LinkageError e) {
                            addDiagnostic(
                                ModLoadDiagnostic.Severity.ERROR,
                                ModLoadDiagnostic.Stage.SYMBOL_REGISTRATION,
                                jarFile.getName(),
                                className,
                                ModLoadDiagnostic.describe(e)
                            );
                        }
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            addDiagnostic(
                ModLoadDiagnostic.Severity.ERROR,
                ModLoadDiagnostic.Stage.FILE_ACCESS,
                jarFile.getName(),
                null,
                ModLoadDiagnostic.describe(e)
            );
        }
        return infos;
    }

    private void reportNodeTypeCollision(
        String typeId,
        Class<? extends Node> nodeClass,
        String jarName,
        String className
    ) {
        Class<? extends Node> registered = NodeFactory.getRegisteredClass(typeId);
        if (registered != null && registered != nodeClass &&
            !registered.getName().equals(nodeClass.getName())) {
            addDiagnostic(
                ModLoadDiagnostic.Severity.WARNING,
                ModLoadDiagnostic.Stage.NODE_REGISTRATION,
                jarName,
                className,
                "노드 typeId '" + typeId + "'의 기존 등록(" + registered.getName() + ")을 덮어씁니다."
            );
        }
    }

    private File resolveModFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            addDiagnostic(
                ModLoadDiagnostic.Severity.ERROR,
                ModLoadDiagnostic.Stage.FILE_ACCESS,
                "<blank>",
                null,
                "모드 파일 이름이 비어 있습니다."
            );
            return null;
        }

        java.nio.file.Path root = modsDir.toPath().toAbsolutePath().normalize();
        java.nio.file.Path candidate = root.resolve(fileName).normalize();
        if (!candidate.startsWith(root)) {
            addDiagnostic(
                ModLoadDiagnostic.Severity.ERROR,
                ModLoadDiagnostic.Stage.FILE_ACCESS,
                fileName,
                null,
                "프로젝트 mods 디렉터리 밖의 파일은 불러올 수 없습니다."
            );
            return null;
        }
        return candidate.toFile();
    }

    private void addDiagnostic(
        ModLoadDiagnostic.Severity severity,
        ModLoadDiagnostic.Stage stage,
        String jarName,
        String className,
        String detail
    ) {
        ModLoadDiagnostic diagnostic = new ModLoadDiagnostic(
            severity,
            stage,
            jarName,
            className,
            detail
        );
        diagnostics.add(diagnostic);
        System.err.println("[ModLoader] " + diagnostic.toDisplayString());
    }
}
