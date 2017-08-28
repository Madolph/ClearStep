package timestepping;

import java.io.IOException;

import javax.swing.JFrame;

import org.junit.Test;
import org.math.plot.Plot2DPanel;

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
	public TimeStepper Stepper = new TimeStepper((float) 0.3, (float) 0.1, (float) 0.5, (float) 0.1);
	public float startStep;
	public float span;
	//duration in seconds
	public float duration = 20;
	
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
		  
		  // as long as we aren't above the time, we will now generate pictures and compute timesteps from them
		  while (time<(duration*1000))  
		  {
			  float currStep = Stepper.step;
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
				  float step = Stepper.computeStep(diff, currStep, time);
		  
				  // put the Thread to sleep to simulate realtime... kinda... sorta
		  
				  System.out.println("computed step is: "+step);
			  }
		  }
		  int Lx = Stepper.Info.devs.size();
		  int Ly = Stepper.Info.times.size();
			
		  double[] x = new double [Lx];
		  double[] y = new double [Ly];
			
		  for (int i=0;i<Lx;i++)
		  {
			  x[i]=(double)Stepper.Info.devs.get(i);
		  }
		  for (int i=0;i<Ly;i++)
		  {
			  y[i]=(double)Stepper.Info.times.get(i);
		  }
					 
		  // create your PlotPanel (you can use it as a JPanel)
		  Plot2DPanel plot = new Plot2DPanel();
					 
		  // add a line plot to the PlotPanel
		  plot.addLinePlot("my plot", x, y);
					 
		  // put the PlotPanel in a JFrame, as a JPanel
		  JFrame frame = new JFrame("a plot panel");
		  frame.setContentPane(plot);
		  frame.setVisible(true);
		  
		  lViewImage.waitWhileShowing();
	  }
	}
}
