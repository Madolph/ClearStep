package demo;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcontrol.stack.ContiguousOffHeapPlanarStackFactory;
import clearcontrol.stack.StackInterface;
import clearcontrol.stack.StackRequest;
import clearcontrol.stack.sourcesink.source.RawFileStackSource;
import coremem.recycling.BasicRecycler;
import framework.Handler;

public class FileDemo {

	@Test
	public void testWithFile() throws IOException
	{
		Handler Stepper = new Handler(null, ImageChannelDataType.Float);
		
		final ContiguousOffHeapPlanarStackFactory lOffHeapPlanarStackFactory =
                new ContiguousOffHeapPlanarStackFactory();

		final BasicRecycler<StackInterface, StackRequest> lStackRecycler =
            new BasicRecycler<StackInterface, StackRequest>(lOffHeapPlanarStackFactory, 10);

		RawFileStackSource lLocalFileStackSource =
				new RawFileStackSource(lStackRecycler);

		lLocalFileStackSource.setLocation(new File("/Volumes/myersspimdata/XScope"), "2017-08-09-17-18-17-27-ZFishRun4On09.08.17");

		lLocalFileStackSource.update();
		
		ClearCLImage image = (ClearCLImage) lLocalFileStackSource.getStack(0);
		
		Stepper.processImage(image, 0, 60);
		
		lLocalFileStackSource.close();
	}
}
