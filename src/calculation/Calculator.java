package calculation;

import static java.lang.Math.pow;

import java.io.IOException;
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
	public ClearCLImage mImage;
	
	/**
	 * stores whether the last image received was an even or uneven capture
	 */
	boolean even = false;
	
	ClearCLProgram calcProgram;
	
	ClearCLProgram noiseCleaner;
	
	ClearCLKernel compare, clean, sum;
	
	/**
	 * stores whether or not the calculator currently has two images stored
	 */
	public boolean filled;

	int mReductionFactor = 16;

	ClearCLBuffer mEnd;
	ClearCLContext mContext;

	public Calculator(ClearCLContext pContext, ClearCLProgram Calc, ClearCLProgram Noise) {
		calcProgram = Calc;
		noiseCleaner = Noise;
		createKernels();
		mContext = pContext;
		mEnd = mContext.createBuffer(NativeTypeEnum.Float, (int) pow(mReductionFactor, 3));
	}

	public void createKernels()
	{
		compare = calcProgram.createKernel("compare");
		clean = noiseCleaner.createKernel("cleanNoise");
		//clean = noiseCleaner.createKernel("meanFilter");
		sum = calcProgram.createKernel("Sum3D");
	}
	
	/** 
	 * saves an image to Cache and then compares two cached images, if possible
	 * @param lImage
	 * @param lProgram
	 * @param lSize
	 * @return
	 * @throws IOException 
	 */
	public float cacheAndCompare(ClearCLImage lImage, ClearCLProgram lProgram, ClearCLProgram lProgram2, int lSize)
	{
		float result = 0;
		CachePic(lImage, mContext, lSize);
		result = compareImages(lProgram, lProgram2, lSize);
		return result;
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
			{ filled = true; }
	}
	
	/**
	 * compares two images and responds with a metric that
	 * relates to the change between the two
	 *
	 * @param calc 	The OpenCL-Program
	 * @param lSize 	The Size of the images
	 * @return 			The metric of change between the images
	 * @throws IOException 
	 */
	public float compareImages(ClearCLProgram calc, ClearCLProgram noiseClean, int lSize)
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
		
		squareDiff(calc);
	    
		boolean noise = true;
		if (noise)
			{ cleanNoise(); }
	    
	    // runs the kernel for summing up the "difference-Map" block-wise into an array
	    sumUpImageToBuffer(calc);

	    // fill Buffer
	    OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(mEnd.getLength());

	    // copy the array from the kernel to a buffer and sum everything up
	    float lSum = sumUpBuffer(lBuffer);	
	    lSum = (float) Math.sqrt(lSum);
	    System.out.println("Difference is: "+ lSum);
	    
		return lSum;
	}

	/**
	 * sums up the values of an image into a much smaller buffer
	 * @param lProgram
	 * @throws IOException 
	 */
	public void sumUpImageToBuffer(ClearCLProgram lProgram)
	{
	    sum.setArgument("image", mImage);
	    sum.setArgument("result", mEnd);
	    sum.setGlobalSizes(mReductionFactor, mReductionFactor, mReductionFactor);
	    sum.run(true);
	}
	
	/**
	 * cleans the result-image from noise (the older image is used as a cache
	 * and will be overwritten here)
	 * @param lProgram
	 */
	public void cleanNoise()
	{
		clean.setArgument("image1", mImage);
		if (even)
			{ clean.setArgument("cache", mImage2); }
		else
			{ clean.setArgument("cache", mImage1); }
		clean.setGlobalSizes(mImage);
		clean.run(true);
		
		//do another sweep so that mImage is the computational result again
		
		if (even)
		{ 	clean.setArgument("image1", mImage2); }
		else
		{ 	clean.setArgument("image1", mImage1); }
		clean.setArgument("cache", mImage);
		
		clean.setGlobalSizes(mImage);
		clean.run(true);
		
		
		/**
		if (even)
			{ mImage2.copyTo(mImage, true); }
		else
			{ mImage1.copyTo(mImage, true); } */
	}
	
	/**
	 * will calculate the square of the difference of every
	 * pixel between the two images
	 * @param lProgram
	 */
	public void squareDiff(ClearCLProgram lProgram)
	{
	    compare.setArgument("image1", mImage1);
	    compare.setArgument("image2", mImage2);
	    compare.setArgument("result", mImage);
	    compare.setGlobalSizes(mImage1);
	    compare.run(true);
	}
	
	/**
	 * sums up every value in a Buffer
	 * @param lBuffer
	 * @return the sum of the buffer
	 */
	public float sumUpBuffer(OffHeapMemory lBuffer) {
		mEnd.writeTo(lBuffer, true);
	    float lDiff = 0;
	    for (int i = 0; i < mEnd.getLength(); i++)
	    		{ lDiff += lBuffer.getFloatAligned(i); }
		return lDiff;
	}
}
