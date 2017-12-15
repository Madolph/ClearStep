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
	 * stores whether or not JFx has been initialized
	 */
	public boolean mFxOn = false;
	
	public ClearCLContext mContext;
	public ClearCLDevice mFastestGPUDevice;
	public ClearCL mClearCL;
	public ClearCLBackendInterface mClearCLBackendInterface;
	// Calculation
	public ClearCLProgram calculations;
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
		mCalc = new Calculator(mContext, createCalcProgram(DataType), createNoiseHandlerProgram());
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
			break;
		default:
			calculations.addDefine("READ_IMAGE", "read_imagef");
			break;
		}
		calculations.buildAndLog();
		
		return calculations;
		
	}
	
	public ClearCLProgram createNoiseHandlerProgram() throws IOException
	{
		noiseCleaner= mContext.createProgram(KernelTest.class, "Noise.cl");
		noiseCleaner.buildAndLog();
		return noiseCleaner;
		
	}
	
	public void processImage(ClearCLImage image, float time, float step)
	{	
		//System.out.println(image.getHeight()+" // "+(int)image.getHeight());
		float diff = mCalc.cacheAndCompare(image);
		if (mCalc.filled)
		{
			System.out.println("diff= "+diff);
			float metric = mPred.predict(diff, time);
			System.out.println("metric= "+metric);
			mTimeStepper.computeNextStep(metric, step);
		
			System.out.println("Timestep is: "+step);
		}
	}
}
