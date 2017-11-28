package Kernels;

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
import fastfuse.FastFusionEngine;
import fastfuse.FastFusionMemoryPool;
import fastfuse.stackgen.StackGenerator;
import fastfuse.tasks.AverageTask;
import fastfuse.tasks.MemoryReleaseTask;
import framework.Handler;
import simbryo.synthoscopy.microscope.lightsheet.drosophila.LightSheetMicroscopeSimulatorDrosophila;

public class KernelTest {
	
	@Test
	public void testSphere() throws InterruptedException, IOException
	{
	// create the Overhead for the actual Test
	  ClearCLBackendInterface lClearCLBackendInterface = ClearCLBackends.getBestBackend();
	  try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
	  {
		  ClearCLDevice lFastestGPUDevice = lClearCL.getFastestGPUDeviceForImages();
		  System.out.println(lFastestGPUDevice);

		  ClearCLContext lContext = lFastestGPUDevice.createContext();

		  ClearCLProgram lProgram = lContext.createProgram(KernelTest.class, "CalcKernels.cl");
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
	
	@Test
	public void testNoiseCleaner() throws Exception
	{
		Handler lHandler = new Handler(null);
		
		lHandler.mProgram1 = lHandler.mContext.createProgram(KernelTest.class, "Calculator.cl");
		lHandler.mProgram1.addDefine("CONSTANT", "1");
		lHandler.mProgram1.addDefine("READ_IMAGE", "read_imageui");
		lHandler.mProgram1.addDefine("WRITE_IMAGE", "write_imageui");
		lHandler.mProgram1.buildAndLog();
			  
		// now that this is done, we initialize the time and create two images that will
		// be filled by the simulator during the run	  
		int lSize = 128;
			  
		int lPhantomWidth = lSize;
		int lPhantomHeight = lPhantomWidth;
		int lPhantomDepth = lPhantomWidth;
		      
		//hier drigend auf Datentyp achten
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, 
																lSize, lSize, lSize);
		
		ClearCLImage lImage2 = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, 
				lSize, lSize, lSize);
			  
		ClearCLImageViewer lView = ClearCLImageViewer.view(lImage);
		
		@SuppressWarnings("unused")
		ClearCLImageViewer lView2 = ClearCLImageViewer.view(lImage2);
		
		int lNumberOfDetectionArms = 2;
		int lNumberOfIlluminationArms = 4;
		int lMaxCameraResolution = lSize;
			  
		LightSheetMicroscopeSimulatorDrosophila lSimulator =
                      new LightSheetMicroscopeSimulatorDrosophila(lHandler.mContext,
                                                             		lNumberOfDetectionArms,
                                                             		lNumberOfIlluminationArms,
                                                             		lMaxCameraResolution,
                                                             		0f,
                                                             		lPhantomWidth,
                                                             		lPhantomHeight,
                                                             		lPhantomDepth);
		
		System.out.println("DrosoSim is done");
			  
		StackGenerator lStackGenerator = new StackGenerator(lSimulator);
		
		FastFusionEngine lFastFusionEngine = new FastFusionEngine(lHandler.mContext);
		
		@SuppressWarnings("unused")
		FastFusionMemoryPool lMemoryPool = FastFusionMemoryPool.getInstance(lHandler.mContext,
                                                 							100 * 1024 * 1024, true);
		    
		String[][] Tags = new String[lNumberOfDetectionArms][lNumberOfIlluminationArms];
		
		for (int c=0;c<lNumberOfDetectionArms;c++)
			for (int i=0;i<lNumberOfIlluminationArms;i++)
			{
				Tags[c][i]=(String)("C"+c+"L"+i);
			}
		
		for (int c=0;c<lNumberOfDetectionArms;c++)
			{
				lFastFusionEngine.addTask(new AverageTask(  Tags[c][0], Tags[c][1],
															Tags[c][2], Tags[c][3],
															(String)("C"+c) )       );
			}
			
		
		lFastFusionEngine.addTask(new AverageTask("C0", "C1", "fused"));
		lFastFusionEngine.addTask(new MemoryReleaseTask("fused","C0L0",
														"C0L1","C0L2",
														"C0L3","C0",
														"C1L0","C1L1",
														"C1L2","C1L3",
														"C1"));
		
		lStackGenerator.setCenteredROI(lSize, lSize);

		lStackGenerator.setLightSheetHeight(500f);
		lStackGenerator.setLightSheetIntensity(10f);

		int blubb = 0;
		while (blubb<1)
		{
			for (int c = 0; c < lNumberOfDetectionArms; c++)
				for (int l = 0; l < lNumberOfIlluminationArms; l++)
			    {
					String lKey = String.format("C%dL%d", c, l);
	
					System.out.print("now generating stacks");
					
			        lStackGenerator.generateStack(c, l, -lSize/2f, lSize/2f, lSize);
	
			        lFastFusionEngine.passImage(lKey,lStackGenerator.getStack());
			    }
			
			lFastFusionEngine.executeAllTasks();
			
			lFastFusionEngine.getImage("fused").copyTo(lImage, true);
			
			lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
			
			blubb ++;
		}
		ClearCLKernel lKernel = lHandler.mProgram1.createKernel("cleanNoise");
		lKernel.setArgument("image1", lImage);
		lKernel.setArgument("cache", lImage2);
		lKernel.setGlobalSizes(lImage);
		lKernel.run(true);
		
		lImage2.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
		
		lView.waitWhileShowing();
		lSimulator.close();
		lStackGenerator.close();
	}
	
