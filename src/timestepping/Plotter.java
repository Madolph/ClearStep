package timestepping;

import java.util.concurrent.CountDownLatch;

import com.sun.javafx.application.PlatformImpl;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class Plotter extends Application implements Runnable{
	
	XYChart.Series<Number, Number> series;
	
	@Override 
	public void start(Stage stage) {
	    stage.setTitle("Deviation-Plot");
	    //defining the axes
	    final NumberAxis xAxis = new NumberAxis();
	    xAxis.setAnimated(true);
	    final NumberAxis yAxis = new NumberAxis();
	    yAxis.setAnimated(true);
	    
	    xAxis.setLabel("Time");
	    //creating the chart
	    final LineChart<Number,Number> lineChart = 
	    		new LineChart<Number,Number>(xAxis,yAxis);
	    
	    lineChart.setAnimated(true);
	                
	    lineChart.setTitle("Deviation over Time");
	    //defining a series
	    series = new XYChart.Series<Number, Number>();
	    series.setName("Deviations");
	    //populating the series with data
	    
	    Scene scene  = new Scene(lineChart,800,600);
	    lineChart.getData().add(series);
	    
	    stage.setScene(scene);
	    stage.show();
	}
 
	public void plotdata(float time, float diff)
	{
		final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () ->  { 
        	series.getData().add(new XYChart.Data<Number,Number>(time, diff)); 
        	lCountDownLatch.countDown();
        });

        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }
	}
	
    public void plot(boolean fxOn) {

    	if (!fxOn)
    	{
        PlatformImpl.startup(() -> {
        });
        //sets fxOn to true, so that its not started twice
        fxOn=true;
    	}
    	
    	final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () -> {
        	Stage stage = new Stage();
        	stage.setTitle("Deviation-Plot");
    	    //defining the axes
    	    final NumberAxis xAxis = new NumberAxis();
    	    xAxis.setAnimated(true);
    	    final NumberAxis yAxis = new NumberAxis();
    	    yAxis.setAnimated(true);
    	    
    	    xAxis.setLabel("Time");
    	    //creating the chart
    	    final LineChart<Number,Number> lineChart = 
    	    		new LineChart<Number,Number>(xAxis,yAxis);
    	    
    	    lineChart.setAnimated(true);
    	                
    	    lineChart.setTitle("Deviation over Time");
    	    //defining a series
    	    series = new XYChart.Series<Number, Number>();
    	    series.setName("Deviations");
    	    //populating the series with data
    	    
    	    Scene scene  = new Scene(lineChart,800,600);
    	    lineChart.getData().add(series);
    	    
    	    stage.setScene(scene);
    	    stage.show();

          lCountDownLatch.countDown();
        });

        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }
    }

	@Override
	public void run() {
		launch();
		
	}
}
