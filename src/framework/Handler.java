package framework;

import java.io.IOException;
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

/**
 * The overlying framework-class that handles all the modules of ClearStep
 * 
 * @author Max
 */
public class Handler 	implements 
						timeStepAdapter{
	
	/**
	 * The Calculator of our Stepper
	 */
	public Calculator mCalc;
	
	/**
	 * The Predictor of our Stepper 
	 */
	public Predictor mPred;
	
	/**
	 * The TimeStepper of our Stepper
	 */
	public TimeStepper mTimeStepper;
	
	/**
	 * stores whether or not JFx has been initialized
	 */
	public boolean mFxOn = false;
	
	/**
	 * the Context used for ClearCL-operations
	 */
	public ClearCLContext mContext;
	
	/**
	 * the device used for ClearCL-operations
	 */
	public ClearCLDevice mFastestGPUDevice;
	
	/**
	 * Necessary for the initiation of ClearCL
	 */
	public ClearCL mClearCL;
	
	/**
	 * Necessary for the initiation of ClearCL
	 */
	public ClearCLBackendInterface mClearCLBackendInterface;
	
	/**
	 * ClearCL program containing kernels for calculation
	 */
	public ClearCLProgram calculations;
	
	/**
	 * ClearCL program containing kernels for noise cleaning
	 */
	public ClearCLProgram noiseCleaner;
	
	/**
	 * creates a new Handler for dynamic time-stepping
	 * @param Context can be null, in which case new context will be created
	 * @param DataType the DataType of images pasted into the Handler (will be converted to float)
	 * @throws IOException 
	 */
	public Handler(ClearCLContext Context, ImageChannelDataType DataType) throws IOException
	{	
		// create a new Context if null was given at creation
		if (Context == null)
		{
			mClearCLBackendInterface = ClearCLBackends.getBestBackend();
			mClearCL = new ClearCL(mClearCLBackendInterface);
			mFastestGPUDevice = mClearCL.getFastestGPUDeviceForImages();

			mContext = mFastestGPUDevice.createContext();
		}
		// take the Context given
		else 
			{ mContext=Context; }
		
		// The Predictor to be used is controlled here and via the switch statement 
		String Pred = "HoltWinters";
		//String: Pred = "Regression";
		
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
		
		// reasonable setup for internal demos
		mTimeStepper = new TimeStepper(1f, 0.5f, 2f, 0.1f);
		
		//reasonable setup for the microscope
		//mTimeStepper = new TimeStepper(90f, 30f, 150f, 30f);
		
		//creating the Calculator
		mCalc = new Calculator(mContext, createCalcProgram(DataType), createNoiseHandlerProgram());
	}
	
	/**
	 * Creates the program that contains the kernels for calculation
	 * 
	 * @param DataType The format the received image uses
	 * @return the program created
	 * @throws IOException
	 */
	public ClearCLProgram createCalcProgram(ImageChannelDataType DataType) throws IOException
	{
		calculations = mContext.createProgram(KernelDemo.class, "Calculator.cl");
		
		// alter the program according to the datatype of the image received
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
	
	/**
	 * Creates the program that contains the kernels for noise handling
	 */
	public ClearCLProgram createNoiseHandlerProgram() throws IOException
	{
		noiseCleaner= mContext.createProgram(KernelDemo.class, "Noise.cl");
		noiseCleaner.buildAndLog();
		return noiseCleaner;
		
	}
	
	/**
	 * The primary pipeline starts here by pasting an image into the handler
	 * 
	 * @param image the image to be saved by the Stepper
	 * @param time the current time in the imaging run
	 * @param step the current time step applied during imaging
	 */
	public void processImage(ClearCLImage image, float time, float step)
	{	
		// invokes the Calculator to produce a value
		float diff = mCalc.cacheAndCompare(image);
		
		//check if the Calculator has enough data to work
		if (mCalc.filled)
		{
			System.out.println("diff= "+diff);
			
			//invoke the Predictor to produce a metric
			float metric = mPred.predict(diff, time);
			System.out.println("metric= "+metric);
			
			//invoke the Time Stepper to set the actual time step
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
