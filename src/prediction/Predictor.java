package prediction;

public abstract class Predictor {
	
	public float value;
	public float prediction;
	public float average;
	
	public abstract float predict(float value, float time);
	
	public void setPlotValues()
	{
		
	}

}
