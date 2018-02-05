package framework;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import Kernels.KernelDemo;
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
import timestepping.TimeStepper;

public class Handler 	implements 
						timeStepAdapter{
	
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
		String Pred = "HoltWinters";
		
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
		case "HoltWinters":
			mPred = new PredictorHoltWinters();
			break;
		case "Regression":
			mPred = new PredictorRegression();
			break;
		default:
			mPred = new PredictorRegression();
			break;
		}
		// for our little demo
		mTimeStepper = new TimeStepper(1f, 0.5f, 2f, 0.1f);
		//reasonable for the microscope
		//mTimeStepper = new TimeStepper(90f, 30f, 150f, 30f);
		mCalc = new Calculator(mContext, createCalcProgram(DataType), createNoiseHandlerProgram());
	}
	
	public ClearCLProgram createCalcProgram(ImageChannelDataType DataType) throws IOException
	{
		calculations = mContext.createProgram(KernelDemo.class, "Calculator.cl");
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
		noiseCleaner= mContext.createProgram(KernelDemo.class, "Noise.cl");
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
		
			System.out.println("Timestep is: "+mTimeStepper.mStep);
		}
		
		/*List<String> lines1 = Arrays.asList(mCalc.mValues1);
		Path file1 = Paths.get("DataRaw.txt");
		try {
			Files.write(file1, lines1, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<String> lines2 = Arrays.asList(mCalc.mValues2);
		Path file2 = Paths.get("DataClean.txt");
		try {
			Files.write(file2, lines2, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
