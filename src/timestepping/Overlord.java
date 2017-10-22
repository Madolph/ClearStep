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
import simbryo.dynamics.tissue.embryo.zoo.Drosophila;
import simbryo.synthoscopy.microscope.lightsheet.drosophila.LightSheetMicroscopeSimulatorDrosophila;
import simbryo.synthoscopy.microscope.parameters.IlluminationParameter;
import simbryo.synthoscopy.microscope.parameters.PhantomParameter;
import simbryo.synthoscopy.phantom.fluo.impl.drosophila.DrosophilaHistoneFluorescence;
import simbryo.synthoscopy.phantom.scatter.impl.drosophila.DrosophilaScatteringPhantom;

public class Overlord {
	
	/**
	 * The simulator used for the Test
	 */
	public Simulator mSim = new Simulator();
	
	/**
	 * The Calculator used for the Test
	 */
	public Calculator mCalc;
	
	/**
	 * The TimeStepper used for the Test
	 */
	public TimeStepper mTimeStepper = new TimeStepper((float) 0.5, (float) 0.2, (float) 1.0, (float) 0.1, true);
	
	/**
	 * duration of the Test in seconds
	 */
	public float mDuration = 3600;
	
	/**
	 * stores whether or not JFx has been initialized
	 */
	boolean mFxOn = false;
	
