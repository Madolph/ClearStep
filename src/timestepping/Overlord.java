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

public class Overlord {
	
	public Simulator Sim = new Simulator();
	public Calculator Calc = new Calculator();
	public TimeStepper Stepper = new TimeStepper((float) 0.3, (float) 0.2);
	public float startStep;
	public float span;
	//duration in seconds
	public float duration = 600;
	
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

		  ClearCLProgram lProgram = lContext.createProgram(Simulator.class, "CalcKernels.cl");
		  lProgram.addDefine("CONSTANT", "1");
		  lProgram.buildAndLog();
		  
		  // now that this is done, we initialize the time and create two images that will
		  // be filled by the simulator during the run
		  float time=0;
		  
		  int lSize = 128;
		  
		  // as long as we aren't above the time, we will now generate pictures and compute timesteps from them
		  while (time<(duration*1000))  
		  {
			  System.out.println("current time is: "+time);
			  ClearCLImage lImage = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
			  
			  Sim.generatePic(lContext, lProgram, time, lImage, lSize);
			  ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
			  
			  Thread.sleep((long) Stepper.step);
			  time += Stepper.step;
			  Calc.CachePic(lImage);
			  if (Calc.filled)
			  {			  
				  // computes the difference between the two pictures
				  float diff = Calc.compareImages(lContext, lProgram, lSize);
				  System.out.println("diff is: "+diff);
				  // computed the step out of the saved difference
				  float step = Stepper.computeStep(diff);
		  
				  // put the Thread to sleep to simulate realtime... kinda... sorta
		  
				  System.out.println("computed step is: "+step);
			  }
		  }
	  }
	}
}
