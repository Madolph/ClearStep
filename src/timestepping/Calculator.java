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

/**
 * This class can store two images and computes a metric for pixel-wise difference between the two
 * 
 * @author Raddock
 *
 */
public class Calculator {
	
	Setable mThres = new Setable();
	
	/**
	 * The first image that is stored by the calculator
	 */
	ClearCLImage mImage1=null;
	
	/**
	 * The second image that is stored by the calculator
	 */
	ClearCLImage mImage2=null;
	
	/**
	 * Is used to decide which cached picture will be overwritten
	 */
	boolean even = false;
	
	/**
	 * stores whether or not the calculator currently has two images stored
	 */
	boolean filled;

	int mReductionFactor = 16;

	ClearCLBuffer mEnd;
	ClearCLBuffer mBufferMin;
	ClearCLBuffer mBufferMax;
	ClearCLContext mContext;

	ClearCLImage mImage;

	public Calculator(ClearCLContext pContext) {
		mContext = pContext;

		mEnd = mContext.createBuffer(NativeTypeEnum.Float,
																							 (int) pow(mReductionFactor,
																												 3));
		mBufferMin = mContext.createBuffer(NativeTypeEnum.Float,
																										 (int) pow(mReductionFactor,
																															 3));
		mBufferMax = mContext.createBuffer(NativeTypeEnum.Float,
																										 (int) pow(mReductionFactor,
																															 3));
	}

	
	/**
	 * receives an image and stores it into the Cache
	 * 
	 * @param image		The image received
	 * @param lContext	The OpenCL-Context
	 * @param lSize		The image-size to initialize an empty image if necessary
	 */
	public void CachePic(ClearCLImage image, ClearCLContext lContext, int lSize)
	{
		if (!even)
		{
			if (mImage1==null)
				// creates an empty picture if the cache is null
				{ mImage1 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize); }
					
			image.copyTo(mImage1, false);
			even=true;
			System.out.println("image1 set");
		}
		else 
		{
			if (mImage2==null)
				// creates an empty picture if the cache is null
				{ mImage2 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize); }
			
			image.copyTo(mImage2, false);
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

		// creates a temporary image to store the pixel-wise difference
		if (mImage == null || mImage.getWidth() != lSize)
		{
			mImage =
					mContext.createSingleChannelImage(ImageChannelDataType.Float,
																						lSize,
																						lSize,
																						lSize);
		}

		
		// runs the kernel for image-comparison
	    /**String lString = new String();
	    if (!mThres.set)
	    {
	    	lString = "compare";
	    	mThres.set=true;
	    }
	    else 
	    	lString = "compareAndFilter";*/
	    if (!mThres.set)
	    { 	
	    	mThres.val=5;
	    	mThres.set=true;
	    }
	    ClearCLKernel lKernel = lProgram.createKernel("compareNFilter");
	    lKernel.setArgument("image1", mImage1);
	    lKernel.setArgument("image2", mImage2);
	    lKernel.setArgument("result", mImage);
	    lKernel.setArgument("threshold", mThres.val);
	    lKernel.setArgument("BufferMin", mBufferMin);
	    lKernel.setArgument("BufferMax", mBufferMax);
	    lKernel.setGlobalSizes(mReductionFactor, mReductionFactor, mReductionFactor);
	    lKernel.run(true);
	    
	    OffHeapMemory lBufferMin = OffHeapMemory.allocateFloats(mBufferMin.getLength());
	    OffHeapMemory lBufferMax = OffHeapMemory.allocateFloats(mBufferMax.getLength());
	    
	    mBufferMin.writeTo(lBufferMin, true);
	    float max = 0;
    	float min = 99999;
	    for (int i = 0; i < mBufferMin.getLength(); i++)
	    {
	    	float lFloatAligned = lBufferMin.getFloatAligned(i);
	    	if (lFloatAligned<min)
	    		min=lFloatAligned;
	    	if (lFloatAligned>max)
	    		max=lFloatAligned;
	    }
	    
	    mThres.val=min+((max-min)/10);
	    mThres.val=0;
		
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
		return lDiff;
	}
}
