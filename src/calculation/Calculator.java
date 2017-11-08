package calculation;

import static java.lang.Math.pow;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

/**
 * This class can store two images and computes a metric for pixel-wise difference between the two
 * 
 * @author Raddock
 *
 */
public class Calculator {
	
	/**
	 * The first image that is stored by the calculator
	 */
	ClearCLImage mImage1=null;
	
	/**
	 * The second image that is stored by the calculator
	 */
	ClearCLImage mImage2=null;
	
	/**
	 * The "Image" that stores the values of calculation
	 */
	ClearCLImage mImage;
	
	/**
	 * Is used to decide which cached picture will be overwritten
	 */
	boolean even = false;
	
	/**
	 * stores whether or not the calculator currently has two images stored
	 */
	public boolean filled;

	int mReductionFactor = 16;

	ClearCLBuffer mEnd;
	ClearCLContext mContext;

	public Calculator(ClearCLContext pContext) {
		mContext = pContext;
		mEnd = mContext.createBuffer(NativeTypeEnum.Float, (int) pow(mReductionFactor, 3));
	}

	
	/**
	 * receives an image and stores it into the Cache
	 * 
	 * @param image		The image received
	 * @param lContext	The OpenCL-Context
	 * @param lSize		The image-size to initialize an empty image if necessary
	 */
	public void CachePic(ClearCLImage lImage, ClearCLContext lContext, int lSize)
	{
		if (!even)
		{
			if (mImage1==null)
				// creates an empty picture if the cache is null
				{ mImage1 = lContext.createSingleChannelImage(lImage.getChannelDataType(), lSize, lSize, lSize); }
					
			lImage.copyTo(mImage1, false);
			even=true;
			System.out.println("image1 set");
		}
		else 
		{
			if (mImage2==null)
				// creates an empty picture if the cache is null
				{ mImage2 = lContext.createSingleChannelImage(lImage.getChannelDataType(), lSize, lSize, lSize); }
			
			lImage.copyTo(mImage2, false);
			even=false;
			System.out.println("image2 set");
		}
		if (mImage1 != null && mImage2 !=null)
			filled = true;
	}
	
	/**
	 * compares two images and responds with a metric that
	 * relates to the change between the two
	 *
	 * @param lProgram 	The OpenCL-Program
	 * @param lSize 	The Size of the images
	 * @return 			The metric of change between the images
	 */
	public float compareImages(ClearCLProgram lProgram, int lSize)
	{

		if (!filled)
		{
			System.out.println("Calculator is not set up");
			return 0f;
		}
		// creates a temporary image to store the pixel-wise difference
		if (mImage == null || mImage.getWidth() != lSize)
		{
			mImage =
					mContext.createSingleChannelImage(mImage1.getChannelDataType(),
														lSize,
														lSize,
														lSize);
		}

	    ClearCLKernel lKernel = lProgram.createKernel("compare");
	    lKernel.setArgument("image1", mImage1);
	    lKernel.setArgument("image2", mImage2);
	    lKernel.setArgument("result", mImage);
	    lKernel.setGlobalSizes(mImage1);
	    lKernel.run(true);
	    
	    // runs the kernel for summing up the "difference-Map" block-wise into an array
	    ClearCLKernel lKernel1 = lProgram.createKernel("Sum3D");
	    lKernel1.setArgument("image", mImage);
	    lKernel1.setArgument("result", mEnd);
	    lKernel1.setGlobalSizes(mReductionFactor, mReductionFactor, mReductionFactor);
	    lKernel1.run(true);

	    OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(mEnd.getLength());

	    // copy the array from the kernel to a buffer and sum everything up
	    mEnd.writeTo(lBuffer, true);
	    float lDiff = 0;
	    for (int i = 0; i < mEnd.getLength(); i++)
	    {
	    	float lFloatAligned = lBuffer.getFloatAligned(i);
	    	lDiff += lFloatAligned;
	    }	
	    lDiff = (float) Math.sqrt(lDiff);
	    System.out.println("Difference is: "+ lDiff);
	    
		return lDiff;
	}
}
