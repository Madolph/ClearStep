package timestepping;

import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;

public class Simulator {
	
	float [] Position = new float[3];

	public Simulator(){
		for (int i=0;i<Position.length;i++)
			Position[i]=0;
	}
	
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
	    computePosition(time);
	    
	    ClearCLKernel lKernel = lProgram.createKernel("sphere");
	    lKernel.setArgument("image", lImage1);
	    lKernel.setGlobalSizes(lImage1);
	    lKernel.setArgument("r", 0.25f);
	    
	    lKernel.setArgument("cx", ((lSize/2)+Position[0]));
	    System.out.println("shift X is: "+Position[0]);
	    lKernel.setArgument("cy", ((lSize/2)+Position[1]));
	    System.out.println("shift Y is: "+Position[1]);
	    lKernel.setArgument("cz", ((lSize/2)+Position[2]));
	    System.out.println("shift Z is: "+Position[2]);
	    
	    /**lKernel.setArgument("cx", lSize/2);
		lKernel.setArgument("cy", lSize/2);
		lKernel.setArgument("cz", lSize/2);**/
	    
	    lKernel.run(true);
	    
	    System.out.println("image generated");      
	}
	
	/**
	 * returns the middle of the generates spheres that appear in the picture
	 * 
	 * @param time The current timepoint
	 * @return The positions of the sphere in every dimension
	 */
	public void computePosition(float time)
	{
		float timespan = 20000;
		float period = timespan/4;
		//float[] Alpha = getAlpha(time, timespan);
		//System.out.println("AX: "+Alpha[0]+" / AY: "+Alpha[1]+" / AZ: "+Alpha[2]);
		float PeriodTime = time%period;
		float Phase = time%timespan;
		if (Phase<period)
			Position[0]=0;
		if (Phase>=period && Phase<(period*2))
			Position[0]=32*((PeriodTime)/period);
		if (Phase>=(period*2) && Phase<(period*3))
			Position[0]=32;
		if (Phase>=(period*3))
			Position[0]=32-(32*((PeriodTime)/period));
	}
	
	/**
	 * Parses the Alpha, that changes periodically
	 * 
	 * @param time The current time
	 * @param timespan The length of a period
	 * @param increment The amount of spatial change
	 * @return The Alpha for every dimension
	 */
	public float[] getAlpha(float time, float timespan)
	{
		float[] Alpha=new float[3];
		float periodTime = time%(4*timespan);
		for (int i=0;i<Alpha.length;i++)
		{
			Alpha[i]=0;
		}
		if (periodTime<timespan)
			;
		if (periodTime>=timespan && periodTime<(timespan*2))
			Alpha[0]=1;
		if (periodTime>=(timespan*2) && periodTime <(timespan*3))
			;
		if (periodTime>=(timespan*3))
			Alpha[0]=-1;
		return Alpha;
	}
}
