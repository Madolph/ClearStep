package calculation;

import static java.lang.Math.pow;

import java.io.IOException;

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
	
	/**
	 * the program to be used for calculation-kernels
	 */
	ClearCLProgram calcProgram;
	
	/**
	 * the program to create kernels handling noise
	 */
	ClearCLProgram noiseCleaner;
	
	/**
	 * Kernels
	 */
	ClearCLKernel compare, clean, sum, convert;
	
	/**
	 * stores whether or not the calculator currently has two images stored
	 */
	public boolean filled = false;

	/**
	 * specifies the amount of blocks created in the kernel that sums up
	 * the computational image
	 */
	int mReductionFactor = 16;

	/**
	 * The Buffer used when summing up
	 */
	public ClearCLBuffer mEnd;
	
	/**
	 * The Context stored by the Calculator
	 */
	ClearCLContext mContext;

	/**
	 * constructs a Calculator
	 * 
	 * @param pContext	the ClearCLContext
	 * @param lCalc		the program for calculations
	 * @param lNoise		the program to handle noise
	 */
	public Calculator(ClearCLContext pContext, ClearCLProgram lCalc, ClearCLProgram lNoise) {
		calcProgram = lCalc;
		noiseCleaner = lNoise;
		createKernels();
		mContext = pContext;
		mEnd = mContext.createBuffer(NativeTypeEnum.Float, (int) pow(mReductionFactor, 3));
	}

	/**
	 * creates the kernels from the saved programs
	 */
	public void createKernels()
	{
		compare = calcProgram.createKernel("compare");
		clean = noiseCleaner.createKernel("cleanNoise");
		sum = calcProgram.createKernel("Sum3D");
		convert = calcProgram.createKernel("convert");
	}
	
	/** 
	 * saves an image to Cache and then compares two cached images, if possible
	 * @param lImage
	 * @param lProgram
	 * @param lSize
	 * @return
	 * @throws IOException 
	 */
	public float cacheAndCompare(ClearCLImage lImage, int lSize)
	{
		convert(lImage);
		float result = 0;
		CachePic(lSize);
		if (filled)
			{ result = compareImages(calcProgram, noiseCleaner, lSize); }
		return result;
	}
	
	/**
	 * receives an image and stores it into the Cache
	 * 
	 * @param image		The image received
	 * @param lContext	The OpenCL-Context
	 * @param lSize		The image-size to initialize an empty image if necessary
	 */
	public void CachePic(int lSize)
	{
		if (!even)
		{
			if (mImage1==null)
				// creates an empty picture if the cache is null
				{ mImage1 = mContext.createSingleChannelImage(mImage.getChannelDataType(), lSize, lSize, lSize); }
					
			mImage.copyTo(mImage1, true);
			even=true;
			System.out.println("image1 set");
		}
		else 
		{
			if (mImage2==null)
				// creates an empty picture if the cache is null
				{ mImage2 = mContext.createSingleChannelImage(mImage.getChannelDataType(), lSize, lSize, lSize); }
			
			mImage.copyTo(mImage2, true);
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
		/**
		if (mImage == null || mImage.getWidth() != lSize)
		{
			mImage =
					mContext.createSingleChannelImage(ImageChannelDataType.Float,
														lSize,
														lSize,
														lSize);
		}*/
		
		squareDiff();
	    
		boolean noise = true;
		if (noise)
			{ cleanNoise(); }
	    
	    // runs the kernel for summing up the "difference-Map" block-wise into an array
	    sumUpImageToBuffer();

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
	public void sumUpImageToBuffer()
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
	public void squareDiff()
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
	
	public void convert(ClearCLImage lImage)
	{
		System.out.println(lImage.isFloat());
		
		if (mImage == null)
		{
			mImage =
					mContext.createSingleChannelImage(ImageChannelDataType.Float,
													lImage.getDimensions());
		}
		
		if (!lImage.isFloat())
		{
			System.out.println("Converting image-data to float now");
			convert.setArgument("source", lImage);
			convert.setArgument("cache", mImage);
			convert.setGlobalSizes(lImage);
			convert.run(true);
		}
		else
		{
			System.out.println("Its already in float");
			lImage.copyTo(mImage, true);
		}
	}
}
