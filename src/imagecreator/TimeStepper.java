package imagecreator;

import java.io.IOException;

public class TimeStepper {

	public Stackholder Stacks = new Stackholder();
	public Demo lApp = new Demo();
	public float globMin;
	public float locMax;
	public float lowStep;
	public float StepVari;
	
	public TimeStepper(float minStep, float StepSpan)
	{
		lowStep=minStep;
		StepVari=StepSpan;
		globMin=999999;
	}
	
	public void scanstacks()
	{
		for (int i=0;i<Stacks.Cache.length;i++)
		{
			if (Stacks.Cache[i]<globMin)
			{
				globMin=Stacks.Cache[i];
			}
		}
		locMax=Stacks.Cache[0];
		for (int i=1;i<Stacks.Cache.length;i++)
		{
			if (Stacks.Cache[i]>locMax)
			{
				locMax=Stacks.Cache[i];
			}
		}
	}
	
	public float computeStep(){
		float locDist = (((Stacks.Cache[0]+Stacks.Cache[1])/2)-globMin)/
									(locMax-globMin);
		float step = lowStep+(locDist*StepVari);
		return step;
	}
	
	public void assignCache() throws InterruptedException, IOException{
		lApp.CompareDemoImages();
		for (int i=0;i<lApp.changes.length;i++)
		{
			Stacks.register(lApp.changes[i]);
		}
	}
}
