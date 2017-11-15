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
public class PlotterXY extends Application implements Runnable
{
	
	/**
	 * stores several series of data
	 */
	XYChart.Series<Number, Number>[] mSeries;
	
	/**
	 * the amount of Y-series in the plot
	 */
	int plotSize;
 
	
	/**
	 * creates a plotter for the given number of series
	 * 
	 * @param amount	amount of series to be plotted
	 */
	@SuppressWarnings("unchecked")
	public PlotterXY(int amount)
	{
		mSeries = new XYChart.Series[amount];
		plotSize = amount;
	}
	
	/**
	 * Initializes the window and the series of the plot
	 * 
	 * @param fxOn			indicates if JFx is already running
	 * @param plotTitle		the title of the plot
	 * @param windowTitle	the title of the new window
	 * @param titleX		the title of the X-Axis
	 * @param Tags			the titles of the Y-series
	 * @param height		the height of the window
	 * @param width			the width of the window
	 */
    public void initializePlotter(boolean fxOn, 
    		String plotTitle, String windowTitle, String titleX, String[] Tags,
    		int height, int width) 
    {

    	// if the JFX-framework is not yet started, start it up
    	if (!fxOn)
    	{
    		PlatformImpl.startup(() -> {
    		});
    		//sets fxOn to true, so that its not started twice
    		fxOn=true;
    	}
    	
    	final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () -> 
        {
        	Stage lStage = new Stage();
        	lStage.setTitle(windowTitle);
        	
    	    //defining the axes
    	    final NumberAxis xAxis = new NumberAxis();
    	    xAxis.setAnimated(true);
    	    final NumberAxis yAxis = new NumberAxis();
    	    yAxis.setAnimated(true);
    	    xAxis.setLabel(titleX);
    	    //creating the chart
    	    final LineChart<Number,Number> lLineChart = new LineChart<Number,Number>(xAxis,yAxis);
    	    
    	    lLineChart.setAnimated(true);
    	    lLineChart.setTitle(plotTitle);
    	    
    	    if (Tags.length!=plotSize)
    	    	System.out.println
    	    	("Plotter set for "+plotSize+" but "+Tags.length+" Tags are supplied...");
    	    
    	    for (int i=0;i<plotSize;i++)
    	    	{mSeries[i] = new XYChart.Series<Number, Number>();
    	    	mSeries[i].setName(Tags[i]);}
    	    
    	    Scene scene  = new Scene(lLineChart,height,width);
    	    
    	    for (int i=0;i<plotSize;i++)
    	    	{lLineChart.getData().add(mSeries[i]);}
    	    
    	    lStage.setScene(scene);
    	    lStage.show();

          lCountDownLatch.countDown();
        }
        );
        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }
    }
	
    /**
     * plots a full dataset at a position x
     * 
     * @param x		the x-value of the dataset
     * @param data	the values of the dataset (ordered!)
     */
    public void plotFullDataSetXY(float x, float[] data)
    {
    	if (data.length != plotSize)
	    	System.out.println
	    	("Plotter set for "+plotSize+" but "+data.length+" values are supplied...");
    	
    	final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () ->  	
        { 
        	for (int i=0;i<plotSize;i++)
        		{mSeries[i].getData().add(new XYChart.Data<Number,Number>(x, data[i]));}
        		
        	lCountDownLatch.countDown();
        }
        );

        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }
    }
    
    /**
     * plots a single value at a value x
     * 
     * @param x			the x-value
     * @param y			the value of the series
     * @param indice	the indication of the series to be expanded
     */
    public void plotSingleDataXY(float x, float y, int indice)
    {
    	if (indice > plotSize)
    		System.out.println("invalid indice for data");
    	
    	final CountDownLatch lCountDownLatch = new CountDownLatch(1);
    	
        Platform.runLater( () ->  	
        { 
        	mSeries[indice].getData().add(new XYChart.Data<Number,Number>(x, y));
        	
        	lCountDownLatch.countDown();
        }
        );

        try
        { lCountDownLatch.await(); }
        catch (InterruptedException e) { }			
    }

    /**
     * generic method for JFx
     */
	@Override
	public void run() {
		launch();	
	}

	/**
	 * generic method for JFx
	 */
	@Override
	public void start(Stage arg0) throws Exception {
		
	}
}
