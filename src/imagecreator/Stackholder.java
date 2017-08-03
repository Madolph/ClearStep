package imagecreator;

public class Stackholder {

	// saves the deviations between pictures
	// 0 is the newest and 9 the oldest value
	public float[] Cache = new float[10];
	
	public void register(float deviation) 
	{
		this.rearrange();
		this.Cache[0]= deviation;
	}
	
	// puts the oldest value to 0, where it will be overwritten and shifts the other values
	public void rearrange()
	{
		float temp;
		temp=Cache[0];
		Cache[0] = Cache[9];
		
		for (int i=1;i<Cache.length-1;i++)
			{
				Cache[i+1]=Cache[i];
			}
		Cache[1]=temp;	
	}
}
