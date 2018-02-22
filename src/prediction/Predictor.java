package prediction;

/**
 * abstract class for every predictor
 * 
 * @author Max
 *
 */
public abstract class Predictor implements 
								PredictorInterface {
	
	/**
	 * generic slots that can be filled with values to be accessed by other classes
	 */
	public float value1, value2, value3;
	
	public abstract float predict(float value, float time);
	
	public abstract void setPlotValues();

}
