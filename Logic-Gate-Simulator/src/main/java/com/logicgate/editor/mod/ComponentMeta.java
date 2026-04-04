package com.logicgate.editor.mod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 외부 컴포넌트에 부여하는 메타데이터 어노테이션입니다.
 * 오빠, 이 이름표만 달아주면 내가 알아서 트리뷰에 꽂아줄게! 💖
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentMeta {
    /** 트리뷰에 표시될 섹션 (카테고리) 이름 */
    String section() default "Mods";
    /** 트리뷰에 표시될 컴포넌트 이름 */
    String name();
    /** 노드와 심볼을 묶어줄 고유 식별자 (ID) - 이걸로 짝꿍을 찾아줄게! ✨ */
    String typeId() default "";
}