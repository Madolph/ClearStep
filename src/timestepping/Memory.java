package timestepping;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * saves deviations and computes stochastic values
 */
public class Memory {
	
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
	Setable mMean = new Setable();
	
	/**
	 * used to store the estimated mean for every point by regression
	 */
	double[] mMeanCache;
	
	/**
	 * The set sensitivity (a higher value means the program will need more change to adjust the timestep)
	 */
	float mSensitivity = (float) 0.20;
	
	/**
	 * decides whether the mean should be reset every step or always keep a part of its information
	 */
	float mMeanStiff = (float) 0.0;
	
	/**
	 * the current Sigma
	 */
	float mCurrentSigma;
	
	/**
	 * becomes true when 10 the dev-matrix is set up
	 */
	boolean AllSet=false;
	
	
	/**
	 * creates an instance of memory and sets the array of deviations and timepoints to 0
	 */
	public Memory()
	{
		for (int i=0;i<mDev.length;i++)
		{
			mDev[i][0]=0;
			mDev[i][1]=0;
		}
	}
	
	/**
	 * saves the supplied difference to the array, after shifting all the values,
	 * so that only the 10 newest ones are stored. Also checks if the array has
	 * been initialized yet and if not, does so. Then is calculates some metrics
	 * and answers, if it is necessary to compute a new step
	 * 
	 * @param diff	The difference-metric that is saved as a new deviation
	 * @param time	The currently used timestep
	 * @return True if a new step needs to be calculated
	 */
	public boolean saveAndCheckDiff(float diff, float time)
	{
		rearrangeDev();
		mDev[0][0]= (diff)/(time-mDev[1][1]);
		mDev[0][1]= time;
		
		if (mDev[9][1]==0)
			return false;
	
		calcStDevAdapt();
		System.out.println("Sigma is: "+mCurrentSigma+" / StDev: "+mStDev);
		// Check if the new value is outside the Standard deviation
		if (mStDev==0)
			// No deviation means Sigma has to be zero (would devide by = otherwise)
			{ mCurrentSigma=0; }
		else
			//{ mCurrentSigma=((mDev[0][0]-mMean)/mStDev); }
			{ mCurrentSigma=(float) ((mDev[0][0]-mMean.val)/mStDev); }
		
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
			{
			mDev[i][0]=mDev[i-1][0];
			mDev[i][1]=mDev[i-1][1];
			}
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
			lMean += mDev[i][0];
		}
		mMean.val = lMean/mDev.length;
		for (int i=0;i<mDev.length;i++)
		{
			float lDummy= (mDev[i][0]-mMean.val);
			lGap += lDummy*lDummy;
		}
		mStDev = (float) Math.sqrt(lGap);
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
	
	private double[] adaptMean(){
		double[] x = new double[mDev.length];
		double[] y = new double[mDev.length];
		
		for (int i=0;i<x.length;i++)
			{
			x[i]=(double) mDev[mDev.length-i-1][1];
			y[i]=(double) mDev[mDev.length-i-1][0];
			}
		// TODO Use Regression instead of interpolation
		LinearInterpolator  lLinInterp = new LinearInterpolator() ;
		PolynomialSplineFunction lPolyFunc = lLinInterp.interpolate(x, y);
		double[] lMeanCache = new double[mDev.length];
		for (int i=0;i<lMeanCache.length;i++)
		{
			lMeanCache[i] = lPolyFunc.value(x[x.length-i-1]);
		}
		float lMean=0;
		for (int i=0;i<lMeanCache.length;i++)
			lMean += lMeanCache[i];
		lMean = lMean/lMeanCache.length;
		
		mMean.val = mMean.val*(mMeanStiff)+lMean*(1-mMeanStiff);
		
		return lMeanCache;
	}
	
	private double[] adaptMeanRegress(){
		SimpleRegression  lRegress = new SimpleRegression();
		for (int i=0;i<mDev.length;i++)
			{
			lRegress.addData(mDev[i][1], mDev[i][0]);
			}
		double[] lMeanCache = new double[mDev.length];
		for (int i=0;i<lMeanCache.length;i++)
		{
			lMeanCache[i] = lRegress.predict((double)mDev[i][1]);
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
}
