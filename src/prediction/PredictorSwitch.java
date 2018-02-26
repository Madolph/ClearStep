package prediction;

public class PredictorSwitch extends Predictor{

	public float baseline;
	
	@Override
	public float predict(float value, float time) 
	{
		checkAndUpdateBaseline(value);
		return 0;
	}

	private void checkAndUpdateBaseline(float value) 
	{
		
	}

	@Override
	public void setPlotValues() 
	{
		// TODO Auto-generated method stub
	}

}
