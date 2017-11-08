package demo;

import java.io.IOException;

import org.junit.Test;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import simulation.Simulator;

public class KernelTest {
	
	@Test
	public void TestSphere() throws InterruptedException, IOException
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
		  
		  int lSize = 128;
		  
		  ClearCLImage lImage = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		  
		  ClearCLKernel lKernel = lProgram.createKernel("sphere");
		  lKernel.setArgument("image", lImage);
		  lKernel.setGlobalSizes(lImage);
		  lKernel.setArgument("r", 0.25f);
		    
		  lKernel.setArgument("cx", lSize/2);
		  lKernel.setArgument("cy", lSize/2);
		  lKernel.setArgument("cz", lSize/2);
		    
		  lKernel.run(true);
		    
		  ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
		  
		  lViewImage.waitWhileShowing();
	  }
	}
}
