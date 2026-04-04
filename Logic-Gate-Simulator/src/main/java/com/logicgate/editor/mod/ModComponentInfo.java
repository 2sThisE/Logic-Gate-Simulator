package com.logicgate.editor.mod;

/**
 * ModLoader가 찾아낸 외부 컴포넌트의 메타데이터를 담는 DTO입니다.
 */
public class ModComponentInfo {
    public final String section;
    public final String name;
    public final String fqn; // Fully Qualified Name (패키지 포함 전체 클래스 이름)

    public ModComponentInfo(String section, String name, String fqn) {
        this.section = section;
        this.name = name;
        this.fqn = fqn;
    }
}