package com.logicgate.ui;

/**
 * JavaFX 애플리케이션을 단일 실행 파일(Fat JAR)이나 jpackage를 이용한 네이티브 앱으로 패키징할 때,
 * JavaFX 런타임 모듈 인식 문제를 우회하기 위한 런처 클래스입니다.
 * 이 클래스는 Application을 상속받지 않으므로, Classpath에서 JavaFX를 정상적으로 로드할 수 있게 해줍니다.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
