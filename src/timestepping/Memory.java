package timestepping;

/**
 * saves deviations and computes stochastic values
 */
public class Memory {
	
	/**
	 * An Array that saves the last 10 deviations
	 */
	public float[] mDev = new float[10];
	
	/**
	 * The current Standard-Deviation
	 */
	float mStDev;
	
	/**
	 * The current mean
	 */
	float mMean;
	
	/**
	 * The set sensitivity (a higher value means the program will need more change to adjust the timestep)
	 */
	float mSensitivity = (float) 0.4;
	
	/**
	 * the current Sigma
	 */
	float mCurrentSigma;
	
	/**
	 * Is set to false after the first run
	 */
	boolean FirstRun=true;
	
	/**
	 * saves the supplied difference to the array, after shifting all the values,
	 * so that only the 10 newest ones are stored. Also checks if the array has
	 * been initialized yet and if not, does so. Then is calculates some metrics
	 * and answers, if it is necessary to compute a new step
	 * 
	 * @param diff	The difference-metric that is saved as a new deviation
	 * @param step	The currently used timestep
	 * @return True if a new step needs to be calculated
	 */
	public boolean saveAndCheckDiff(float diff, float step)
	{
		// if this is the first run, every value is set to the current difference
		if (FirstRun)
			{ 
			for (int i=0;i<mDev.length-1;i++)
				{ mDev[i]=diff/step; }
			FirstRun=false; 
			}
		
		rearrangeDev();
		mDev[0]= diff/step;
		calcStDev();
		// Check if the new value is outside the Standard deviation
		if (mStDev==0)
			// No deviation means Sigma has to be zero (would devide by = otherwise)
			{ mCurrentSigma=0; }
		else
			{ mCurrentSigma=((mDev[0]-mMean)/mStDev); }
		
		if (mCurrentSigma>mSensitivity || mCurrentSigma<-mSensitivity)
		{ return true; }
		else
		{ return false; }
	}
	
	/**
	 * Just shifts the values of the array.
	 * Loses the oldest value along the way.
	 */
	private void rearrangeDev()
	{
		for (int i=mDev.length-1;i>0;i--)
			mDev[i]=mDev[i-1];
	}
	
	/**
	 * calculates the standard-deviation
	 */
	private void calcStDev()
	{
		float lMean=0;
		float lGap=0;
		for (int i=0;i<mDev.length;i++)
		{
			lMean += mDev[i];
		}
		mMean = lMean/mDev.length;
		for (int i=0;i<mDev.length;i++)
		{
			float lDummy= (mDev[i]-mMean);
			lGap += lDummy*lDummy;
		}
		mStDev = (float) Math.sqrt(lGap);
	}
}
