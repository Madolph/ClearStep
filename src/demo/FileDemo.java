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

		lLocalFileStackSource.setLocation( new File("D:/workspace/Microscope/ClearStep") , "timelapse");

		lLocalFileStackSource.update();
		
		int timepoints = (int) lLocalFileStackSource.getNumberOfStacks();
		
		for (int i=0;i<timepoints;i++)
		{
			float timestep = 60f;
			Stepper.processImage((ClearCLImage) lLocalFileStackSource.getStack(0), i*timestep, timestep);
		}
		
		lLocalFileStackSource.close();
	}
}
