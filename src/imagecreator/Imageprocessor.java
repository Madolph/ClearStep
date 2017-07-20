package imagecreator;

import java.io.IOException;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.ocllib.OCLlib;
import clearcl.test.ClearCLBasicTests;
import clearcl.viewer.ClearCLImageViewer;
import coremem.offheap.OffHeapMemory;

public class Imageprocessor {

	public static void main(String[] args) throws InterruptedException, IOException
	{
		Demo Test = new Demo();
		
		Test.demoViewImage3DF();
	}
}
