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
	
	/**
	 * Computed trend divided by the series level
	 */
	public float normTrend;
	
	/**
	 * Series-level-new
	 */
	public float SN;
	
	/**
	 * Series-level-past
	 */
	public float SP;
	
	/**
	 * true if this isn't the first run
	 */
	boolean running = false;
	
	/**
	 * true if a trend has already been computed
	 */
	boolean trend = false;
	
	/**
	 * coefficient trend damping
	 */
	public float phi;
	
	/**
	 * weight coefficient for series level
	 */
	public float alpha;
	
	/**
	 * weight coefficient for trend
	 */
	public float gamma;
	
	/**
	 * saves the time the predictor was previously called
	 */
	public float lastTime;
	
	/**
	 * creates the predictor and initiates all the coefficients
	 */
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
	
	/**
	 * divides the value receives by the time span between this and the
	 * last image
	 * 
	 * @param value the value received
	 * @param time the current time
	 * @return the adjusted value
	 */
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
		value1 = SN;
		value2 = normTrend;
		value3 = EN;
	}
	
	/**
	 * computes a trend relative to the current series level
	 * 
	 * @return a relative trend
	 */
	public float computeNormTrend()
	{
		if ((SP+SN)/2==0)
			normTrend=0;
		else
			normTrend = TN/Math.abs((SP+SN)/2);
		
		System.out.println("Trend is: "+TN+" normalized to: "+normTrend);
		//normTrend *= 3;
		return normTrend;
	}
	
	/**
	 * performs the first run
	 * 
	 * @param value the given value
	 */
	public void initialRun(float value)
	{
		SN=EN;
		TN=0;
		normTrend=0;
	}
	
	/**
	 * initializes the trend
	 */
	public void setTrend()
	{
		TN=EN-SP;
		TP=TN;
	}
	
	/**
	 * computes a new series level from an old series level and the recent value
	 */
	public void computeSeriesLevel()
	{
		SN=alpha*EN+(1-alpha)*(SP+phi*TP);
	}
	
	/**
	 * computes a trend from the old trend and the recent series level
	 */
	public void computeTrend()
	{
		TN=gamma*(SN-SP)+(1-gamma)*phi*TP;
	}
	
	/** 
	 * receive a value to store and process it
	 * 
	 * @param value the value to be processed
	 */
	public void saveEntry(float value)
	{
		EN=value;
	}
	
	/**
	 * sets all relevant present value to be viewed as past values for the next run
	 */
	public void shiftValues()
	{
		SP=SN;
		TP=TN;
	}
}
