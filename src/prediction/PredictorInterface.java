package prediction;

public interface PredictorInterface {

	public float predict(float value, float time);
	
	public void setPlotValues();
}
