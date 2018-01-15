package prediction;

public abstract class Predictor implements 
								PredictorInterface {
	
	public float value;
	public float prediction;
	public float average;
	
	public abstract float predict(float value, float time);
	
	public abstract void setPlotValues();

}
