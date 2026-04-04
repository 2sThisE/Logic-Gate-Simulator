package com.logicgate.editor.utils;

import com.logicgate.gates.*;
import com.logicgate.editor.mod.ComponentMeta;
import java.util.HashMap;
import java.util.Map;

public class NodeFactory {
    private static final Map<String, Class<? extends Node>> registry = new HashMap<>();

    static {
        // 기본 게이트들 등록
        register("And", And.class);
        register("Nand", Nand.class);
        register("Or", Or.class);
        register("Nor", Nor.class);
        register("Not", Not.class);
        register("Xor", Xor.class);
        register("Xnor", Xnor.class);
        register("InputPin", InputPin.class);
        register("OutputPin", OutputPin.class);
        register("Joint", Joint.class);
    }

    public static void register(String typeName, Class<? extends Node> clazz) {
        registry.put(typeName, clazz);
        registry.put(clazz.getName(), clazz); // FQN으로도 등록!
    }

    public static Node createNodeByType(String type) {
        // 1. 레지스트리에서 검색
        Class<? extends Node> clazz = registry.get(type);
        
        // 2. 없으면 리플렉션으로 직접 시도 (외부 모드 지원)
        if (clazz == null) {
            try {
                clazz = (Class<? extends Node>) Class.forName(type);
                register(type, clazz);
            } catch (Exception e) {
                System.err.println("노드 타입을 찾을 수 없습니다: " + type);
                return null;
            }
        }

        try {
            Node node = clazz.getDeclaredConstructor().newInstance();
            
            // ComponentMeta 어노테이션이 있으면 typeId 주입
            ComponentMeta meta = clazz.getAnnotation(ComponentMeta.class);
            if (meta != null && !meta.typeId().isEmpty()) {
                node.setTypeId(meta.typeId());
            } else {
                // 어노테이션이 없으면 클래스 이름(기존 방식)을 fallback으로 사용
                node.setTypeId(clazz.getSimpleName());
            }
            
            return node;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}