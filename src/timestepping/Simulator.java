package timestepping;

import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;

public class Simulator {

	public void generatePics(ClearCLContext lContext, ClearCLProgram lProgram, 
			float time, ClearCLImage lImage1, ClearCLImage lImage2, int lSize, float step)
	{ 
	    float[] Position = this.computePosition(time);
	    
	    ClearCLKernel lKernel = lProgram.createKernel("xorsphere");
	    lKernel.setArgument("image", lImage1);
	    lKernel.setGlobalSizes(lImage1);
	    lKernel.setOptionalArgument("r", 0.25f);
	    lKernel.setOptionalArgument("cx", lSize / 2 + Position[0]);
	    lKernel.setOptionalArgument("cy", lSize / 2 + Position[1]);
	    lKernel.setOptionalArgument("cz", lSize / 2 + Position[2]);
	    lKernel.setOptionalArgument("a", 1);
	    lKernel.setOptionalArgument("b", 1);
	    lKernel.setOptionalArgument("c", 1);
	    lKernel.setOptionalArgument("d", 1);
	    lKernel.run(true);
	    System.out.println("image1 done");
	    
	    time += step;
	    Position = this.computePosition(time);
	    
	    lKernel.setArgument("image", lImage2);
	    lKernel.setGlobalSizes(lImage2);
	    lKernel.setOptionalArgument("cx", lSize / 2 + Position[0]);
	    lKernel.setOptionalArgument("cy", lSize / 2 + Position[1]);
	    lKernel.setOptionalArgument("cz", lSize / 2 + Position[2]);
	    lKernel.run(true);
	    System.out.println("image2 done");	      
	}
	
	public float[] computePosition(float time)
	{
		float timespan = 10000;
		float increment = 12;
		float[] Position = new float[3];
		float[] Alpha = this.getAlpha(time, timespan, increment);
		for (int i=0;i<Position.length;i++)
		{
			if ((time%(timespan*2))<timespan)
				Position[i]=Alpha[i]*(time%timespan);
			else
				Position[i]=((increment*(timespan/2))-(Alpha[i]*(time%timespan)));
		}
		return Position;
	}
	
	public float[] getAlpha(float time, float timespan, float increment)
	{
		float[] Alpha=new float[3];
		for (int i=0;i<Alpha.length;i++)
		{
			if ((time%timespan)<(timespan/2))
			{
				Alpha[i]=1/4*increment;
			}
			else
			{
				Alpha[i]=3/4*increment;
			}
		}
		return Alpha;
	}
}
