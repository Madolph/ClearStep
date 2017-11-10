package plotting;

import java.util.concurrent.CountDownLatch;

import com.sun.javafx.application.PlatformImpl;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

/**
 * Used to plot Data via JavaFx
 * 
 * @author Raddock
 *
 */
public class Plotter extends Application implements Runnable{
	
	XYChart.Series<Number, Number>[] mSeries;
	
	/**
	 * stores image-deviations
	 */
	XYChart.Series<Number, Number> mSeries1;
	
	/**
	 * stores the sigma (dislocated to the deviation)
	 */
	XYChart.Series<Number, Number> mSeries2;
	
	/**
	 * stores the timesteps computed
	 */
	XYChart.Series<Number, Number> mSeries3;
	
	
	
	/**
	 * stores the current mean
	 */
	XYChart.Series<Number, Number> mSeries4;
 
	
	
	
	/**
	 * actually adds data to the series created previously
	 * 
	 * @param time	The Timepoint
	 * @param diff	The Difference-metric
	 * @param sigma	The Sigma
	 * @param step	The temporal stepsize
	 * @param mean	The current mean 
	 */
	public void plotdata(float time, float diff, float sigma, float step, float mean)
	{
		final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () ->  { 
        	mSeries1.getData().add(new XYChart.Data<Number,Number>(time, diff));
        	mSeries2.getData().add(new XYChart.Data<Number,Number>(time, sigma));
        	System.out.println("Sigma is: "+sigma);
        	mSeries3.getData().add(new XYChart.Data<Number,Number>(time, step));
        	mSeries4.getData().add(new XYChart.Data<Number,Number>(time, mean));
        	lCountDownLatch.countDown();
        });

        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }
	}
	
	public void plotDataSimpleSimStepper(float time, float timeStep, float trend, float metric)
	{
		final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () ->  { 
        	mSeries1.getData().add(new XYChart.Data<Number,Number>(time, timeStep));
        	mSeries2.getData().add(new XYChart.Data<Number,Number>(time, trend));
        	mSeries3.getData().add(new XYChart.Data<Number,Number>(time, metric));
        	lCountDownLatch.countDown();
        });

        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }
	}
	
	public void initializePlotSimpleSimStepper(boolean fxOn) {

    	// if the JFX-framework is not yet started, start it up
    	if (!fxOn)
    	{
    		PlatformImpl.startup(() -> {
    		});
    		//sets fxOn to true, so that its not started twice
    		fxOn=true;
    	}
    	
    	final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () -> {
        	Stage lStage = new Stage();
        	lStage.setTitle("Plot");
    	    //defining the axes
    	    final NumberAxis xAxis = new NumberAxis();
    	    xAxis.setAnimated(true);
    	    final NumberAxis yAxis = new NumberAxis();
    	    yAxis.setAnimated(true);
    	    
    	    xAxis.setLabel("Time");
    	    //creating the chart
    	    final LineChart<Number,Number> lLineChart = 
    	    		new LineChart<Number,Number>(xAxis,yAxis);
    	    
    	    lLineChart.setAnimated(true);
    	                
    	    lLineChart.setTitle("Deviation over Time");
    	    //defining a series
    	    mSeries1 = new XYChart.Series<Number, Number>();
    	    mSeries1.setName("Timestep");
    	    mSeries2 = new XYChart.Series<Number, Number>();
    	    mSeries2.setName("Trend");
    	    mSeries3 = new XYChart.Series<Number, Number>();
    	    mSeries3.setName("Metric");
    	    //populating the series with data
    	    
    	    Scene scene  = new Scene(lLineChart,1200,900);
    	    lLineChart.getData().add(mSeries1);
    	    lLineChart.getData().add(mSeries2);
    	    lLineChart.getData().add(mSeries3);
    	    
    	    lStage.setScene(scene);
    	    lStage.show();

          lCountDownLatch.countDown();
        });

        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }
    }
	
	/**
	 * prepares the plot of the 4 series into a chart
	 * 
	 * @param fxOn	says whether JFx is initiated
	 */
    public void initializePlot(boolean fxOn) {

    	// if the JFX-framework is not yet started, start it up
    	if (!fxOn)
    	{
    		PlatformImpl.startup(() -> {
    		});
    		//sets fxOn to true, so that its not started twice
    		fxOn=true;
    	}
    	
    	final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () -> {
        	Stage lStage = new Stage();
        	lStage.setTitle("Plot");
    	    //defining the axes
    	    final NumberAxis xAxis = new NumberAxis();
    	    xAxis.setAnimated(true);
    	    final NumberAxis yAxis = new NumberAxis();
    	    yAxis.setAnimated(true);
    	    
    	    xAxis.setLabel("Time");
    	    //creating the chart
    	    final LineChart<Number,Number> lLineChart = 
    	    		new LineChart<Number,Number>(xAxis,yAxis);
    	    
    	    lLineChart.setAnimated(true);
    	                
    	    lLineChart.setTitle("Deviation over Time");
    	    //defining a series
    	    mSeries1 = new XYChart.Series<Number, Number>();
    	    mSeries1.setName("Deviations");
    	    mSeries2 = new XYChart.Series<Number, Number>();
    	    mSeries2.setName("Sigma");
    	    mSeries3 = new XYChart.Series<Number, Number>();
    	    mSeries3.setName("Step");
    	    mSeries4 = new XYChart.Series<Number, Number>();
    	    mSeries4.setName("mean");
    	    //populating the series with data
    	    
    	    Scene scene  = new Scene(lLineChart,1200,900);
    	    lLineChart.getData().add(mSeries1);
    	    lLineChart.getData().add(mSeries2);
    	    lLineChart.getData().add(mSeries3);
    	    lLineChart.getData().add(mSeries4);
    	    
    	    lStage.setScene(scene);
    	    lStage.show();

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

	@Override
	public void start(Stage arg0) throws Exception {
		
	}
}
