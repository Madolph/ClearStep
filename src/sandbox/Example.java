package sandbox;

import java.util.concurrent.CountDownLatch;

import com.sun.javafx.application.PlatformImpl;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Example
{


  private Button mBtn;

  public Example()
  {
  }

  public void open()
  {
    final CountDownLatch lCountDownLatch = new CountDownLatch(1);

    PlatformImpl.startup(() -> {
    });
    Platform.runLater(() -> {

      mBtn = new Button();
      mBtn.setText("Say 'Hello World'");
      mBtn.setOnAction(new EventHandler<ActionEvent>()
      {

        @Override
        public void handle(ActionEvent event)
        {
          System.out.println("Hello World!");
        }
      });

      StackPane root = new StackPane();
      root.getChildren().add(mBtn);

      Scene scene = new Scene(root, 300, 250);

      Stage primaryStage = new Stage();

      primaryStage.setTitle("Hello World!");
      primaryStage.setScene(scene);
      primaryStage.show();

      lCountDownLatch.countDown();
    });

    try
    {
      lCountDownLatch.await();
    }
    catch (InterruptedException e)
    {
    }
  }

  void setButtonText(String pText)
  {
    Platform.runLater(() -> {
      mBtn.setText(pText);
    });
  }

}
