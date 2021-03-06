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
	
	/**
	 * created to assembe a String for a simple data-output
	 */
	public String mValues1="uncleaned: ";
	
	/**
	 * created to assembe a String for a simple data-output
	 */
	public String mValues2="cleaned: ";
	
	/**
	 * Image Slot 1 for caching of fused images
	 */
	public ClearCLImage mImage1=null;
	
	/**
	 * Image Slot 2 for caching of fused images
	 */
	public ClearCLImage mImage2=null;
	
	/**
	 * Image slot mainly used for computational purpose
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
	 * Kernels used by the calculator
	 */
	ClearCLKernel compare, clean, sum, convert;
	
	/**
	 * stores whether or not the calculator currently has two images stored
	 */
	public boolean filled = false;

	/**
	 * specifies the amount of blocks created in the kernel that sums up our image into a buffer
	 */
	int mReductionFactor = 16;

	/**
	 * The Buffer used when summing up
	 */
	public ClearCLBuffer mEnd;
	
	/**
	 * The Context stored by the calculator
	 */
	ClearCLContext mContext;
	
	/**
	 * The final result computed by the calculator
	 */
	public float mResult;

	/**
	 * constructs the Calculator
	 * 
	 * @param pContext the ClearCLContext ClearStep runs on
	 * @param lCalc the program for calculations
	 * @param lNoise the program to handle noise
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
	 * receives an image, converts it to float and cleans noise before caching it and
	 * running calculations
	 * 
	 * @param lImage the image to be processed
	 * @return the numeric result of the image comparison
	 */
	public float cacheAndCompare(ClearCLImage lImage)
	{
		convert(lImage);
		float result = 0;
		cachePic();
		
		boolean noise = true;
		if (noise)
			{ cleanNoise(1); }
		
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
	 * moves the converted image into the designated cache
	 */
	public void cachePic()
	{
		//determine the right cache slot
		if (!even)
		{
			if (mImage1 == null)
			{
				mImage1 = mContext.createSingleChannelImage(mImage.getChannelDataType(), mImage.getDimensions());
			}
					
			mImage.copyTo(mImage1, true);
			even=true;
			System.out.println("image1 set");
		}
		else 
		{
			if (mImage2 == null)
			{
				mImage2 = mContext.createSingleChannelImage(mImage.getChannelDataType(), mImage.getDimensions());
			}
			
			mImage.copyTo(mImage2, true);
			even=false;
			System.out.println("image2 set");
		}
		
		//check if the cache is set for computation
		if (mImage1 != null && mImage2 !=null)
			{ filled = true; }
	}
	
	
	/**
	 * compares two images and responds with a metric that
	 * relates to the change between the two
	 */
	public float compareImages()
	{
		squareDiff();
		
	    sumUpImageToBuffer();
	    
	    // fill Buffer
	    OffHeapMemory lBuffer2 = OffHeapMemory.allocateFloats(mEnd.getLength());

	    // copy the array from the kernel to a buffer and sum everything up
	    float lSum2 = sumUpBuffer(lBuffer2);
	    
	    lSum2 = (float) Math.sqrt(lSum2);
	    
	    //mValues2=mValues2+lSum2+" ";
	    
		return lSum2;
	}

	/**
	 * sums up the values of an image into a much smaller buffer
	 * @throws IOException 
	 */
	public void sumUpImageToBuffer()
	{
		System.out.println("setting up the SumUp kernel");
		
		//set images for the kernel
	    sum.setArgument("image", mImage);
	    sum.setArgument("result", mEnd);
	    
	    //the actual dimensions
	    long[] realDim=mImage.getDimensions();
	    
	    //new dimensions that are compatible with the reduction factor
	    int[] blockDim=new int[realDim.length];
	    
	    //computes the new dimensions
	    for (int i=0;i<realDim.length;i++)
	    {
	    	if (realDim[i]%mReductionFactor!=0)
	    	{
	    		blockDim[i]= ((int) realDim[i]/mReductionFactor)+1;
	    	}
	    	else
	    	{
	    		blockDim[i]= (int) realDim[i]/mReductionFactor;
	    	}
	    }
	    
	    //set parameters for the kernel
	    sum.setArgument("blockWidthX", blockDim[0]);
	    sum.setArgument("blockWidthY", blockDim[1]);
	    sum.setArgument("blockWidthZ", blockDim[2]);
	    sum.setGlobalSizes(mReductionFactor, mReductionFactor, mReductionFactor);
	    
	    System.out.println("running the SumUp kernel");
	    sum.run(true);
	}
	
	/**
	 * cleans the result-image from noise (the older image is used as a cache
	 * and will be overwritten here)
	 * @param sweeps the amount of noise cleaning sweeps to be applied (performs even counts)
	 */
	public void cleanNoise(int sweeps)
	{
		int i = 0;
		while (i < sweeps)
		{
			if (even)
			{ clean.setArgument("image1", mImage1); }
			else
			{ clean.setArgument("image1", mImage2); }
			
			clean.setArgument("cache", mImage);
			
			clean.setGlobalSizes(mImage);
			clean.run(true);
			
			clean.setArgument("image1", mImage);
			
			if (even)
			{ clean.setArgument("cache", mImage1); }
			else
			{ clean.setArgument("cache", mImage2); }
			
			clean.setGlobalSizes(mImage);
			clean.run(true);
			
			// this method always applies even sweep numbers
			i += 2;
		}
	}
	
	/**
	 * will calculate the square of the difference of every
	 * pixel between the two images and store that information in another image
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
	 * sums up every value of an image in a Buffer
	 * @param lBuffer
	 * @return the sum of the buffer
	 */
	public float sumUpBuffer(OffHeapMemory lBuffer) 
	{
		mEnd.writeTo(lBuffer, true);
	    float lDiff = 0;
	    for (int i = 0; i < mEnd.getLength(); i++)
	    { 
	    	lDiff += lBuffer.getFloatAligned(i); 
	    }
		return lDiff;
	}
	
	/**
	 * converts an image received into float and saves it into a local image slot
	 * 
	 * @param lImage the image to be processed
	 */
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
