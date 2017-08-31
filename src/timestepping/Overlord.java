package timestepping;

import java.io.IOException;

import org.junit.Test;

import com.sun.javafx.application.PlatformImpl;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;

public class Overlord {
	
	public Simulator Sim = new Simulator();
	public Calculator Calc = new Calculator();
	public TimeStepper mTimeStepper = new TimeStepper((float) 0.3, (float) 0.1, (float) 0.5, (float) 0.1);
	public float startStep;
	public float span;
	//duration in seconds
	public float duration = 20;
	boolean fxOn = false;
	
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
		  ClearCLImage lImage = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		  
		  ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
		  
		  Plotter Graph = new Plotter();
		  Graph.plot(fxOn);
		  
		  // as long as we aren't above the time, we will now generate pictures and compute timesteps from them
		  while (time<(duration*1000))  
		  {
			  float currStep = mTimeStepper.step;
			  System.out.println("current time is: "+time);
			  
			  Sim.generatePic(lContext, lProgram, time, lImage, lSize);
			  lImage.notifyListenersOfChange(lContext.getDefaultQueue());
			  
			  Thread.sleep((long) currStep);
			  time += currStep;
			  Calc.CachePic(lImage, lContext, lSize);
			  if (Calc.filled)
			  {			  
				  // computes the difference between the two pictures
				  float diff = Calc.compareImages(lContext, lProgram, lSize);
				  System.out.println("diff is: "+diff);
				  // computed the step out of the saved difference
				  float step = mTimeStepper.computeStep(diff, currStep);
		  
				  // put the Thread to sleep to simulate realtime... kinda... sorta
		  
				  System.out.println("computed step is: "+step);
				  
				  Graph.plotdata(time, diff);
			  }
		  }
		  /**int Lx = Stepper.Info.devs.size();
		  int Ly = Stepper.Info.times.size();
			
		  float[] x = new float [Lx];
		  float[] y = new float [Ly];
			
		  for (int i=0;i<Lx;i++)
		  {
			  x[i]=Stepper.Info.devs.get(i);
		  }
		  for (int i=0;i<Ly;i++)
		  {
			  y[i]=Stepper.Info.times.get(i);
		  }**/
		  
		  lViewImage.waitWhileShowing();
	  }
	}
}
