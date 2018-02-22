package prediction;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import framework.Setable;



/**
 * saves a value to a memory of the last 10 values and computes
 * a measure of current trend of the values
 */
public class PredictorRegression 	extends 
									Predictor
									implements
									PredictorInterface
{
	
	/**
	 * An Array that saves the last 10 deviations and their time points
	 */
	public float[][] mDev = new float[10][2];
	
	/**
	 * The current Mean-Square-Root
	 */
	float mRMSE;
	
	/**
	 * The current Series-Level (not a primitive value, to enable the functionality of a walking average)
	 */
	public Setable mSeriesLevel = new Setable();
	
	/**
	 * coefficient that decides how strongly the new value will influence the series-level
	 */
	float mSeriesSmooth = 0.75f;
	
	/**
	 * the current offset from the regression divided by the root of the MSE
	 */
	public float mCurrOffset;
	
	/**
	 * coefficient to determine how much the offset will be smoothed with previous information
	 */
	public float mOffsetSmooth = 0.75f;
	
	/**
	 * the smoothed offset that will be used for further calculation
	 */
	public Setable mAvgOffset = new Setable();
	
	/**
	 * an object that holds data and produces a regression
	 */
	SimpleRegression  mRegress = new SimpleRegression();
	
	/**
	 * sets the array of deviations and timepoints to 0
	 */
	public PredictorRegression()
	{
		for (int i=0;i<mDev.length;i++)
		{
			mDev[i][0]=0;
			mDev[i][1]=0;
		}
	}
	
	/**
	 * receives a new value and the corresponding timepoint and then calculates the metric
	 */
	@Override
	public float predict(float diff, float time)
	{
		rearrangeDev();
		mDev[0][0]= (diff)/(time-mDev[1][1]);
		mDev[0][1]= time;
	
		adaptMeanRegress();
		setSeriesLevel();
		// Check if the new value is outside the Standard deviation
		if (mRMSE==0)
			// No deviation means Sigma has to be zero (would divide by 0 otherwise)
			{ mCurrOffset = 0; }
		else
			{
			
			//mCurrOffset = (float)((mDev[0][0]-(float)mRegress.predict((double)mDev[0][1]))/mRMSE);
			//mCurrOffset /= 3;
			mCurrOffset = (float)((mDev[0][0]-mSeriesLevel.val)/mRMSE);
			}
		//System.out.println("value is: "+mDev[0][0]+" and prediction would have been: "+mRegress.predict((double)mDev[0][1]));
		
		/*if (mAvgOffset.set)
			{ mAvgOffset.val = mAvgOffset.val*mOffsetSmooth+mCurrOffset*(1-mOffsetSmooth); }
		else
			{ mAvgOffset.val = mCurrOffset; }*/ // need to be set true for smoothing to happen
		
		setPlotValues();
		System.out.println("Sigma is: "+mCurrOffset+" / StDev: "+mRMSE);
		return mCurrOffset;
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
	 * clear the old regression, sets up a new one and calculates the root of the RMSE
	 */
	private void adaptMeanRegress(){
		
		mRegress.clear();
		for (int i=0;i<mDev.length;i++)
		{
			mRegress.addData(mDev[i][1], mDev[i][0]);		
		}
		
		mRMSE = (float)Math.sqrt(mRegress.getMeanSquareError());
		
	}
	
	/**
	 * adjusts the current level of the series... and does so smoothly (oh-yeah)
	 */
	private void setSeriesLevel()
	{
		if (mSeriesLevel.set)
			mSeriesLevel.val = mDev[0][0]*(mSeriesSmooth)+mSeriesLevel.val*(1-mSeriesSmooth);
		else
			{ mSeriesLevel.val = mDev[0][0];
			mSeriesLevel.set = true; }
	}
	
	/**
	 * pasts some values into extra spots so a plotter can pick them up (purely for convenience)
	 */
	public void setPlotValues()
	{
		value1 = mDev[0][0];
		value2 = mCurrOffset;
		//average = (float)mRegress.predict((double)mDev[0][1]);
		value3 = mSeriesLevel.val;
		System.out.println("value: "+value1+" / prediction: "+value2+" / average: "+value3);
	}
}
