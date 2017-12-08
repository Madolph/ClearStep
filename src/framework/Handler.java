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
import prediction.PredictorRegression;
import prediction.PredictorStDev;
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
	public ClearCLProgram calculations;
	// Generation
	public ClearCLProgram simulation;
	// NoiseHandling
	public ClearCLProgram noiseCleaner;
	
	/**
	 * creates a new Handler for dynamic time-stepping
	 * @param Context if null, new context will be created
	 * @throws IOException 
	 */
	public Handler(ClearCLContext Context, ImageChannelDataType DataType) throws IOException
	{
		// TODO add option to choose the predictor
		String Pred = "Regression";
		
		// create new Context if null was given
		if (Context == null)
		{
			mClearCLBackendInterface = ClearCLBackends.getBestBackend();
			mClearCL = new ClearCL(mClearCLBackendInterface);
			mFastestGPUDevice = mClearCL.getFastestGPUDeviceForImages();

			mContext = mFastestGPUDevice.createContext();
		}
		else 
			{ mContext=Context; }
		
		switch (Pred)
		{
		case "StDev":
			mPred = new PredictorStDev();
			break;
		case "HoltWinters":
			mPred = new PredictorHoltWinters();
			break;
		case "Regression":
			mPred = new PredictorRegression();
			break;
		default:
			mPred = new PredictorStDev();
			break;
		}
		// for our little demo
		//mTimeStepper = new TimeStepper(1f, 1f, 2f, 0.1f);
		//reasonable for the microscope
		mTimeStepper = new TimeStepper(60f, 20f, Float.MAX_VALUE, 0.1f);
		
		createSimProgram();
		mCalc = new Calculator(mContext, createCalcProgram(DataType), createNoiseHandlerProgram(DataType));
		
		// might not be necessary
		mDuration = 3600;
	}
	
	public ClearCLProgram createCalcProgram(ImageChannelDataType DataType) throws IOException
	{
		calculations = mContext.createProgram(KernelTest.class, "Calculator.cl");
		switch (DataType)
		{
		case Float: 
			calculations.addDefine("READ_IMAGE", "read_imagef");
			break;
		case UnsignedInt16:
			calculations.addDefine("READ_IMAGE", "read_imageui");
		default:
			calculations.addDefine("READ_IMAGE", "read_imagef");
			break;
		}
		calculations.buildAndLog();
		
		return calculations;
		
	}
	
	public void createSimProgram() throws IOException
	{
		simulation=mContext.createProgram(KernelTest.class, "Simulator.cl");
		simulation.buildAndLog();
	}
	
	public ClearCLProgram createNoiseHandlerProgram(ImageChannelDataType DataType) throws IOException
	{
		noiseCleaner= mContext.createProgram(KernelTest.class, "Noise.cl");
		switch (DataType)
		{
		case Float: 
			noiseCleaner.addDefine("READ_IMAGE", "read_imagef");
			noiseCleaner.addDefine("WRITE_IMAGE", "write_imagef");
			noiseCleaner.addDefine("DATA", "float4");
			break;
		case UnsignedInt16:
			noiseCleaner.addDefine("READ_IMAGE", "read_imageui");
			noiseCleaner.addDefine("WRITE_IMAGE", "write_imageui");
			noiseCleaner.addDefine("DATA", "uint4");
		default:
			noiseCleaner.addDefine("READ_IMAGE", "read_imagef");
			noiseCleaner.addDefine("WRITE_IMAGE", "write_imagef");
			noiseCleaner.addDefine("DATA", "float4");
			break;
		}
		
		noiseCleaner.buildAndLog();
		return noiseCleaner;
		
	}
	
	public void processImage(ClearCLImage image, float time, float step)
	{	
		float diff = mCalc.cacheAndCompare(image, calculations, noiseCleaner, (int)image.getHeight());
		if (mCalc.filled)
		{
			float metric = mPred.predict(diff, time);
			mTimeStepper.computeNextStep(metric, step);
		
			System.out.println("Timestep is: "+step);
		}
	}
}
