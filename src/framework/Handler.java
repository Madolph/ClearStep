package framework;

import java.io.IOException;

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
import prediction.Predictor;
import prediction.PredictorHoltWinters;
import prediction.PredictorStDev;
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
	// NoiseHandling
	public ClearCLProgram mProgram3;
	
	/**
	 * creates a new Handler for dynamic time-stepping
	 * @param Context if null, new context will be created
	 */
	public Handler(ClearCLContext Context)
	{
		// TODO add option to choose the predictor
		String Pred = "StDev";
		
		// create new Context of null was given
		if (Context == null)
		{
			mClearCLBackendInterface = ClearCLBackends.getBestBackend();
			mClearCL = new ClearCL(mClearCLBackendInterface);
			mFastestGPUDevice = mClearCL.getFastestGPUDeviceForImages();
			System.out.println(mFastestGPUDevice);

			mContext = mFastestGPUDevice.createContext();
		}
		else 
			mContext=Context;
		
		mCalc = new Calculator(mContext);
		
		switch (Pred)
		{
		case "StDev":
			mPred = new PredictorStDev();
		case "HoltWinters":
			mPred = new PredictorHoltWinters();
		default:
			mPred = new PredictorStDev();
		}
		
		mTimeStepper = new TimeStepper(0.5f, 0.2f, 1f, 0.1f);
		
		// might not be necessary
		mDuration = 3600;
	}
	
	public void createCalcProgram(ImageChannelDataType DataType) throws IOException
	{
		mProgram1 = mContext.createProgram(KernelTest.class, "Calculator.cl");
		mProgram1.addDefine("CONSTANT", "1");
		switch (DataType)
		{
		case Float: 
			mProgram1.addDefine("READ_IMAGE", "read_imagef");
			break;
		case UnsignedInt16:
			mProgram1.addDefine("READ_IMAGE", "read_imageui");
		default:
			mProgram1.addDefine("READ_IMAGE", "read_imagef");
			break;
		}
		
	}
	
	public void processImage(ClearCLImage image, float time)
	{	
		float diff = mCalc.cacheAndCompare(image, mProgram1, mProgram3, (int)image.getHeight());
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
	 * TODO should be removed when tests are moved to the demo-class
	 * Initializes all the Classes and ClearCL-Overhead
	 * to use the Handler as the executing class that calls the simulation
	 * and then handles images
	 * @throws IOException
	 * @param StDev specifies the used predictor
	 */
	public void InitializeModules(boolean StDev) throws IOException
	{
		mClearCLBackendInterface = ClearCLBackends.getBestBackend();
		  mClearCL = new ClearCL(mClearCLBackendInterface);
		  {
			  mFastestGPUDevice = mClearCL.getFastestGPUDeviceForImages();
			  System.out.println(mFastestGPUDevice);

			  mContext = mFastestGPUDevice.createContext();
		  }
		
		@SuppressWarnings("unused")
		Simulator lSim = new Simulator();
		mCalc = new Calculator(mContext);
		
		if (StDev)
			mPred = new PredictorStDev();
		else
			mPred = new PredictorHoltWinters();
		
		mTimeStepper = new TimeStepper(0.5f, 0.2f, 1f, 0.1f);	
	}
}
