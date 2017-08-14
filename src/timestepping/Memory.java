package timestepping;

public class Memory {
	
	public float[] dev= new float[10];
	public float StDev;
	public float mean;
	public float sensitivity = (float) 1.5;
	public float currentSigma;
	public boolean FirstRun;

	public Memory()
	{
		FirstRun = true;
	}
	
	public boolean saveAndCheckDiff(float diff)
	{
		// if this is the first run, every value is set to the current difference
		if (FirstRun)
		{
			for (int i=0;i<dev.length-1;i++)
			{
				dev[i]=diff;
			}
		}
		
		this.rearrangeDiff();
		dev[0]= diff;
		this.calcStDev();
		// Check if the new value is outside the Standard deviation
		currentSigma=((dev[0]-mean)/StDev);
		
		if (currentSigma>sensitivity || 
			currentSigma<(sensitivity*-1))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private void rearrangeDiff()
	{
		for (int i=0;i<dev.length-1;i++)
			dev[i+1]=dev[i];
	}
	
	private void calcStDev()
	{
		mean=0;
		float gap=0;
		for (int i=0;i<dev.length;i++)
			mean += dev[i];
		for (int i=0;i<dev.length;i++)
		{
			float lGap= (dev[i]-mean);
			gap += lGap*lGap;
		}
		this.StDev = (float) Math.sqrt(gap);
	}
}
