package timestepping;

import static java.lang.Math.pow;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

public class Calculator {
	
	ClearCLImage image1=null;
	ClearCLImage image2=null;
	boolean uneven = true;
	boolean filled;

	public void CachePic(ClearCLImage image)
	{
		if (uneven)
		{
			image1=image;
			uneven=false;
			System.out.println("image1 set");
		}
		else 
		{
			image2=image;
			uneven=true;
			System.out.println("image2 set");
		}
		if (image1 != null && image2 !=null)
			filled = true;
	}
	
	/**
	 * compares two images and responds with a metric that
	 * relates to the change between the two
	 * 
	 * @param lContext the OpenCL-Context
	 * @param lProgram the OpenCL-Program
	 * @param lImage1 Image Number 1
	 * @param lImage2 Image Number 2
	 * @param lSize the Size of the images
	 * @return the metric of change between the images
	 */
	public float compareImages(ClearCLContext lContext, ClearCLProgram lProgram, int lSize)
	{
		ClearCLImage lImage3 = lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                   lSize,
                                                   lSize,
                                                   lSize);
		
		int lReductionFactor = 16;

	      ClearCLBuffer lEnd = lContext.createBuffer(NativeTypeEnum.Float,
	                                                 (int) pow(lReductionFactor,
	                                                           3));
		
		float lthres = 0;
	    ClearCLKernel lKernel = lProgram.createKernel("compare");
	    lKernel.setArgument("image1", image1);
	    lKernel.setArgument("image2", image2);
	    lKernel.setArgument("result", lImage3);
	    lKernel.setArgument("threshold", lthres);
	    lKernel.setGlobalSizes(image1);
	    lKernel.run(true);
		
	    ClearCLKernel lKernel1 = lProgram.createKernel("Sum3D");
	    lKernel1.setArgument("image", lImage3);
	    lKernel1.setArgument("result", lEnd);
	    lKernel1.setGlobalSizes(lReductionFactor,
	                              lReductionFactor,
	                              lReductionFactor);
	    lKernel1.run(true);

	    OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(lEnd.getLength());

	    lEnd.writeTo(lBuffer, true);
	    float diff = 0;
	    for (int i = 0; i < lEnd.getLength(); i++)
	    {
	    	float lFloatAligned = lBuffer.getFloatAligned(i);
	    	diff += lFloatAligned;
	    }
	    // print out the end-result
	    System.out.println("result is " + diff);
		
		return diff;
	}
}
