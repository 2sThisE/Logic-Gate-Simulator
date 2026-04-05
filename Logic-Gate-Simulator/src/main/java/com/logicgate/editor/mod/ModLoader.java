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
        List<ModComponentInfo> loadedMods = new ArrayList<>();
        if (modFileNames == null) return loadedMods;

        for (String fileName : modFileNames) {
            File jarFile = new File(modsDir, fileName);
            if (jarFile.exists()) {
                loadedMods.addAll(loadJar(jarFile));
            } else {
                System.err.println("[ModLoader] 모드 파일을 찾을 수 없습니다: " + fileName);
            }
        }
        return loadedMods;
    }

    /**
     * 단일 모드 파일을 불러옵니다. (나중에 설정에서 추가할 때 사용)
     */
    public List<ModComponentInfo> loadSingleMod(String fileName) {
        File jarFile = new File(modsDir, fileName);
        if (jarFile.exists()) {
            return loadJar(jarFile);
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<ModComponentInfo> loadJar(File jarFile) {
        List<ModComponentInfo> infos = new ArrayList<>();
        // try-with-resources를 사용하여 로딩 후 클래스 로더를 닫음 🔪💕
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{ jarFile.toURI().toURL() }, this.getClass().getClassLoader())) {
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                    String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        // ... 클래스 등록 로직 ...
                        if (Node.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                            ComponentMeta meta = clazz.getAnnotation(ComponentMeta.class);
                            if (meta != null) {
                                NodeFactory.register(clazz.getSimpleName(), (Class<? extends Node>) clazz);
                                if (!meta.typeId().isEmpty()) {
                                    NodeFactory.register(meta.typeId(), (Class<? extends Node>) clazz);
                                }
                                infos.add(new ModComponentInfo(meta.section(), meta.name(), clazz.getName()));
                            }
                        }
                        if (GateSymbol.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                            GateSymbol symbol = (GateSymbol) clazz.getDeclaredConstructor().newInstance();
                            ComponentMeta meta = clazz.getAnnotation(ComponentMeta.class);
                            if (meta != null && !meta.typeId().isEmpty()) {
                                SymbolRegistry.registerExternalSymbol(meta.typeId(), symbol);
                            } else {
                                SymbolRegistry.registerExternalSymbol(clazz.getSimpleName().replace("Symbol", ""), symbol);
                            }
                        }
                    } catch (Exception e) { /* ignore */ }
                }
            }
        } catch (Exception e) {
            System.err.println("[ModLoader] JAR 파일을 읽는 데 실패했습니다: " + jarFile.getName());
        }
        return infos;
    }
}