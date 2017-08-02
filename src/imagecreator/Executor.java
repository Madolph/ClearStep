package imagecreator;

import java.io.IOException;

public class Executor {

	public static void main(String[] args) throws InterruptedException, IOException{
		TimeStepper Stepper= new TimeStepper(1,6);
		Stepper.assignCache();
		Stepper.scanstacks();
		float step = Stepper.computeStep();
		System.out.println(step);
	}
}
