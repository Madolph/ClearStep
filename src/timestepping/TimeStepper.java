package timestepping;

public class TimeStepper {
	
	public Memory Info= new Memory();
	public float neutralStep;
	public float span;
	public float max;
	public float min;
	// sets the stiffness of the neutralStep
	public float stiff = (float) 0.9;
	public float step;
	
	/**
	 * initializes some necessary values of the TimeStepper
	 * 
	 * @param start the chosen average timestep at the beginning
	 * @param width the span of timesteps possible
	 */
	public TimeStepper(float start, float width, float maxStep, float minStep)
	{
		neutralStep = start*1000;
		span = width*1000;
		step = neutralStep;
		max = maxStep*1000;
		min = minStep*1000;
	}

	/**
	 * calculates the new Timestep
	 * 
	 * @param diff the metric of image-change
	 * @return the newly computed timestep
	 */
	public float computeStep(float diff, float currStep)
	{
		boolean calcStep = Info.saveAndCheckDiff(diff, currStep);
		if (!calcStep)
		{
			System.out.println("no need for new step");
			// do nothing if the current change is within the current Area of error
			;
		}
		else
		{
			System.out.println("new step neccessary");
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
				step=neutralStep-(Info.currentSigma/3)*span;
			}
		}
		
		// adjust neutral Step
		neutralStep = (float) ((neutralStep*stiff) + (step*(1-stiff)));
		
		if (step>max)
			step = max;
		if (step<min)
			step = min;
		
		return step;
	}
}
