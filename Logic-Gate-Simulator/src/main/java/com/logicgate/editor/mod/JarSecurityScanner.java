package com.logicgate.editor.mod;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarSecurityScanner {

    // 오빠가 지정해준 필수 허용 목록 💖 (Java 기본 패키지 일부 포함)
    private static final String[] WHITELIST = {
        "com/logicgate/gates/Node",
        "com/logicgate/editor/mod/ComponentMeta",
        "com/logicgate/editor/rendering/symbol/AbstractGateSymbol",
        "com/logicgate/editor/model/VisualNode",
        "java/lang/",
        "java/util/",
        "java/io/PrintStream", // 디버깅용 System.out 허용
        "java/io/Serializable",
        "javafx/scene/canvas/GraphicsContext",
        "javafx/scene/paint/Color"
    };

    /**
     * JAR 파일을 스캔하여 화이트리스트에 없는 외부 클래스 참조를 찾아냅니다.
     * @return 발견된 의심스러운 외부 클래스명 리스트
     */
    public static List<String> scanJarForSuspiciousClasses(File jarFile) {
        List<String> suspicious = new ArrayList<>();
        
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                
                // 현재 검사 중인 모드 파일의 최상위 패키지 이름 추출 (자기 자신은 허용하기 위함)
                String topLevelPackage = "";
                int firstSlash = entry.getName().indexOf('/');
                if (firstSlash > 0) {
                    topLevelPackage = entry.getName().substring(0, firstSlash) + "/";
                }

                try (InputStream is = jar.getInputStream(entry);
                     DataInputStream dis = new DataInputStream(is)) {
                    
                    int magic = dis.readInt();
                    if (magic != 0xCAFEBABE) continue; // 클래스 파일 매직 넘버 확인
                    
                    dis.readUnsignedShort(); // minor
                    dis.readUnsignedShort(); // major
                    
                    int cpCount = dis.readUnsignedShort();
                    String[] utf8Pool = new String[cpCount];
                    int[] classPool = new int[cpCount];
                    
                    for (int i = 1; i < cpCount; i++) {
                        int tag = dis.readUnsignedByte();
                        switch (tag) {
                            case 1: // Utf8
                                utf8Pool[i] = dis.readUTF();
                                break;
                            case 3: // Integer
                            case 4: // Float
                            case 9: // Fieldref
                            case 10: // Methodref
                            case 11: // InterfaceMethodref
                            case 12: // NameAndType
                            case 18: // InvokeDynamic
                                dis.skipBytes(4);
                                break;
                            case 5: // Long
                            case 6: // Double
                                dis.skipBytes(8);
                                i++; // Long/Double은 슬롯을 2개 차지함
                                break;
                            case 7: // Class
                                classPool[i] = dis.readUnsignedShort();
                                break;
                            case 8: // String
                            case 16: // MethodType
                            case 19: // Module
                            case 20: // Package
                                dis.skipBytes(2);
                                break;
                            case 15: // MethodHandle
                                dis.skipBytes(3);
                                break;
                            default:
                                break; // 알 수 없는 태그는 무시
                        }
                    }
                    
                    // Class 항목 분석
                    for (int i = 1; i < cpCount; i++) {
                        if (classPool[i] > 0 && classPool[i] < cpCount) {
                            String className = utf8Pool[classPool[i]];
                            if (className != null && className.length() > 0) {
                                // 배열 타입 제거 ("[Ljava/lang/String;" -> "java/lang/String")
                                if (className.startsWith("[")) {
                                    className = className.replaceAll("^\\[+L", "").replaceAll(";$|^\\[+", "");
                                }
                                
                                // 기본 타입이 아니고 패키지 구조를 가졌으며, 자신의 패키지도 아닌 외부 클래스일 때
                                if (className.contains("/") && !className.startsWith(topLevelPackage)) {
                                    if (!isWhitelisted(className)) {
                                        String dottedName = className.replace('/', '.');
                                        if (!suspicious.contains(dottedName)) {
                                            suspicious.add(dottedName);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[JarSecurityScanner] JAR 스캔 중 에러 발생: " + e.getMessage());
        }
        return suspicious;
    }

    private static boolean isWhitelisted(String className) {
        for (String white : WHITELIST) {
            if (className.startsWith(white)) {
                return true;
            }
        }
        return false;
    }
}
