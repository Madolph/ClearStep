package Kernels;

import java.io.IOException;


import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import coremem.offheap.OffHeapMemory;
import framework.Handler;
import org.junit.Test;
import simulation.Simulator;

public class KernelTest {
	
	@Test
	public void TestConvert() throws InterruptedException, IOException
	{	
		Handler lHandler = new Handler(null, ImageChannelDataType.UnsignedInt16);
		
		int lSize = 128;
		
		Simulator lSim = new Simulator(ImageChannelDataType.UnsignedInt16, lHandler.mContext);
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, lSize, lSize, lSize);
		ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage, "converted to float");
		
		lSim.generatePic(0, lImage, lSize, true);
		lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
		
		Thread.sleep(5000);
		
		lHandler.mCalc.convert(lImage);
		lViewImage.setImage(lHandler.mCalc.mImage);
		lHandler.mCalc.mImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
		
		
		lHandler.mCalc.sumUpImageToBuffer();
		
		OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(lHandler.mCalc.mEnd.getLength());
		
		float Sum = lHandler.mCalc.sumUpBuffer(lBuffer);
		
		System.out.println("Sum of converted image is: "+Sum);
		
		Simulator lSim2 = new Simulator(ImageChannelDataType.Float, lHandler.mContext);
		ClearCLImage lImage2 = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize);
		@SuppressWarnings("unused")
		ClearCLImageViewer lViewImage2 = ClearCLImageViewer.view(lImage2, "straight to float");
		lImage2.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
		
		lSim2.generatePic(0, lImage2, lSize, true);
		
		lViewImage.waitWhileShowing();
	}
	
	
}
