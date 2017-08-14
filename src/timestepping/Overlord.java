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
	public TimeStepper Stepper = new TimeStepper(10, 5);
	public float startStep;
	public float span;
	//duration in seconds
	public float duration = 600;

	@Test
	public void SimulateStepper() throws InterruptedException, IOException
	{
	  ClearCLBackendInterface lClearCLBackendInterface = ClearCLBackends.getBestBackend();
	  try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
	  {
		  ClearCLDevice lFastestGPUDevice = lClearCL.getFastestGPUDeviceForImages();
		  System.out.println(lFastestGPUDevice);

		  ClearCLContext lContext = lFastestGPUDevice.createContext();

		  ClearCLProgram lProgram = lContext.createProgram(Demo.class, "CalcKernels.cl");
		  lProgram.addDefine("CONSTANT", "1");
		  lProgram.buildAndLog();
		  
		  float time=0;
		  
		  int lSize = 128;

		    ClearCLImage lImage1 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		    ClearCLImage lImage2 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		  
		  while (time<(duration*1000))  
		  {
			  float oldStep = Stepper.step;
			  Sim.generatePics(lContext, lProgram, time, lImage1, lImage2, lSize, oldStep);
			  float diff = Calc.compareImages(lContext, lProgram, lImage1, lImage2, lSize);
			  float step = Stepper.computeStep(diff);
		  
			  Thread.sleep((long) oldStep); 
		  
			  System.out.println(step);
		  }
	  }
	}
}
