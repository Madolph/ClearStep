package toolset;

import java.io.IOException;

import Kernels.KernelDemo;
import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

public class CenterDetector {
	
	ClearCLContext mContext;
	
	ClearCLKernel mScanX, mScanY, mScanZ;
	
	ClearCLProgram mScanPlane;
	
	public ClearCLBuffer mArrayX, mArrayY, mArrayZ;

	public CenterDetector(ClearCLContext context) throws IOException
	{
		mContext = context;
		mScanPlane = mContext.createProgram(KernelDemo.class, "Center.cl");
		mScanPlane.buildAndLog();
		
		mScanX = mScanPlane.createKernel("ScanPlaneX");
		mScanY = mScanPlane.createKernel("ScanPlaneY");
		mScanZ = mScanPlane.createKernel("ScanPlaneZ");
	}
	
	public float[] detectCenter(ClearCLImage image)
	{
		float[] center = new float[3];
		
		center[0]=getCenterX(image);
		center[1]=getCenterY(image);
		center[2]=getCenterZ(image);
		
		return center;
	}
	
	public float getCenter(ClearCLBuffer array)
	{
		float position = 0;
		
		OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(array.getLength());
		array.writeTo(lBuffer, true);
		
	    float totalWeight = 0;
	    float coordWeight = 0;
	    for (int i = 0; i < array.getLength(); i++)
	    { 
	    	totalWeight += lBuffer.getFloatAligned(i);
	    	coordWeight += (lBuffer.getFloatAligned(i)*i);
	    }
	    position= coordWeight/totalWeight;
		
		return position;
	}
	
	public float getCenterX(ClearCLImage image)
	{
		long [] imageDim = image.getDimensions();
		
		mArrayX = mContext.createBuffer(NativeTypeEnum.Float, imageDim[0]);
		
		mScanX.setArgument("image", image);
		mScanX.setArgument("arrayX", mArrayX);
		mScanX.setArgument("planeDimY", imageDim[1]);
		mScanX.setArgument("planeDimZ", imageDim[2]);
		mScanX.setGlobalSizes(imageDim[0],1,1);
		mScanX.run(true);
		
		float center = getCenter(mArrayX);
		
		return center;
	}
	
	public float getCenterY(ClearCLImage image)
	{
		long [] imageDim = image.getDimensions();
		
		mArrayY = mContext.createBuffer(NativeTypeEnum.Float, imageDim[1]);
		
		mScanY.setArgument("image", image);
		mScanY.setArgument("arrayX", mArrayX);
		mScanY.setArgument("planeDimX", imageDim[0]);
		mScanY.setArgument("planeDimZ", imageDim[2]);
		mScanY.setGlobalSizes(1,imageDim[1],1);
		mScanY.run(true);
		
		float center = getCenter(mArrayY);
		
		return center;
	}
	
	public float getCenterZ(ClearCLImage image)
	{
		long [] imageDim = image.getDimensions();
		
		mArrayZ = mContext.createBuffer(NativeTypeEnum.Float, imageDim[2]);
		
		mScanZ.setArgument("image", image);
		mScanZ.setArgument("arrayX", mArrayX);
		mScanZ.setArgument("planeDimX", imageDim[0]);
		mScanZ.setArgument("planeDimY", imageDim[1]);
		mScanZ.setGlobalSizes(1,1,imageDim[2]);
		mScanZ.run(true);
		
		float center = getCenter(mArrayZ);
		
		return center;
	}
	
}
