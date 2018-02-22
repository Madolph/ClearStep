package demo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import fastfuse.FastFusionEngine;
import fastfuse.FastFusionMemoryPool;
import fastfuse.stackgen.StackGenerator;
import fastfuse.tasks.AverageTask;
import fastfuse.tasks.MemoryReleaseTask;
import framework.Handler;

import plotting.PlotterXY;
import prediction.PredictorHoltWinters;
import simbryo.synthoscopy.microscope.lightsheet.drosophila.LightSheetMicroscopeSimulatorDrosophila;
import simulation.Simulator;

public class HandlerDemo {

	
	@Test
	public void createAndMeasureSimbryoStack() throws Exception
	{
		Handler lHandler = new Handler(null, ImageChannelDataType.UnsignedInt16);
			  
		// now that this is done, we initialize the time and create two images that will
		// be filled by the simulator during the run	  
		int lSize = 512;
			  
		int lPhantomWidth = lSize;
		int lPhantomHeight = lPhantomWidth;
		int lPhantomDepth = lPhantomWidth;
		      
		//hier drigend auf Datentyp achten
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, 
																lSize, lSize, lSize);
			  
		ClearCLImageViewer lView = ClearCLImageViewer.view(lImage);
		
		int lNumberOfDetectionArms = 2;
		int lNumberOfIlluminationArms = 4;
		int lMaxCameraResolution = lSize;
			  
		LightSheetMicroscopeSimulatorDrosophila lSimulator =
                      new LightSheetMicroscopeSimulatorDrosophila(lHandler.mContext,
                                                             		lNumberOfDetectionArms,
                                                             		lNumberOfIlluminationArms,
                                                             		lMaxCameraResolution,
                                                             		5f,
                                                             		lPhantomWidth,
                                                             		lPhantomHeight,
                                                             		lPhantomDepth);
		
		System.out.println("DrosoSim is done");
			  
		StackGenerator lStackGenerator = new StackGenerator(lSimulator);
		
		FastFusionEngine lFastFusionEngine = new FastFusionEngine(lHandler.mContext);
		
		@SuppressWarnings("unused")
		FastFusionMemoryPool lMemoryPool = FastFusionMemoryPool.getInstance(lHandler.mContext,
                                                 							512 * 1024 * 1024, true);
		
		//opens the viewer to see what the simulated camera sees
		@SuppressWarnings("unused")
		ClearCLImageViewer lCameraImageViewer = lSimulator.openViewerForCameraImage(0);
		
		lSimulator.render(true);
		    
		String[][] Tags = new String[lNumberOfDetectionArms][lNumberOfIlluminationArms];
		
		for (int c=0;c<lNumberOfDetectionArms;c++)
			for (int i=0;i<lNumberOfIlluminationArms;i++)
			{
				Tags[c][i]=(String)("C"+c+"L"+i);
			}
		
		for (int c=0;c<lNumberOfDetectionArms;c++)
			{
				lFastFusionEngine.addTask(new AverageTask(  Tags[c][0], Tags[c][1],
															Tags[c][2], Tags[c][3],
															(String)("C"+c) )       );
			}
			
		
		lFastFusionEngine.addTask(new AverageTask("C0", "C1", "fused"));
		lFastFusionEngine.addTask(new MemoryReleaseTask("fused","C0L0",
														"C0L1","C0L2",
														"C0L3","C0",
														"C1L0","C1L1",
														"C1L2","C1L3",
														"C1"));
		
		lStackGenerator.setCenteredROI(lSize, lSize);

		lStackGenerator.setLightSheetHeight(500f);
		lStackGenerator.setLightSheetIntensity(10f);