	@Test
	public void testMeanCleaner() throws Exception
	{
		Handler lHandler = new Handler(null);
		
		lHandler.mProgram1 = lHandler.mContext.createProgram(KernelTest.class, "Calculator.cl");
		lHandler.mProgram1.addDefine("CONSTANT", "1");
		lHandler.mProgram1.addDefine("READ_IMAGE", "read_imageui");
		lHandler.mProgram1.addDefine("WRITE_IMAGE", "write_imageui");
		lHandler.mProgram1.addDefine("Readvector", "uint4");
		lHandler.mProgram1.buildAndLog();
			  
		// now that this is done, we initialize the time and create two images that will
		// be filled by the simulator during the run	  
		int lSize = 128;
			  
		int lPhantomWidth = lSize;
		int lPhantomHeight = lPhantomWidth;
		int lPhantomDepth = lPhantomWidth;
		      
		//hier drigend auf Datentyp achten
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, 
																lSize, lSize, lSize);
		
		ClearCLImage lImage2 = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, 
				lSize, lSize, lSize);
			  
		ClearCLImageViewer lView = ClearCLImageViewer.view(lImage);
		
		@SuppressWarnings("unused")
		ClearCLImageViewer lView2 = ClearCLImageViewer.view(lImage2);
		
		int lNumberOfDetectionArms = 2;
		int lNumberOfIlluminationArms = 4;
		int lMaxCameraResolution = lSize;
			  
		LightSheetMicroscopeSimulatorDrosophila lSimulator =
                      new LightSheetMicroscopeSimulatorDrosophila(lHandler.mContext,
                                                             		lNumberOfDetectionArms,
                                                             		lNumberOfIlluminationArms,
                                                             		lMaxCameraResolution,
                                                             		0f,
                                                             		lPhantomWidth,
                                                             		lPhantomHeight,
                                                             		lPhantomDepth);
		
		System.out.println("DrosoSim is done");
			  
		StackGenerator lStackGenerator = new StackGenerator(lSimulator);
		
		FastFusionEngine lFastFusionEngine = new FastFusionEngine(lHandler.mContext);
		
		@SuppressWarnings("unused")
		FastFusionMemoryPool lMemoryPool = FastFusionMemoryPool.getInstance(lHandler.mContext,
                                                 							100 * 1024 * 1024, true);
		    
		String[][] Tags = new String[lNumberOfDetectionArms][lNumberOfIlluminationArms];
		
		for (int c=0;c<lNumberOfDetectionArms;c++)
			for (int i=0;i<lNumberOfIlluminationArms;i++)
			{
				Tags[c][i]=(String)("C"+c+"L"+i);
			}
		
		for (int c=0;c<lNumberOfDetectionArms;c++)
			{
				lFastFusionEngine.addTask(new AverageTask(  Tags[c][0], Tags[c][1],
															Tags[c][2], Tags[c][3],
															(String)("C"+c) )       );
			}
			
		
		lFastFusionEngine.addTask(new AverageTask("C0", "C1", "fused"));
		lFastFusionEngine.addTask(new MemoryReleaseTask("fused","C0L0",
														"C0L1","C0L2",
														"C0L3","C0",
														"C1L0","C1L1",
														"C1L2","C1L3",
														"C1"));
		
		lStackGenerator.setCenteredROI(lSize, lSize);

		lStackGenerator.setLightSheetHeight(500f);
		lStackGenerator.setLightSheetIntensity(10f);

		int blubb = 0;
		while (blubb<1)
		{
			for (int c = 0; c < lNumberOfDetectionArms; c++)
				for (int l = 0; l < lNumberOfIlluminationArms; l++)
			    {
					String lKey = String.format("C%dL%d", c, l);
	
					System.out.print("now generating stacks");
					
			        lStackGenerator.generateStack(c, l, -lSize/2f, lSize/2f, lSize);
	
			        lFastFusionEngine.passImage(lKey,lStackGenerator.getStack());
			    }
			
			lFastFusionEngine.executeAllTasks();
			
			lFastFusionEngine.getImage("fused").copyTo(lImage, true);
			
			lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
			
			blubb ++;
		}
		ClearCLKernel lKernel = lHandler.mProgram1.createKernel("meanFilter");
		lKernel.setArgument("image1", lImage);
		lKernel.setArgument("cache", lImage2);
		lKernel.setGlobalSizes(lImage);
		lKernel.run(true);
		
		lImage2.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
		
		lView.waitWhileShowing();
		lSimulator.close();
		lStackGenerator.close();
	}
	
}
