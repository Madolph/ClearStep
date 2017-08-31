package timestepping;


public class Memory {
	
  public float[] dev = new float[10];
  float StDev;
  float mean;
  float sensitivity = (float) 0.5;
  float currentSigma;
  boolean FirstRun;

	public Memory()
	{
		FirstRun = true;
	}
	
	/**
   * saves the supplied difference to the array, after shifting all the values,
   * so that only the 10 newest ones are stored. Also checks if the array has
   * been initialized yet and if not, does so. Then is calculates some metrics
   * and answers, if it is necessary to compute a new step
   * 
   * @param diff
   * @param step
   * @param time
   * @return true if a new step needs to be calculated
   */
	public boolean saveAndCheckDiff(float diff, float step)
	{
		// if this is the first run, every value is set to the current difference
		if (FirstRun)
		{
			//System.out.println("this is the first run");
			for (int i=0;i<dev.length-1;i++)
			{
				dev[i]=diff/step;
			}
			FirstRun=false;
		}
		
		this.rearrangeDiff();
		dev[0]= diff/step;
		this.calcStDev();
		// Check if the new value is outside the Standard deviation
		currentSigma=((dev[0]-mean)/StDev);
		
		if (currentSigma>sensitivity || 
			currentSigma<-sensitivity)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * just shifts the values of the array
	 */
	private void rearrangeDiff()
	{
		for (int i=dev.length-1;i>1;i--)
			dev[i]=dev[i-1];
	}
	
	/**
	 * calculates the standard-deviation
	 */
	private void calcStDev()
	{
		mean=0;
		float gap=0;
		for (int i=0;i<dev.length;i++)
		{
			mean += dev[i];
		}
		mean = mean/dev.length;
		for (int i=0;i<dev.length;i++)
		{
			float lGap= (dev[i]-mean);
			gap += lGap*lGap;
		}
		this.StDev = (float) Math.sqrt(gap);
	}
}
