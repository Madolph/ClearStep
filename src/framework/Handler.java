package framework;

import java.io.IOException;

import org.junit.Test;

import Kernels.KernelTest;
import calculation.Calculator;
import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import fastfuse.FastFusionEngine;
import fastfuse.stackgen.StackGenerator;
import fastfuse.tasks.AverageTask;
import plotting.Plotter;
import prediction.Predictor;
import prediction.PredictorHoltWinters;
import prediction.PredictorStDev;
import simbryo.synthoscopy.microscope.lightsheet.drosophila.LightSheetMicroscopeSimulatorDrosophila;
import simulation.Simulator;
import timestepping.TimeStepper;

public class Handler implements timeStepAdapter{
	
	/**
	 * The Calculator used for the Test
	 */
	public Calculator mCalc;
	
	public Predictor mPred;
	
	/**
	 * The TimeStepper used for the Test
	 */
	public TimeStepper mTimeStepper;
	
	/**
	 * duration of the Test in seconds
	 */
	public float mDuration;
	
	/**
	 * stores whether or not JFx has been initialized
	 */
	public boolean mFxOn = false;
	
	public ClearCLContext mContext;
	public ClearCLDevice mFastestGPUDevice;
	public ClearCL mClearCL;
	public ClearCLBackendInterface mClearCLBackendInterface;
	// Calculation
	public ClearCLProgram mProgram1;
	// Generation
	public ClearCLProgram mProgram2;
	
	public Handler(ClearCLContext Context)
	{
		
	}
	
	public Handler()
	{
		// add option to choose the predictor
		boolean StDev = true;
		
		if (mContext==null)
		{
			mClearCLBackendInterface = ClearCLBackends.getBestBackend();
			mClearCL = new ClearCL(mClearCLBackendInterface);
			mFastestGPUDevice = mClearCL.getFastestGPUDeviceForImages();
			System.out.println(mFastestGPUDevice);

			mContext = mFastestGPUDevice.createContext();
		}
		
		mCalc = new Calculator(mContext);
		
		if (StDev)
			mPred = new PredictorStDev();
		else
			mPred = new PredictorHoltWinters();
		
		mTimeStepper = new TimeStepper(0.5f, 0.2f, 1f, 0.1f);
		
		// might not be necessary
		mDuration = 3600;
	}
	
	public boolean checkInitialization()
	{
		boolean initialzed = false;
		if (mContext!=null)
			initialzed = true;
		return initialzed;
	}
	
	public void passContext(ClearCLContext Context)
	{
		mContext = Context;
	}
	
	public void processImage(ClearCLImage image, float time)
	{
		boolean ready = checkInitialization();
		
		float diff = mCalc.cacheAndCompare(image, mProgram1, (int)image.getHeight());
		boolean StDev = true;
		float metric;
		if (StDev)
		{
			metric = mPred.predict(diff, time);
		}
		else
		{
			metric = mPred.predict(diff, time);
		}
		float step = mTimeStepper.computeNextStep(metric);
		
		System.out.println("Timestep is: "+step);
	}
	
	/**
	 * Initializes all the Classes and ClearCL-Overhead
	 * to use the Handler as the executing class that calls the simulation
	 * and then handles images
	 * @throws IOException
	 * @param StDev specifies the used predictor
	 */
	public void InitializeModules(boolean StDev) throws IOException
	{
		Simulator lSim = new Simulator();
		
		mClearCLBackendInterface = ClearCLBackends.getBestBackend();
		  mClearCL = new ClearCL(mClearCLBackendInterface);
		  {
			  mFastestGPUDevice = mClearCL.getFastestGPUDeviceForImages();
			  System.out.println(mFastestGPUDevice);

			  mContext = mFastestGPUDevice.createContext();
		  }
		
		lSim = new Simulator();
		mCalc = new Calculator(mContext);
		
		if (StDev)
			mPred = new PredictorStDev();
		else
			mPred = new PredictorHoltWinters();
		
		mTimeStepper = new TimeStepper(0.5f, 0.2f, 1f, 0.1f);	
	}

	
	
	
	
	@Test
	public void SimbryoTest() throws Exception
	{
		boolean StDev = true;
		
		InitializeModules(StDev);
		
		mProgram1 = mContext.createProgram(KernelTest.class, "Calculator.cl");
		mProgram1.addDefine("CONSTANT", "1");
		mProgram1.buildAndLog();
			  
		// now that this is done, we initialize the time and create two images that will
		// be filled by the simulator during the run
		float time=0;
			  
		int lSize = 128;
			  
		int lPhantomWidth = lSize;
		int lPhantomHeight = lPhantomWidth;
		int lPhantomDepth = lPhantomWidth;
		      
		//hier drigend auf Datentyp achten
		ClearCLImage lImage = mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, 
																lSize, lSize, lSize);
			  
		ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
			  
