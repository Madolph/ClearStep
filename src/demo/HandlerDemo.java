package demo;

import java.io.IOException;

import org.junit.Test;

import Kernels.KernelTest;
import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import fastfuse.FastFusionEngine;
import fastfuse.FastFusionMemoryPool;
import fastfuse.stackgen.StackGenerator;
import fastfuse.tasks.AverageTask;
import fastfuse.tasks.MemoryReleaseTask;
import framework.Handler;
import plotting.Plotter;
import simbryo.synthoscopy.microscope.lightsheet.drosophila.LightSheetMicroscopeSimulatorDrosophila;
import simulation.Simulator;

public class HandlerDemo {

	
	@Test
	public void createAndMeasureSimbryoStack() throws Exception
	{
		Handler lHandler = new Handler();
		
		boolean StDev = true;
		
		lHandler.InitializeModules(StDev);
		
		lHandler.mProgram1 = lHandler.mContext.createProgram(KernelTest.class, "Calculator.cl");
		lHandler.mProgram1.addDefine("CONSTANT", "1");
		lHandler.mProgram1.buildAndLog();
			  
		// now that this is done, we initialize the time and create two images that will
		// be filled by the simulator during the run	  
		int lSize = 128;
			  
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
                                                 							100 * 1024 * 1024, true);
		
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
	public void SimpleSimStepper() throws IOException, InterruptedException
	{
		Handler lHandler = new Handler();
		
		boolean StDev = true;
		
		Simulator lSim = new Simulator();
		
		lHandler.InitializeModules(StDev);
		
		lHandler.mProgram1 = lHandler.mContext.createProgram(KernelTest.class, "Calculator.cl");
		lHandler.mProgram1.addDefine("CONSTANT", "1");
		lHandler.mProgram1.buildAndLog();
		
		lHandler.mProgram2 = lHandler.mContext.createProgram(KernelTest.class, "Simulator.cl");
		lHandler.mProgram2.addDefine("CONSTANT", "1");
		lHandler.mProgram2.buildAndLog();
		
		Plotter Plotter = new Plotter();
		Plotter.initializePlotSimpleSimStepper(lHandler.mFxOn);
		
		int lSize = 128;
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
		float time=0;
		while (time<(lHandler.mDuration*1000))  
		{
			float currStep = lHandler.mTimeStepper.mStep;
			System.out.println("current time is: "+time+" with step: "+currStep);
			  
			lSim.generatePic(lHandler.mContext, lHandler.mProgram2, time, lImage, lSize, false);
			lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
			lHandler.mCalc.CachePic(lImage, lHandler.mContext, lSize);
			if (lHandler.mCalc.filled)
			{			  
				float diff = lHandler.mCalc.compareImages(lHandler.mProgram1, lSize);
				float metric;
				if (StDev)
				{
					metric = lHandler.mPred.predict(diff, time);
				}
				else
				{
					metric = lHandler.mPred.predict(diff, time);
				}
				float step = lHandler.mTimeStepper.computeNextStep(metric);  
				
				Plotter.plotDataSimpleSimStepper(time,
						step/100,
						lHandler.mPred.prediction,
						lHandler.mPred.average);
			}
			time += currStep;
			Thread.sleep((long) currStep);
		}  
		lViewImage.waitWhileShowing();
	}
}
