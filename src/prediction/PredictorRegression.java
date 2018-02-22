package prediction;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import framework.Setable;



/**
 * a predictor that saves the 10 most recent values and estimates the current trend of data
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
	 * The current Root mean square error
	 */
	float mRMSE;
	
	/**
	 * The current Series-Level
	 */
	public Setable mSeriesLevel = new Setable();
	
	/**
	 * coefficient that decides how strongly the new value will influence the series-level
	 */
	float mSeriesLevelWeighting = 0.75f;
	
	/**
	 * the current offset from the regression divided by the root of the MSE
	 */
	public float mCurrOffset;
	
	/**
	 * an object that receives the cache and produces a regression
	 */
	SimpleRegression  mRegress = new SimpleRegression();
	
	/**
	 * creates the predictor and sets initializes all saved values to zero
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
	 * @param diff the value to be analyzed
	 * @param time the corresponding time point
	 * @return the computed metric
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
		{
			// No deviation means sigma has to be zero (would divide by 0 otherwise)
			mCurrOffset = 0; 
		}
		else
		{
			mCurrOffset = (float)((mDev[0][0]-mSeriesLevel.val)/mRMSE);
		}
		
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
	 * clears the old regression, sets up a new one and calculates the RMSE
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
	 * adjusts the current level of the series... and does so smoothly
	 */
	private void setSeriesLevel()
	{
		if (mSeriesLevel.set)
			mSeriesLevel.val = mDev[0][0]*(mSeriesLevelWeighting)+mSeriesLevel.val*(1-mSeriesLevelWeighting);
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
		value3 = mSeriesLevel.val;
		System.out.println("value: "+value1+" / prediction: "+value2+" / average: "+value3);
	}
}
