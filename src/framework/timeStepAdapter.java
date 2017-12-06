package framework;

import clearcl.ClearCLImage;

public interface timeStepAdapter {

	public void processImage(ClearCLImage image, float time, float step);	
}