		int blubb = 0;
		while (blubb<1)
		{
			for (int c = 0; c < lNumberOfDetectionArms; c++)
				for (int l = 0; l < lNumberOfIlluminationArms; l++)
			    {
					String lKey = String.format("C%dL%d", c, l);
	
					System.out.print("now generating stacks");
					
			        lStackGenerator.generateStack(c, l, -lSize/2f, lSize/2f, lSize);
	
			        lFastFusionEngine.passImage(lKey,lStackGenerator.getStack());
			    }
			
			lFastFusionEngine.executeAllTasks();
			
			lFastFusionEngine.getImage("fused").copyTo(lImage, true);
			
			lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
			
			blubb ++;
		}
		lView.waitWhileShowing();
		lSimulator.close();
		lStackGenerator.close();
	}
	
	@Test
	public void SimpleStepper() throws IOException, InterruptedException
	{
		ImageChannelDataType Datatype = ImageChannelDataType.Float;
		//ImageChannelDataType Datatype = ImageChannelDataType.Float;
		
		Handler lHandler = new Handler(null, Datatype);
		
		Simulator lSim = new Simulator(Datatype, lHandler.mContext);
		
		PlotterXY Plotter = new PlotterXY(3);
		String[] Titles = new String[3];
		Titles[0] = "timestep";
		Titles[1] = "prediction";
		Titles[2] = "average";
		Plotter.initializePlotter(lHandler.mFxOn, "Flummi-Demo", "Plot", "time", Titles, 1000, 1000);
		
		int lSize = 126;
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(Datatype, lSize, lSize, lSize);
		ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage, "Flummi");
		ClearCLImageViewer compPic = ClearCLImageViewer.view(lImage, "difference-map");

		float time=0;
		float[] data = new float[3];
		float Duration = 120;
		
		String Steps = "Steps: ";
		String Track = "Track: ";
		String Metric = "Metric: ";
		String Values = "Values: ";
		
		while (time<(Duration*1000))  
		{
			float currStep = lHandler.mTimeStepper.mStep;
			float step = currStep;
			System.out.println("current time is: "+time+" with step: "+currStep/1000+"s");
			  
			lSim.generatePic(time, lImage, lSize, true);
			lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
			lHandler.processImage(lImage, time, step);
			
			if (lHandler.mCalc.filled)
			{
				compPic.setImage(lHandler.mCalc.mImage);
				
				data[0] = lHandler.mTimeStepper.mStep/200;
				Steps = Steps+lHandler.mTimeStepper.mStep+" ";
				
				data[1] = lHandler.mPred.value2;
				Metric = Metric+lHandler.mPred.value2+" ";
				
				data[2] = lHandler.mPred.value3;
				Track = Track+lHandler.mPred.value3+" ";
				
				Values = Values+lHandler.mPred.value1+" ";
				Plotter.plotFullDataSetXY(time, data);
			}
			
			time += currStep;
			Thread.sleep((long) currStep/5);
		}  
		
		
		List<String> lines1 = Arrays.asList(Steps, Metric, Track, Values);
		Path file1 = Paths.get("SimData.txt");
		try {
			Files.write(file1, lines1, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		lViewImage.waitWhileShowing();
	}
	
	@Test
	public void PredictorDemo() throws InterruptedException
	{
		PredictorHoltWinters lPred = new PredictorHoltWinters();
		int amount = 3;
		float time = 1;
		int i = 0;
		@SuppressWarnings("unused")
		float res;
		float val=0;
		
		String[] Tags = new String[amount];
		Tags[0]="Value";
		Tags[1]="Series-Level";
		Tags[2]="Trend";
		
		PlotterXY plot = new PlotterXY(amount);
		plot.initializePlotter(false, "HoltWinters", "demoPlot", "time", Tags, 1000, 1000);
		
		while (i<100)
		{		
			res = lPred.predict(val, time);
			val++;
			i++;
			time ++;
			
			float[] data = new float[amount];
			data[0] = val;
			data[1] = lPred.SN;
			data[2] = lPred.TN*50;
			
			plot.plotFullDataSetXY(time, data);
			
			Thread.sleep(1000);
		}
		
	}
	
	@Test
	public void runStepperwithTxt() throws IOException
	{
		Handler lHandler = new Handler(null, ImageChannelDataType.Float);
		
		Path path = FileSystems.getDefault().getPath("./Data.txt");
		byte[] encoded = Files.readAllBytes(path);
		String text = new String(encoded, "UTF-8");
		String[] numbers = text.split(" ");
		float[] values = new float[numbers.length-1];
		for (int i=1;i<numbers.length;i++)
		{
			values[values.length-i]=Float.valueOf(numbers[i]);
		}
		
		float time=0;
		float setStep=90000;
		String results = "step: ";
		String level = "niveau: ";
		int niveau=10;
		
		
		for (int i=0;i<values.length;i++)
		{
			time = time +setStep;
			float metric = lHandler.mPred.predict(values[i], time);
			lHandler.mTimeStepper.computeNextStep(metric, setStep);
			results = results+lHandler.mTimeStepper.mStep+" ";
			
			if (lHandler.mTimeStepper.mStep<89000&&lHandler.mTimeStepper.mStep>91000)
			{
				;
			}
			if (lHandler.mTimeStepper.mStep>91000)
			{
				niveau=niveau+1;
			}
			if (lHandler.mTimeStepper.mStep<89000)
			{
				niveau=niveau-1;
			}
			if (lHandler.mTimeStepper.mStep<85000)
			{
				niveau=niveau-1;
			}
			if (lHandler.mTimeStepper.mStep>95000)
			{
				niveau=niveau+1;
			}
			
			level = level+niveau+" ";
		}
		
		System.out.println(results);
		
		List<String> lines1 = Arrays.asList(results, level);
		Path file1 = Paths.get("DataProcessed.txt");
		try {
			Files.write(file1, lines1, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	@Test
	public void readTxT() throws IOException
	{
		Path path = FileSystems.getDefault().getPath("./testTxT.txt");
		byte[] encoded = Files.readAllBytes(path);
		String text = new String(encoded, "UTF-8");
		System.out.println(text);
		String[] numbers = text.split(" ");
		for (int i=0;i<numbers.length;i++)
		{
			System.out.println(numbers[i]);
		}
	}
}
