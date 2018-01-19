package simulation;

import java.io.IOException;
import java.util.Random;

import Kernels.KernelDemo;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.enums.ImageChannelDataType;
import coremem.offheap.OffHeapMemory;

/**
 * Simulates a simple moving Dot
 * 
 * @author Raddock
 *
 */
public class Simulator {
	
	float [] mPosition = new float[3];
	
	public ImageChannelDataType mDataType;
	
	public ClearCLProgram simulation;
	
	ClearCLContext mContext;

	/**
	 * Initializes the Position-array to zero
	 * @throws IOException 
	 */
	public Simulator(ImageChannelDataType Datatype, ClearCLContext Context) throws IOException{
		for (int i=0;i<mPosition.length;i++)
			mPosition[i]=0;
		mDataType = Datatype;
		mContext = Context;
		createSimProgram();
	}
	
	public ClearCLKernel sim;
	
	public void createSimProgram() throws IOException
	{
		simulation=mContext.createProgram(KernelDemo.class, "Simulator.cl");
		switch (mDataType)
		{
		case Float:
			simulation.addDefine("WRITE_IMAGE", "write_imagef");
			simulation.addDefine("DATA", "float4");
			break;
		case UnsignedInt16:
			simulation.addDefine("WRITE_IMAGE", "write_imageui");
			simulation.addDefine("DATA", "uint4");
			break;
		default:
			simulation.addDefine("WRITE_IMAGE", "write_imagef");
			simulation.addDefine("DATA", "float4");
			break;
		}
		
		simulation.buildAndLog();
	}
	
	/**
	 * creates two pictures that depend on the time
	 * 
	 * @param lContext 	The OpenCL-Context
	 * @param lProgram 	The OpenCL-Program
	 * @param time 		The current time
	 * @param lImage1 	Storage for the generated image
	 * @param lSize 	The size of the images
	 */
	public void generatePic(float time, ClearCLImage lImage1, int lSize, boolean noise)
	{ 
	    computePosition(time);
	    Random lRandom = new Random();
	    
	    sim = simulation.createKernel("noisySphere");
	    sim.setGlobalSizes(lImage1);
	    sim.setArgument("image", lImage1);
	    sim.setArgument("cx", ((lSize/2)+mPosition[0]));
	    sim.setArgument("cy", ((lSize/2)+mPosition[1]));
	    sim.setArgument("cz", ((lSize/2)+mPosition[2]));
	    sim.setArgument("r", 0.25f);
	    
	    sim.run(true);   
	    
	    //ByteBuffer lBuffer = ByteBuffer.allocate(lSize*lSize*lSize*4);
	    
	    if (noise)
	    {
	    	OffHeapMemory lMem = OffHeapMemory.allocateFloats(lSize*lSize*lSize);
	    
	    	System.out.println("Buffersize: "+lSize*lSize*lSize);
	    
	    	System.out.println("Buffersize: "+lMem.getSizeInBytes());

	    	lImage1.writeTo(lMem, true);
	    	
	    	System.out.println("image got pasted");
	    	
	    	for (int i=0;i<lMem.getSizeInBytes()/4;i++)
	    	{
	    		float val = lMem.getFloatAligned(i);
	    		val = val + (lRandom.nextFloat()*5);
	    		lMem.setFloatAligned(i, val);
	    	}
	    
	    System.out.println("loop finished");
	    
	    lImage1.readFrom(lMem, true);
	    }
	}
	
	/**
	 * returns the middle of the generates spheres that appear in the picture
	 * 
	 * @param time	The current timepoint
	 */
	public void computePosition(float time)
	{
		float timespan = 40000;
		float period = timespan/4;
		float periodTime = time%period;
		float phase = time%timespan;
		if (phase<period)
			mPosition[0]=0;
		if (phase>=period && phase<(period*2))
			mPosition[0]=32*((periodTime)/period);
		if (phase>=(period*2) && phase<(period*3))
			mPosition[0]=32;
		if (phase>=(period*3))
			mPosition[0]=32-(32*((periodTime)/period));
	}
}
