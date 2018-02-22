package timestepping;

import framework.Setable;

public class TimeStepper {
	
	boolean fluid;
	
	/**
	 * The step that is used as a start and default value
	 */
	public float mNeutralStep;
	
	/**
	 * The span of steps that is allowed to spontaneously change
	 */
	public float mSpan;
	
	/**
	 * the absolute maximum step
	 */
	public float mMaxStep;
	
	/**
	 * The absolute minimum step
	 */
	public float mMinStep;
	
	/**
	 * Determines how heavily the step will be reset to the neutral step
	 */
	public float mRollback = (float) 0.05;
	
	/**
	 * The current time step
	 */
	public float mStep;
	
	/**
	 * a smoothed representation of the time step to damp fluctuations
	 */
	Setable mStepSmooth = new Setable();
	
	/**
	 * weighting coefficient for the smoothed time step
	 */
	public float smoothing = 0.5f;
	
	/**
	 * Create a new Timestepper
	 * 
	 * @param start		The initially chosen timestep
	 * @param width		The allowed span of spontaneous change
	 * @param maxStep	The maximum allowed timestep (static)
	 * @param minStep	The minimum allowed timestep (static)
	 */
	public TimeStepper(float start, float width, float maxStep, float minStep)
	{
		mNeutralStep = start*1000;
		mSpan = width*1000;
		mStep = mNeutralStep;
		mMaxStep = maxStep*1000;
		mMinStep = minStep*1000;
	}

	/**
	 * truncates the metric to an interval between -1 and 1
	 * 
	 * @param metric the metric to be truncated and processed
	 * @return the processed metric
	 */
	public float processMetric(float metric)
	{
		if (metric>1)
			metric=1;
		if (metric<-1)
			metric=-1;
		metric = metric*Math.abs(metric);
		return metric;
	}
	
	/**
	 * limits the calculated step to the interval allowed
	 */
	public void limitStep()
	{
		if (mStep>mMaxStep)
			mStep = mMaxStep;
		if (mStep<mMinStep)
			mStep = mMinStep;
	}
	
	/**
	 * uses the metric and the current step to compute the length of the next time step
	 * 
	 * @param metric the metric used for calculation
	 * @param step the current time step
	 */
	public void computeNextStep(float metric, float step)
	{
		mStep = step;
		metric = processMetric(metric);
		mStep=mStep+(-metric*mSpan);
		
		limitStep();
		
		if (!mStepSmooth.set)
		{
			mStepSmooth.val = mStep;
			mStepSmooth.set= true;
		}
		else 
		{
			mStepSmooth.val = mStepSmooth.val*(smoothing)+mStep*(1-smoothing);
		}
		
		mStep = mStepSmooth.val;	
		
		// over time, the step will get back to the original step
		mStep =(mNeutralStep*mRollback) + (mStep*(1-mRollback));
	}
}
