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
        try {
            URL[] urls = { jarFile.toURI().toURL() };
            URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }

                    String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        
                        if (Node.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                            ComponentMeta meta = clazz.getAnnotation(ComponentMeta.class);
                            if (meta != null) {
                                String typeName = clazz.getSimpleName();
                                NodeFactory.register(typeName, (Class<? extends Node>) clazz);
                                infos.add(new ModComponentInfo(meta.section(), meta.name(), clazz.getName()));
                                System.out.println("[ModLoader] 외부 게이트 로드 성공 💖: " + meta.name() + " [" + clazz.getName() + "]");
                            }
                        }

                        if (GateSymbol.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                            GateSymbol symbol = (GateSymbol) clazz.getDeclaredConstructor().newInstance();
                            ComponentMeta meta = clazz.getAnnotation(ComponentMeta.class);
                            
                            if (meta != null && !meta.typeId().isEmpty()) {
                                // ID 기반 등록 (강력 추천! ✨)
                                SymbolRegistry.registerExternalSymbol(meta.typeId(), symbol);
                                System.out.println("[ModLoader] 외부 심볼 로드 성공 ✨: " + clazz.getName() + " -> 매핑 ID: " + meta.typeId());
                            } else {
                                // Fallback: 기존 방식
                                String targetType = clazz.getSimpleName().replace("Symbol", "");
                                SymbolRegistry.registerExternalSymbol(targetType, symbol);
                                SymbolRegistry.registerExternalSymbol(className.replace("Symbol", ""), symbol); 
                                System.out.println("[ModLoader] 외부 심볼 로드 성공(이름기반) ✨: " + clazz.getName() + " -> 매핑: " + targetType);
                            }
                        }

                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ModLoader] JAR 파일을 읽는 데 실패했습니다: " + jarFile.getName());
            e.printStackTrace();
        }
        return infos;
    }
}