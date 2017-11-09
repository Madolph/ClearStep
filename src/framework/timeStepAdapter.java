package framework;

import clearcl.ClearCLContext;
import clearcl.ClearCLImage;

public interface timeStepAdapter {

	public void processImage(ClearCLImage image, float time);
	
	public boolean checkInitialization();
	
	public void passContext(ClearCLContext Context);
	
	
}
