package timestepping;

import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;

public class Simulator {

	/**
	 * creates two pictures that depend on the time
	 * 
	 * @param lContext The OpenCL-Context
	 * @param lProgram The OpenCL-Program
	 * @param time The current time
	 * @param lImage1 Storage for the first image
	 * @param lImage2 Storage for the second image
	 * @param lSize The size of the images
	 * @param step The current timestep
	 */
	public void generatePic(ClearCLContext lContext, ClearCLProgram lProgram, 
			float time, ClearCLImage lImage1, int lSize)
	{ 
	    float[] Position = computePosition(time);
	    
	    ClearCLKernel lKernel = lProgram.createKernel("xorsphere");
	    lKernel.setArgument("image", lImage1);
	    lKernel.setGlobalSizes(lImage1);
	    lKernel.setOptionalArgument("r", 0.25f);
	    lKernel.setOptionalArgument("cx", lSize / 2 + Position[0]);
	    System.out.print("shift X is: "+Position[0]);
	    lKernel.setOptionalArgument("cy", lSize / 2 + Position[1]);
	    System.out.print("shift Y is: "+Position[1]);
	    lKernel.setOptionalArgument("cz", lSize / 2 + Position[2]);
	    System.out.print("shift Z is: "+Position[2]);
	    lKernel.setOptionalArgument("a", 1);
	    lKernel.setOptionalArgument("b", 1);
	    lKernel.setOptionalArgument("c", 1);
	    lKernel.setOptionalArgument("d", 1);
	    lKernel.run(true);
	    System.out.println("image generated");      
	}
	
	/**
	 * returns the middle of the generates spheres that appear in the picture
	 * 
	 * @param time The current timepoint
	 * @return The positions of the sphere in every dimension
	 */
	public float[] computePosition(float time)
	{
		float timespan = 0;
		float increment = 3;
		float[] Position = new float[3];
		float[] Alpha = getAlpha(time, timespan, increment);
		System.out.print("AX: "+Alpha[0]+" / AY: "+Alpha[1]+" / AZ: "+Alpha[2]);
		for (int i=0;i<Position.length;i++)
		{
			Position[i]=Alpha[i]*(time);
		}
		return Position;
	}
	
	/**
	 * Parses the Alpha, that changes periodically
	 * 
	 * @param time The current time
	 * @param timespan The length of a period
	 * @param increment The amount of spatial change
	 * @return The Alpha for every dimension
	 */
	public float[] getAlpha(float time, float timespan, float increment)
	{
		float[] Alpha=new float[3];
		for (int i=0;i<Alpha.length;i++)
		{
			Alpha[i]=increment;
			System.out.print("Alpha is: "+Alpha[i]+"  increment is: "+increment);
		}
		return Alpha;
	}
}
