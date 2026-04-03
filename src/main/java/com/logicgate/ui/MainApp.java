package com.logicgate.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // resources 폴더 안의 FXML 로드
        URL fxmlLocation = getClass().getResource("/com/logicgate/ui/main.fxml");
        if (fxmlLocation == null) {
            System.err.println("main.fxml 파일을 찾을 수 없습니다!");
            System.exit(1);
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();
        
        MainController controller = loader.getController();
        
        // 창이 닫힐 때 컨트롤러의 셧다운 호출 (백그라운드 스레드 종료)
        primaryStage.setOnCloseRequest(e -> controller.shutdown());

        Scene scene = new Scene(root);
        primaryStage.setTitle("Logic Gate Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
