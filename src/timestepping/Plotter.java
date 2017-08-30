package timestepping;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;


public class Plotter extends Application implements Runnable{
	
  XYChart.Series<Number, Number> series;
	float check = 4;
	
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
 
	
    public void plot() {
    	(new Thread(new Plotter())).start();
    }

	@Override
	public void run() {
		launch();
		
	}
}
