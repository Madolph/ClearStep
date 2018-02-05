package prediction;

public class PredictorHoltWinters 	extends 
									Predictor
									implements
									PredictorInterface
{

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
	
	public float lastTime;
	
	public PredictorHoltWinters()
	{
		phi = 0.8f;
		alpha = 0.8f;
		gamma = 0.8f;
	}
	
	@Override
	public float predict(float value, float time)
	{
		value = adjustvalue(value, time);
		
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
		//normTrend *= 10;
		return normTrend;
	}
	
	public float adjustvalue(float value, float time) 
	{
		System.out.println("last time was: "+lastTime);
		value = value/(time-lastTime);
		lastTime = time;
		System.out.println("time is: "+lastTime);
		return value;
	}

	/**
	 * pastes some values to fields in the abstract superclass so they can be accessed from outside
	 */
	public void setPlotValues()
	{
		average = SN;
		prediction = normTrend;
		value=EN;
	}
	
	/**
	 * computes the current trend of the series and then divides it by the the mean of the last
	 * and the current series-level to acquire a relative trend
	 * 
	 * @return
	 */
	public float computeNormTrend()
	{
		if ((SP+SN)/2==0)
			normTrend=0;
		else
			normTrend = TN/Math.abs((SP+SN)/2);
		
		System.out.println("Trend is: "+TN+" normalized to: "+normTrend);
		normTrend *= 3;
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
