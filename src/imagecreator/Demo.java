package imagecreator;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.io.IOException;

import clearcl.ClearCL;
import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

public class Demo
{
	float[] changes = new float[10];

  public void CompareDemoImages() throws InterruptedException, IOException
  {
	 System.out.println("doing stuff");
	 float threshold = 0;
	 int shiftx = 2;
	 int shifty = 0;
	 int shiftz = 0;
	 changes[0] = createDummyPics(0, 0, 0,
			 					  shiftx, shifty, shiftz,
			 					  threshold, true);
	 int posx = shiftx;
	 int posy = shifty;
	 int posz = shiftz;
	 
	 for (int i=1;i<changes.length;i++)
	 {
		 changes[i] = createDummyPics(posx, posy, posz,
			 					  	  shiftx, shifty, shiftz,
			 					  	  threshold, false);
		 posx = posx + shiftx;
		 posy = posy + shifty;
		 posz = posz + shiftz;
	 }
	 
  }
	
	
  public float createDummyPics(int initialshiftx, int initialshifty, int initialshiftz, 
		  					  int movementx, int movementy, int movementz,
		  					  float extthreshold, boolean first) 
		  							  throws InterruptedException, IOException
  {

    ClearCLBackendInterface lClearCLBackendInterface =
                                                     ClearCLBackends.getBestBackend();
    try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
    {
      ClearCLDevice lFastestGPUDevice =
                                      lClearCL.getFastestGPUDeviceForImages();

      System.out.println(lFastestGPUDevice);

      ClearCLContext lContext = lFastestGPUDevice.createContext();

      System.out.println("creating program");

      ClearCLProgram lprogram = lContext.createProgram(Demo.class,
                                                       "Maxdemo.cl");

      lprogram.addDefine("CONSTANT", "1");
      lprogram.buildAndLog();

      System.out.println("created program");

      int lSize = 128;

      ClearCLImage lImage1 =
                           lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                             lSize,
                                                             lSize,
                                                             lSize);

      ClearCLImage lImage2 =
                           lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                             lSize,
                                                             lSize,
                                                             lSize);

      ClearCLImage lResult =
                          lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                             lSize,
                                                             lSize,
                                                             lSize);
      
      ClearCLImage lDenoisedResult =
              			  lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                			 lSize,
                                                			 lSize,
                                                			 lSize);

      int lReductionFactor = 16;

      ClearCLBuffer lEnd = lContext.createBuffer(NativeTypeEnum.Float,
                                                 (int) pow(lReductionFactor,
                                                           3));


      System.out.println("created images");

      // give the first image to the kernel
      ClearCLKernel lKernel = lprogram.createKernel("xorsphere");
      lKernel.setArgument("image", lImage1);
      lKernel.setGlobalSizes(lImage1);

      // set all the constants
      lKernel.setOptionalArgument("r", 0.25f);
      lKernel.setOptionalArgument("cx", lSize / 2 + initialshiftx);
      lKernel.setOptionalArgument("cy", lSize / 2 + initialshifty);
      lKernel.setOptionalArgument("cz", lSize / 2 + initialshiftz);

      lKernel.setOptionalArgument("a", 1);
      lKernel.setOptionalArgument("b", 1);
      lKernel.setOptionalArgument("c", 1);
      lKernel.setOptionalArgument("d", 1);

      lKernel.run(true);

      System.out.println("first image done");

      // give the second image to the kernel but leave everything as is
      lKernel.setArgument("image", lImage2);
      lKernel.setOptionalArgument("cx", lSize / 2 + initialshiftx + movementx);
      lKernel.setOptionalArgument("cy", lSize / 2 + initialshifty + movementy);
      lKernel.setOptionalArgument("cz", lSize / 2 + initialshiftz + movementz);
      lKernel.setGlobalSizes(lImage2);
      lKernel.run(true);

      System.out.println("second image done");

      // calculate pixel-difference and save it to another image
      float lthres = 0;
      ClearCLKernel lKernel2 = lprogram.createKernel("compare");
      lKernel2.setArgument("image1", lImage1);
      lKernel2.setArgument("image2", lImage2);
      lKernel2.setArgument("result", lResult);
      lKernel2.setArgument("threshold", lthres);
      lKernel2.setGlobalSizes(lImage1);
      lKernel2.run(true);
      
      ClearCLKernel lKernel3 = lprogram.createKernel("handleNoise");
      lKernel3.setArgument("image", lResult);
      lKernel3.setArgument("clean", lDenoisedResult);
      if (first = true)
    	  lKernel3.setArgument("thresh", lthres);
      else 
    	  lKernel3.setArgument("thresh", extthreshold);
      lKernel3.setGlobalSizes(lResult);
      lKernel3.run(true);

      // while (lViewImageResult.isShowing())
      // {
      // Thread.sleep(10);
      // }

      // take the difference-map and calculate the root of the sum
      ClearCLKernel lKernel5 = lprogram.createKernel("Sum3D");
      lKernel5.setArgument("image", lDenoisedResult);
      lKernel5.setArgument("result", lEnd);
      lKernel5.setGlobalSizes(lReductionFactor,
                              lReductionFactor,
                              lReductionFactor);
      lKernel5.run(true);

      OffHeapMemory lBuffer =
                            OffHeapMemory.allocateFloats(lEnd.getLength());

      lEnd.writeTo(lBuffer, true);

      float lSum = 0;

      for (int i = 0; i < lEnd.getLength(); i++)
      {
        float lFloatAligned = lBuffer.getFloatAligned(i);
        System.out.println(lFloatAligned);
        lSum += lFloatAligned;
      }

      lSum = (float) sqrt(lSum);

      // print out the end-result (in this case should be 0)
      System.out.println("result is " + lSum);
      
      return lSum;
    }

  }

}
