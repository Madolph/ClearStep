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
public class Calculator	implements
						CalculatorInterface
{
	public String mValues1="uncleaned: ";
	
	public String mValues2="cleaned: ";
	
	/**
	 * The first image that is stored by the calculator
	 */
	public ClearCLImage mImage1=null;
	
	/**
	 * The second image that is stored by the calculator
	 */
	public ClearCLImage mImage2=null;
	
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
	
	public float mResult;

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
	public float cacheAndCompare(ClearCLImage lImage)
	{
		convert(lImage);
		float result = 0;
		cachePic();
		if (filled)
		{ 
			System.out.println("Calc is filled");
			result = compareImages(); 
			System.out.println("images are compared");
		}
		
		mResult = result;
		
		return mResult;
	}
	
	/**
	 * receives an image and stores it into the Cache
	 * 
	 * @param image		The image received
	 * @param lContext	The OpenCL-Context
	 * @param lSize		The image-size to initialize an empty image if necessary
	 */
	public void cachePic()
	{
		if (!even)
		{
			if (mImage1==null)
				// creates an empty picture if the cache is null
				{ mImage1 = mContext.createSingleChannelImage(mImage.getChannelDataType(), mImage.getDimensions()); }
					
			mImage.copyTo(mImage1, true);
			even=true;
			System.out.println("image1 set");
		}
		else 
		{
			if (mImage2==null)
				// creates an empty picture if the cache is null
				{ mImage2 = mContext.createSingleChannelImage(mImage.getChannelDataType(), mImage.getDimensions()); }
			
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
	public float compareImages()
	{
		squareDiff();
		sumUpImageToBuffer();
	    
	    // fill Buffer
	    OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(mEnd.getLength());

	    // copy the array from the kernel to a buffer and sum everything up
	    float lSum1 = sumUpBuffer(lBuffer);
	    
	    lSum1 = (float) Math.sqrt(lSum1);
		
		mValues1=mValues1+lSum1+" ";
		
		
		boolean noise = true;
		if (noise)
			{ cleanNoise(1); }
		
	    // runs the kernel for summing up the "difference-Map" block-wise into an array
	    sumUpImageToBuffer();
	    
	    // fill Buffer
	    lBuffer = OffHeapMemory.allocateFloats(mEnd.getLength());

	    // copy the array from the kernel to a buffer and sum everything up
	    float lSum2 = sumUpBuffer(lBuffer);
	    
	    lSum2 = (float) Math.sqrt(lSum2);
	    
	    mValues2=mValues2+lSum2+" ";
	    
		return lSum2;
	}

	/**
	 * sums up the values of an image into a much smaller buffer
	 * @param lProgram
	 * @throws IOException 
	 */
	public void sumUpImageToBuffer()
	{
		System.out.println("setting up the SumUp kernel");
	    sum.setArgument("image", mImage);
	    sum.setArgument("result", mEnd);
	    sum.setGlobalSizes(mReductionFactor, mReductionFactor, mReductionFactor);
	    System.out.println("running the SumUp kernel");
	    sum.run(true);
	}
	
	/**
	 * cleans the result-image from noise (the older image is used as a cache
	 * and will be overwritten here)
	 * @param lProgram
	 */
	public void cleanNoise(int sweeps)
	{
		int i = 0;
		while (i < sweeps)
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
			{ 	mImage2.copyTo(mImage, true); }
			else
			{ 	mImage1.copyTo(mImage, true); }
			
			
			/*if (even)
			{ 	clean.setArgument("image1", mImage2); }
			else
			{ 	clean.setArgument("image1", mImage1); }
			clean.setArgument("cache", mImage);
			
			clean.setGlobalSizes(mImage);
			clean.run(true);*/
			
			i += 1;
		}
	}
	
	/**
	 * will calculate the square of the difference of every
	 * pixel between the two images
	 * @param lProgram
	 */
	public void squareDiff()
	{
		System.out.println("--> setting arguments");
	    compare.setArgument("image1", mImage1);
	    compare.setArgument("image2", mImage2);
	    compare.setArgument("result", mImage);
	    compare.setGlobalSizes(mImage1);
	    System.out.println("--> running compare");
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
