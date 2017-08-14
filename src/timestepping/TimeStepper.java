package timestepping;

public class TimeStepper {
	
	public Memory Info;
	public float neutralStep;
	public float span;
	// sets the stiffness of the neutralStep
	public float stiff = (float) 0.9;
	public float step;
	
	public TimeStepper(float start, float width)
	{
		neutralStep = start*1000;
		span = width*1000;
		step = neutralStep;
	}

	public float computeStep(float diff)
	{
		boolean calcStep = Info.saveAndCheckDiff(diff);
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
			if (Info.currentSigma>3)
			{
				step = neutralStep+span;
				stepSet=true;
			}
			if (Info.currentSigma<-3)
			{
				step = neutralStep-span;
				stepSet=true;
			}
			if (!stepSet)
			{
				step=neutralStep+(Info.currentSigma/3)*span;
			}
		}
		
		// adjust neutral Step
		neutralStep = (float) ((neutralStep*stiff) + (step*(1-stiff)));
		return step;
	}
}