		Plotter Graph = new Plotter();
		Graph.initializePlot(mFxOn); //mFxOn will be checked and set to true here if it is not already true
			  
		int lNumberOfDetectionArms = 2;
		int lNumberOfIlluminationArms = 4;
		int lMaxCameraResolution = lSize;
			  
		LightSheetMicroscopeSimulatorDrosophila lSimulator =
                      new LightSheetMicroscopeSimulatorDrosophila(mContext,
                                                             		lNumberOfDetectionArms,
                                                             		lNumberOfIlluminationArms,
                                                             		lMaxCameraResolution,
                                                             		5f,
                                                             		lPhantomWidth,
                                                             		lPhantomHeight,
                                                             		lPhantomDepth);
			  
		StackGenerator lStackGenerator = new StackGenerator(lSimulator);
		
		FastFusionEngine lFastFusionEngine = new FastFusionEngine(mContext);
		
		@SuppressWarnings("unused")
		ClearCLImageViewer lCameraImageViewer =
                      lSimulator.openViewerForCameraImage(0);
			  
		// as long as we aren't above the time, we will now generate pictures and compute timesteps from them
		while (time<(mDuration*1000))  
		{
			float currStep = mTimeStepper.mStep;
			System.out.println("current time is: "+time);

			lSimulator.simulationSteps((int)currStep/10);
			/**lSimulator.setNumberParameter(IlluminationParameter.Intensity,
                          					0,
                          					50f);
			lSimulator.setNumberParameter(IlluminationParameter.Intensity,
  											1,
  											50f);**/
			
		    lSimulator.render(true);
		    
		    lFastFusionEngine.addTask(new AverageTask("C0L0",
                    									"C0L1",
                    									"C0L2",
                    									"C0L3",
                    									"C0"));
		    lFastFusionEngine.addTask(new AverageTask("C1L0",
                    									"C1L1",
                    									"C1L2",
                    									"C1L3",
                    									"C1"));
		    lFastFusionEngine.addTask(new AverageTask("C0", "C1", "fused"));
		    
		    lStackGenerator.setCenteredROI(lSize, lSize);
		    
		    lStackGenerator.setLightSheetHeight(50f);
		    lStackGenerator.setLightSheetIntensity(50f);
		    
			for (int c = 0; c < 2; c++)
		        for (int l = 0; l < 4; l++)
		        {
		          String lKey = String.format("C%dL%d", c, l);
		          
		          lStackGenerator.generateStack(c, l, -64f, 64f, 128);
		          
		          lFastFusionEngine.passImage(lKey,
		                                      lStackGenerator.getStack());
		        }
		    
		    lFastFusionEngine.executeAllTasks();
		    
		    lFastFusionEngine.getImage("fused").copyTo(lImage, true);
		    
		    
		    lImage.notifyListenersOfChange(mContext.getDefaultQueue());
			mCalc.CachePic(lImage, mContext, lSize);
				  
			Thread.sleep((long) currStep);
			if (mCalc.filled)
			{			  
				// computes the difference between the two pictures
				float diff = mCalc.compareImages(mProgram1, lSize);
				//System.out.println("diff is: "+diff);
				// computed the step out of the saved difference
				float step = mTimeStepper.computeStep(diff, time);
			  
				//put the Thread to sleep to simulate realtime... kinda... sorta
			  
				//System.out.println("computed step is: "+step);
					  
				Graph.plotdata(time, mTimeStepper.mInfo.mDev[0][0], mTimeStepper.mInfo.mDev[0][0]/*+mTimeStepper.mInfo.mCurrentSigma*/, step, mTimeStepper.mInfo.mMean.val);
			}
			time += currStep;
		}	  
		lViewImage.waitWhileShowing();
		lSimulator.close();
		lStackGenerator.close();
	}
	
	@Test
	public void PredictorDemo()
	{
		PredictorHoltWinters lPred = new PredictorHoltWinters();
		float time = 1;
		int i = 0;
		float res;
		float val=0;
		while (i<100)
		{		
			res = lPred.predict(val, time);
			val++;
			System.out.println(res);
			i++;
			time += 1;
		}
		
	}

}
