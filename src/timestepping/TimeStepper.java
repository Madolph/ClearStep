package timestepping;

import framework.Setable;
import prediction.PredictorStDev;

public class TimeStepper {
	
	/**
	 * The Memory that stores the deviations and their stochastic values
	 */
	public PredictorStDev mInfo= new PredictorStDev();
	
	Setable mStepSmooth = new Setable();
	
	boolean fluid;
	
	/**
	 * The step that is used as the center of the step-span
	 */
	public float mNeutralStep;
	
	/**
	 * The span of steps that is available for spontaneous change
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
	 * Determines how slowly the neutral step changes
	 */
	public float mRollback = (float) 0.01;
	
	/**
	 * The currently chosen timestep
	 */
	public float mStep;
	
	public float smoothing = 0.9f;
	
	/**
	 * Create a new Timestepper
	 * 
	 * @param start		The initially chosen timestep
	 * @param width		The allowed span of the timestep from the neutral step (which is dynamic)
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

	public void limitMetric(float metric)
	{
		if (metric>1)
			metric=1;
		if (metric<-1)
			metric=-1;
	}
	
	public void limitStep()
	{
		if (mStep>mMaxStep)
			mStep = mMaxStep;
		if (mStep<mMinStep)
			mStep = mMinStep;
	}
	
	public void computeNextStep(float metric, float step)
	{
		mStep = step;
		limitMetric(metric);
		mStep=mNeutralStep+(-metric*mSpan);
		
		limitStep();
		
		if (!mStepSmooth.set)
		{
			mStepSmooth.val = mStep;
			mStepSmooth.set= true;
		}
		else 
			mStepSmooth.val = mStepSmooth.val*(smoothing)+mStep*(1-smoothing);
			
		mStep = mStepSmooth.val;	
		
		// over time, the step will get back to the original step
		mStep =(mNeutralStep*mRollback) + (mStep*(1-mRollback));
		
		step = mStep;
	}
}
