package timestepping;

import java.io.IOException;

import org.junit.Test;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;

public class Handler {

	/**
	 * The simulator used for the Test
	 */
	public Simulator mSim;
	
	/**
	 * The Calculator used for the Test
	 */
	public Calculator mCalc;
	
	public Predictor mPred;
	
	/**
	 * The TimeStepper used for the Test
	 */
	public TimeStepper mTimeStepper;
	
	public Plotter mPlotter;
	
	/**
	 * duration of the Test in seconds
	 */
	public float mDuration = 3600;
	
	/**
	 * stores whether or not JFx has been initialized
	 */
	boolean mFxOn = false;
	
	public ClearCLContext mContext;
	public ClearCLDevice mFastestGPUDevice;
	public ClearCL mClearCL;
	public ClearCLBackendInterface mClearCLBackendInterface;
	public ClearCLProgram mProgram1;
	public ClearCLProgram mProgram2;
	
	/**
	 * Initializes all the Classes and ClearCL-Overhead
	 * @throws IOException
	 */
	public void InitializeModules(boolean StDev) throws IOException
	{
		mSim = new Simulator();
		
		mClearCLBackendInterface = ClearCLBackends.getBestBackend();
		  mClearCL = new ClearCL(mClearCLBackendInterface);
		  {
			  mFastestGPUDevice = mClearCL.getFastestGPUDeviceForImages();
			  System.out.println(mFastestGPUDevice);

			  mContext = mFastestGPUDevice.createContext();
		  }
		
		mSim = new Simulator();
		mCalc = new Calculator(mContext);
		
		if (StDev)
			mPred = new PredictorStDev();
		else
			mPred = new PredictorHoltWinters();
		
		mTimeStepper = new TimeStepper(0.5f, 0.2f, 1f, 0.1f);	
		
		mProgram1 = mContext.createProgram(Handler.class, "Calculator.cl");
		mProgram1.addDefine("CONSTANT", "1");
		mProgram1.buildAndLog();
		
		mProgram2 = mContext.createProgram(Handler.class, "Simulator.cl");
		mProgram2.addDefine("CONSTANT", "1");
		mProgram2.buildAndLog();
	}
	
	@Test
	public void SimpleSimStepper() throws IOException, InterruptedException
	{
		boolean StDev = true;
		
		InitializeModules(StDev);
		
		mPlotter = new Plotter();
		mPlotter.initializePlotSimpleSimStepper(mFxOn);
		
		int lSize = 128;
		ClearCLImage lImage = mContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
		float time=0;
		while (time<(mDuration*1000))  
		{
			float currStep = mTimeStepper.mStep;
			System.out.println("current time is: "+time+" with step: "+currStep);
			  
			mSim.generatePic(mContext, mProgram2, time, lImage, lSize, false);
			lImage.notifyListenersOfChange(mContext.getDefaultQueue());
			mCalc.CachePic(lImage, mContext, lSize);
			if (mCalc.filled)
			{			  
				float diff = mCalc.compareImages(mProgram1, lSize);
				float metric;
				if (StDev)
				{
					metric = mPred.predict(diff, time);
				}
				else
				{
					diff = diff/currStep;
					metric = mPred.predict(diff);
				}
				float step = mTimeStepper.computeNextStep(metric);  
				
				mPlotter.plotDataSimpleSimStepper(time,
						step/100,
						mPred.prediction,
						mPred.average);
			}
			time += currStep;
			Thread.sleep((long) currStep);
		}  
		lViewImage.waitWhileShowing();
	}
	
	
	@Test
	public void PredictorDemo()
	{
		PredictorHoltWinters lPred = new PredictorHoltWinters();
		int i = 0;
		float res;
		float val=0;
		while (i<100)
		{		
			res = lPred.predict(val);
			val++;
			System.out.println(res);
			i++;
		}
		
	}
}
