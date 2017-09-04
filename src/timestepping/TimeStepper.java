package timestepping;

public class TimeStepper {
	
	/**
	 * The Memory that stores the deviations and their stochastic values
	 */
	public Memory mInfo= new Memory();
	
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
	public float mStiffness = (float) 0.99;
	
	/**
	 * The currently chosen timestep
	 */
	public float mStep;
	
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

	/**
	 * calculates the new Timestep
	 * 
	 * @param diff the metric of image-change
	 * @return the newly computed timestep
	 */
	public float computeStep(float diff, float currStep)
	{
		boolean calcStep = mInfo.saveAndCheckDiff(diff, currStep);
		if (!calcStep)
		{
			// do nothing if the current change is within the current Area of error
			;
		}
		else
		{
			// calculate new Step when necessary
			// first, check, if the Sigma is at the limit
			boolean stepSet=false;
			if (mInfo.mCurrentSigma>=3)
			{
				mStep = mNeutralStep-mSpan;
				stepSet=true;
			}
			if (mInfo.mCurrentSigma<=-3)
			{
				mStep = mNeutralStep+mSpan;
				stepSet=true;
			}
			if (!stepSet)
			{
				mStep=mNeutralStep-(mInfo.mCurrentSigma*(mSpan/3));
			}
		}
		
		// adjust neutral Step
		mNeutralStep =((mNeutralStep*mStiffness) + (mStep*(1-mStiffness)));
		
		if (mStep>mMaxStep)
			mStep = mMaxStep;
		if (mStep<mMinStep)
			mStep = mMinStep;
		
		return mStep;
	}
}
