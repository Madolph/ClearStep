package prediction;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import framework.Setable;

/**
 * saves deviations and computes stochastic values
 */
public class PredictorStDev extends Predictor {
	
	/**
	 * An Array that saves the last 10 deviations
	 */
	public float[][] mDev = new float[10][2];
	
	/**
	 * The current Standard-Deviation
	 */
	float mStDev;
	
	/**
	 * The current mean (not a primitive value, to enable the functionality of mean-stiffness)
	 */
	public Setable mMean = new Setable();
	
	/**
	 * used to store the estimated mean for every point by regression
	 */
	double[] mMeanCache;
	
	/**
	 * The set sensitivity (a higher value means the program will need more change to adjust the timestep)
	 */
	float mSensitivity = (float) 0;
	
	/**
	 * decides whether the mean should be reset every step or always keep a part of its information
	 */
	float mMeanStiff = (float) 0.25;
	
	/**
	 * the current Sigma
	 */
	public float mCurrentSigma;
	
	public Setable mSigmaMetric = new Setable();
	
	SimpleRegression  mRegress = new SimpleRegression();
	
	/**
	 * creates an instance of memory and sets the array of deviations and timepoints to 0
	 */
	public PredictorStDev()
	{
		for (int i=0;i<mDev.length;i++)
		{
			mDev[i][0]=0;
			mDev[i][1]=0;
		}
	}
	
	@Override
	public float predict(float diff, float time)
	{
		rearrangeDev();
		mDev[0][0]= (diff)/(time-mDev[1][1]);
		mDev[0][1]= time;
	
		calcStDevAdapt();
		// Check if the new value is outside the Standard deviation
		if (mStDev==0)
			// No deviation means Sigma has to be zero (would divide by 0 otherwise)
			{ mCurrentSigma = 0; }
		else
			//{ mCurrentSigma=((mDev[0][0]-mMean)/mStDev); }
			{ mCurrentSigma = (float)((mDev[0][0]-mMean.val)/mStDev); }
		
		if (mSigmaMetric.set)
			{ mSigmaMetric.val = (mSigmaMetric.val*9+mCurrentSigma)/10; }
		else
			{ mSigmaMetric.val = mCurrentSigma; }
		
		mSigmaMetric.val /= 3;
		//mCurrentSigma /= 3;
		setPlotValues();
		System.out.println("Sigma is: "+mCurrentSigma+" / StDev: "+mStDev);
		//return mCurrentSigma;
		return mSigmaMetric.val;
	}
	
	/**
	 * Just shifts the values of the array.
	 * Loses the oldest value along the way.
	 */
	private void rearrangeDev()
	{
		for (int i=mDev.length-1;i>0;i--)
			{
			mDev[i][0]=mDev[i-1][0];
			mDev[i][1]=mDev[i-1][1];
			}
	}
	
	private void calcStDevAdapt(){
		mMeanCache=adaptMeanRegress();
		System.out.println("function ready");
		double lGap=0;
		for (int i=0;i<mDev.length;i++)
		{
			double lDummy = ((double)mDev[i][0]-mMeanCache[i]);
			lGap += lDummy*lDummy;
		}
		mStDev = (float) Math.sqrt(lGap);			
	}
	
	//TODO rethink the regression, how data is added and removed 
	//TODO make lRegress a field
	
	private double[] adaptMeanRegress(){
		
		mRegress.clear();
		for (int i=0;i<mDev.length;i++)
			{
			mRegress.addData(mDev[i][1], mDev[i][0]);
			}
		double[] lMeanCache = new double[mDev.length];
		for (int i=0;i<lMeanCache.length;i++)
		{
			lMeanCache[i] = mRegress.predict((double)mDev[i][1]);
		}
		
		float lMean=0;
		for (int i=0;i<lMeanCache.length;i++)
			lMean += lMeanCache[i];
		lMean = lMean/lMeanCache.length;
		
		if (mMean.set)
			mMean.val = mMean.val*(mMeanStiff)+lMean*(1-mMeanStiff);
		else
			{ mMean.val = lMean;
			mMean.set = true; }
		
		return lMeanCache;
	}
	
	public void setPlotValues()
	{
		value = mDev[0][0];
		//prediction = mCurrentSigma;
		prediction = mSigmaMetric.val;
		average = mMean.val;
		System.out.println("value: "+value+" / prediction: "+prediction+" / average: "+average);
	}
}
