package simulation;

import java.util.Random;

import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;

/**
 * Simulates a simple moving Dot
 * 
 * @author Raddock
 *
 */
public class Simulator {
	
	float [] mPosition = new float[3];

	/**
	 * Initializes the Position-array to zero
	 */
	public Simulator(){
		for (int i=0;i<mPosition.length;i++)
			mPosition[i]=0;
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
	public void generatePic(ClearCLContext lContext, ClearCLProgram lProgram, 
			float time, ClearCLImage lImage1, int lSize, boolean noise)
	{ 
	    computePosition(time);
	    Random lRandom = new Random();
	    float random = 0;
	    
	    if (noise)
	    	random = (float)16.807*(lRandom.nextFloat());
	    
	    boolean vibrate = false;
	    
	    float vibration = 0;
	    
	    if (vibrate)
	    	vibration = (lRandom.nextFloat()-0.5f)*4;
	    
	    //ClearCLKernel lKernel = lProgram.createKernel("sphere");
	    ClearCLKernel lKernel = lProgram.createKernel("noisySphere");
	    lKernel.setGlobalSizes(lImage1);
	    lKernel.setArgument("image", lImage1);
	    lKernel.setArgument("cx", ((lSize/2)+mPosition[0]));
	    lKernel.setArgument("cy", ((lSize/2)+mPosition[1])+vibration);
	    lKernel.setArgument("cz", ((lSize/2)+mPosition[2]));
	    lKernel.setArgument("r", 0.25f);
	    lKernel.setArgument("p1", random);
	    
	    lKernel.run(true);      
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
