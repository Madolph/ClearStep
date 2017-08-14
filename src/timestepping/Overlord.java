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
import imagecreator.Demo;

public class Overlord {
	
	public Simulator Sim;
	public Calculator Calc;
	public TimeStepper Stepper = new TimeStepper((float) 0.3, (float) 0.2);
	public float startStep;
	public float span;
	//duration in seconds
	public float duration = 600;

	@Test
	public void SimulateStepper() throws InterruptedException, IOException
	{
	// create the Overhead for the actual Test
	  ClearCLBackendInterface lClearCLBackendInterface = ClearCLBackends.getBestBackend();
	  try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
	  {
		  ClearCLDevice lFastestGPUDevice = lClearCL.getFastestGPUDeviceForImages();
		  System.out.println(lFastestGPUDevice);

		  ClearCLContext lContext = lFastestGPUDevice.createContext();

		  ClearCLProgram lProgram = lContext.createProgram(Demo.class, "CalcKernels.cl");
		  lProgram.addDefine("CONSTANT", "1");
		  lProgram.buildAndLog();
		  
		  // now that this is done, we initialize the time and create two images that will
		  // be filled by the simulator during the run
		  float time=0;
		  
		  int lSize = 128;

		    ClearCLImage lImage1 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		    ClearCLImage lImage2 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		  
		  // as long as we aren't above the time, we will now generate pictures and compute timesteps from them
		  while (time<(duration*1000))  
		  {
			  // we need the current step after the new one was set, so we cache it here
			  float oldStep = Stepper.step;
			  // generates two pictures that depend on the timepoint that is supplied
			  Sim.generatePics(lContext, lProgram, time, lImage1, lImage2, lSize, oldStep);
			  // computes the difference between the two pictures
			  float diff = Calc.compareImages(lContext, lProgram, lImage1, lImage2, lSize);
			  // computed the step out of the saved difference
			  float step = Stepper.computeStep(diff);
		  
			  // put the Thread to sleep to simulate realtime... kinda... sorta
			  Thread.sleep((long) oldStep); 
		  
			  System.out.println(step);
		  }
	  }
	}
}
