package imagecreator;

import java.io.IOException;
import org.junit.Test;

public class Executor {

	@Test
	public void Test(String[] args) throws InterruptedException, IOException{
		TimeStepper Stepper= new TimeStepper(1,6);
		Stepper.assignCache();
		Stepper.scanstacks();
		float step = Stepper.computeStep();
		for (int i=1;i<Stepper.Stacks.Cache.length+1;i++)
			{ System.out.println("Change"+ i +" is calculated as: " + Stepper.Stacks.Cache[i-1]); }
		System.out.println("resulting timestep is: " + step);
	}
	
	@Test
	public void Test1() throws InterruptedException, IOException
	{
		TimeStepper Stepper = new TimeStepper(1,6);
		Stepper.lApp.createContinuousPics(0, 0, 0, 0, 0, 0, 0, true);
	}
}


