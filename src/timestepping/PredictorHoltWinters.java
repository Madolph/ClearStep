package timestepping;

public class PredictorHoltWinters extends Predictor {

	/**
	 * Entry-new
	 */
	public float EN;
	
	/**
	 * Trend-new
	 */
	public float TN;
	
	/**
	 * Trend-past
	 */
	public float TP;
	
	public float normTrend;
	
	/**
	 * Series-level-new
	 */
	public float SN;
	
	/**
	 * Series-level-past
	 */
	public float SP;
	
	public float AN;
	
	public float AP;
	
	public float BN;
	
	public float BP;
	
	boolean running = false;
	
	boolean trend = false;
	
	public float phi;
	
	public float alpha;
	
	public float gamma;
	
	public PredictorHoltWinters()
	{
		phi = 0.8f;
		alpha = 0.8f;
		gamma = 0.5f;
	}
	
	public float predict(float value)
	{
		saveEntry(value);
		
		if (!running && !trend)
		{
			initialRun(value);
			running = true;
		}
		else if (running && !trend)
			{
				setTrend();
				computeSeriesLevel();
				trend = true;
				computeNormTrend();
			}
			else
			{
				computeSeriesLevel();
				computeTrend();
				computeNormTrend();
			}
		
		shiftValues();
		setPlotValues();
		return normTrend;
	}
	
	public void setPlotValues()
	{
		value = SN;
		prediction = TN;
	}
	
	public float computeNormTrend()
	{
		if ((SP+SN)/2==0)
			normTrend=0;
		else
			normTrend = TN/((SP+SN)/2);
		
		System.out.println("Trend is: "+TN+" normalized to: "+normTrend);
		return normTrend;
	}
	
	public void initialRun(float value)
	{
		SN=EN;
		TN=0;
		normTrend=0;
	}
	
	public void setTrend()
	{
		TN=EN-SP;
		TP=TN;
	}
	
	public void computeSeriesLevel()
	{
		SN=alpha*EN+(1-alpha)*(SP+phi*TP);
	}
	
	public void computeTrend()
	{
		TN=gamma*(SN-SP)+(1-gamma)*phi*TP;
	}
	
	public void saveEntry(float value)
	{
		EN=value;
	}
	
	public void shiftValues()
	{
		SP=SN;
		TP=TN;
	}
}