	/**
	 * Runs the whole Test
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void TestDiffCalc() throws InterruptedException, IOException
	{
	// create the Overhead for the actual Test
	  ClearCLBackendInterface lClearCLBackendInterface = ClearCLBackends.getBestBackend();
	  try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
	  {
		  ClearCLDevice lFastestGPUDevice = lClearCL.getFastestGPUDeviceForImages();
		  System.out.println(lFastestGPUDevice);

		  ClearCLContext lContext = lFastestGPUDevice.createContext();
		  mCalc = new Calculator(lContext);

		  ClearCLProgram lProgram = lContext.createProgram(Simulator.class, "CalcKernels.cl");
		  lProgram.addDefine("CONSTANT", "1");
		  lProgram.buildAndLog();
		  
		  // now that this is done, we initialize the time and create two images that will
		  // be filled by the simulator during the run
		  float time=0;
		  
		  int lSize = 128;
		  ClearCLImage lImage = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		  
		  ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
		  
		  Plotter Graph = new Plotter();
		  Graph.plot(mFxOn); //mFxOn will be checked and set to true here if it is not already true
		  
		  // as long as we aren't above the time, we will now generate pictures and compute timesteps from them
		  while (time<(mDuration*1000))  
		  {
			  float currStep = mTimeStepper.mStep;
			  System.out.println("current time is: "+time);
			  
			  mSim.generatePic(lContext, lProgram, time, lImage, lSize);
			  lImage.notifyListenersOfChange(lContext.getDefaultQueue());
			  mCalc.CachePic(lImage, lContext, lSize);
			  
			  Thread.sleep((long) currStep);
			  if (mCalc.filled)
			  {			  
				  // computes the difference between the two pictures
				  float diff = mCalc.compareImages(lProgram, lSize);
				  //System.out.println("diff is: "+diff);
				  // computed the step out of the saved difference
				  float step = mTimeStepper.computeStep(diff, time);
		  
				  // put the Thread to sleep to simulate realtime... kinda... sorta
		  
				  //System.out.println("computed step is: "+step);
				  
				  Graph.plotdata(time, mTimeStepper.mInfo.mDev[0][0], mTimeStepper.mInfo.mDev[0][0]/*+mTimeStepper.mInfo.mCurrentSigma*/, step*10, mTimeStepper.mInfo.mMean.val);
			  }
			  time += currStep;
		  }
		  
		  lViewImage.waitWhileShowing();
	  }
	}
	
	@Test
	public void SimbryoTest() throws Exception
	{
		ClearCLBackendInterface lClearCLBackendInterface = ClearCLBackends.getBestBackend();
		  try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
		  {
			  ClearCLDevice lFastestGPUDevice = lClearCL.getFastestGPUDeviceForImages();
			  System.out.println(lFastestGPUDevice);

			  ClearCLContext lContext = lFastestGPUDevice.createContext();
				mCalc = new Calculator(lContext);


			  ClearCLProgram lProgram = lContext.createProgram(Simulator.class, "CalcKernels.cl");
			  lProgram.addDefine("CONSTANT", "1");
			  lProgram.buildAndLog();
			  
			  // now that this is done, we initialize the time and create two images that will
			  // be filled by the simulator during the run
			  float time=0;
			  
			  int lSize = 128;
			  
			  int lPhantomWidth = lSize;
		      int lPhantomHeight = lPhantomWidth;
		      int lPhantomDepth = lPhantomWidth;
		      
		      //hier drigend auf Datentyp achten
		      ClearCLImage lImage = lContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, lSize, lSize, lSize);
			  
			  ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
			  
			  Plotter Graph = new Plotter();
			  Graph.plot(mFxOn); //mFxOn will be checked and set to true here if it is not already true
			  
			  int lNumberOfDetectionArms = 1;
			  int lNumberOfIlluminationArms = 4;
			  int lMaxCameraResolution = lSize;
			  
			  LightSheetMicroscopeSimulatorDrosophila lSimulator =
                      new LightSheetMicroscopeSimulatorDrosophila(lContext,
                                                             lNumberOfDetectionArms,
                                                             lNumberOfIlluminationArms,
                                                             lMaxCameraResolution,
                                                             5f,
                                                             lPhantomWidth,
                                                             lPhantomHeight,
                                                             lPhantomDepth);
			  
			  ClearCLImageViewer lCameraImageViewer =
                      lSimulator.openViewerForCameraImage(0);
			  
			  // as long as we aren't above the time, we will now generate pictures and compute timesteps from them
			  while (time<(mDuration*1000))  
			  {
				  float currStep = mTimeStepper.mStep;
				  System.out.println("current time is: "+time);

				  lSimulator.simulationSteps((int)currStep/10);
				  
				  lSimulator.setNumberParameter(IlluminationParameter.Height,
                          0,
                          1f);
				  lSimulator.setNumberParameter(IlluminationParameter.Intensity,
                          0,
                          50f);
				  lSimulator.setNumberParameter(IlluminationParameter.Gamma,
                          0,
                          0f);

		          lSimulator.render(true);
				  
		          /** Checkt die Werte des Bildes
		          if (lSimulator.getCameraImage(0).isFloat())
		        	  System.out.println("is float");
		          if (lSimulator.getCameraImage(0).isNormalized())
		        	  System.out.println("is normalized");
		          if (lSimulator.getCameraImage(0).isSigned())
		        	  System.out.println("is signed");
		          if (lSimulator.getCameraImage(0).isInteger())
		        	  System.out.println("is int");**/
		          
		          lSimulator.getCameraImage(0).copyTo(lImage, true);
				  lImage.notifyListenersOfChange(lContext.getDefaultQueue());
				  mCalc.CachePic(lImage, lContext, lSize);
				  
				  Thread.sleep((long) currStep);
				  if (mCalc.filled)
				  {			  
					  // computes the difference between the two pictures
					  float diff = mCalc.compareImages(lProgram, lSize);
					  //System.out.println("diff is: "+diff);
					  // computed the step out of the saved difference
					  float step = mTimeStepper.computeStep(diff, time);
			  
					  // put the Thread to sleep to simulate realtime... kinda... sorta
			  
					  //System.out.println("computed step is: "+step);
					  
					  Graph.plotdata(time, mTimeStepper.mInfo.mDev[0][0], mTimeStepper.mInfo.mDev[0][0]/*+mTimeStepper.mInfo.mCurrentSigma*/, step, mTimeStepper.mInfo.mMean.val);
				  }
				  time += currStep;
			  }
			  
			  lViewImage.waitWhileShowing();
			  lSimulator.close();
		  }
	}
}
