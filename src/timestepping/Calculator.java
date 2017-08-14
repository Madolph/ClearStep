package timestepping;

import static java.lang.Math.pow;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.enums.ImageChannelDataType;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

public class Calculator {

	public float compareImages(ClearCLContext lContext, ClearCLProgram lProgram, 
			ClearCLImage lImage1, ClearCLImage lImage2, int lSize)
	{
		ClearCLImage lImage3 =
                lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                   lSize,
                                                   lSize,
                                                   lSize);
		
		int lReductionFactor = 16;

	      ClearCLBuffer lEnd = lContext.createBuffer(NativeTypeEnum.Float,
	                                                 (int) pow(lReductionFactor,
	                                                           3));
		
		float lthres = 0;
	    ClearCLKernel lKernel = lProgram.createKernel("compare");
	    lKernel.setArgument("image1", lImage1);
	    lKernel.setArgument("image2", lImage2);
	    lKernel.setArgument("result", lImage3);
	    lKernel.setArgument("threshold", lthres);
	    lKernel.setGlobalSizes(lImage1);
	    lKernel.run(true);
		
	    ClearCLKernel lKernel1 = lProgram.createKernel("Sum3D");
	    lKernel1.setArgument("image", lImage1);
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
	    // print out the end-result (in this case should be 0)
	    System.out.println("result is " + diff);
		
		return diff;
	}
}
