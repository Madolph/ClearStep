package demo;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import framework.Handler;
import plotting.PlotterXY;
import simulation.Simulator;
import toolset.CenterDetector;

public class Demo {

	@Test
	public void testCenterDetector() throws IOException
	{
		ImageChannelDataType Datatype = ImageChannelDataType.Float;
		
		Handler lHandler = new Handler(null, Datatype);
		
		Simulator lSim = new Simulator(Datatype, lHandler.mContext);
		
		CenterDetector lCenterCalc = new CenterDetector(lHandler.mContext);
		
		PlotterXY Plotter = new PlotterXY(3);
		String[] Titles = new String[3];
		Titles[0] = "centerX";
		Titles[1] = "centerY";
		Titles[2] = "centerZ";
		Plotter.initializePlotter(lHandler.mFxOn, "Flummi-Demo", "Plot", "time", Titles, 1000, 1000);
		
		int lSize = 256;
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(Datatype, lSize, lSize, lSize);
		ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage, "Flummi");

		float time=0;
		float[] data = new float[3];
		float Duration = 120;
		
		while (time<(Duration*1000))  
		{
			lSim.generatePic(time, lImage, lSize, true);
			lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());
			
			float[] center = lCenterCalc.detectCenter(lImage);
			
			data[0] = center[0];
				
			data[1] = center[1];
				
			data[2] = center[2];
				
			Plotter.plotFullDataSetXY(time, data);
		}  
		
		lViewImage.waitWhileShowing();
	}
	
	@Test
	public void runtimeTest() throws IOException, InterruptedException
	{
		ImageChannelDataType Datatype = ImageChannelDataType.Float;
		//ImageChannelDataType Datatype = ImageChannelDataType.Float;
		
		Handler lHandler = new Handler(null, Datatype);
		
		int lSizex = 256;
		int lSizey = 256;
		int lSizez = 256;
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(Datatype, lSizex, lSizey, lSizez);
		float[][][] lImage2 = new float[lSizex+2][lSizey+2][lSizez+2];
		  
		lImage.fill(1f, false, false);
		
		for (int x=0;x<lImage2.length;x++)
			for (int y=0;y<lImage2[1].length;y++)
				for (int z=0;z<lImage2[1][1].length;z++)
					lImage2[x][y][z]=1f;
		
		lHandler.mCalc.convert(lImage);
		
		lHandler.mCalc.mImage1=lHandler.mContext.createSingleChannelImage(Datatype, lSizex, lSizey, lSizez);
		
		long runTime = System.currentTimeMillis();
		lHandler.mCalc.cleanNoise(1);
		runTime = runTime-System.currentTimeMillis();
		System.out.println("---------->runTime1 is:"+runTime*-1);
		
		runTime = System.currentTimeMillis();
		
		for (int x=1;x<lImage2.length-1;x++)
			for (int y=1;y<lImage2[1].length-1;y++)
				for (int z=1;z<lImage2[1][1].length-1;z++)
				{					
					//allocate value to be used later	
					float ceil = 0;
					
					//Array of all neighboring values
					float[] matrix = new float[27];
					
					//iterating over the 3x3x3-space around the pixel	
					for (int d3=0; d3<3; d3++)
					{
						for (int d2=0; d2<3; d2++)
						{
							for (int d1=0; d1<3; d1++)
							{
								//we use an ordered array of 27 instead of a 3D-matrix
								int index = d1+d2*3+d3*9;
								//read the pixel
								matrix[index] = lImage2[x-1+d1][y-1+d2][z-1+d3]-lImage2[x][y][z];
					
								//save the highest deviation
								if (Math.abs(matrix[index])>ceil)
									{ ceil = matrix[index]; }
							}
						}
					}
					
					//array of all values chosen for final calculation	
					float[] close = new float[15];
						
					//initialize every value to ceil to force them to be overwritten
					for (int c=0;c<15;c++)
						{ close[c] = ceil; }
					
					//check every value and put the smallest(absolute) into the final array	
					for (int c=0;c<15;c++)
					{
						//allocate a value to memorize the smallest value found
						int mem = 0;
						//find the smallest value
						for (int i=0;i<27;i++)
						{
							if (Math.abs(matrix[i])<close[c])
							{
								close[c]=matrix[i];
								//store the index of the currently smallest value
								mem = i;
							}
						}
						//knock out the coppied element by setting it to ceil
						matrix[mem] = ceil;
					}
					
					//initialize the final result	
					float res = 0;
					
					//sum of the final array
					for (int c=0;c<15;c++)
						{ res = res+close[c]; }
					
					//calculate the mean
					lImage2[x][y][z] += (res/15);
				}
		
		runTime = runTime-System.currentTimeMillis();
		System.out.println("---------->runTime2 is:"+runTime*-1);
		
		
		
		
	}
	
	@Test
	public void noiseTest() throws IOException, InterruptedException
	{
		ImageChannelDataType Datatype = ImageChannelDataType.Float;
		//ImageChannelDataType Datatype = ImageChannelDataType.Float;
		
		Handler lHandler = new Handler(null, Datatype);
		
		Simulator lSim = new Simulator(Datatype, lHandler.mContext);
		
		/*PlotterXY Plotter = new PlotterXY(1);
		String[] Titles = new String[1];
		Titles[0] = "Root-Square-Error";
		Plotter.initializePlotter(lHandler.mFxOn, "Noise-demo", "No Handling", "time [s]", Titles, 1000, 1000);*/
		
		int lSize = 256;
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(Datatype, lSize, lSize, lSize);

		float time=0;
		float Duration = 120;
		
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("NoiseData.txt"), "utf-8"))) 
		{
			writer.write("Noise-Handling-results");
		}
		
		String metrics = new String();
		
		while (time<(Duration*1000))  
		{ 
			float step = 1000;
			System.out.println("current time is: "+time);
			  
			lSim.generatePic(time%9000, lImage, lSize, true);
			
			lHandler.mCalc.convert(lImage);
			lHandler.mCalc.cachePic();
			
			float result = 0;
			
			if (lHandler.mCalc.filled)
			{
				//long runTime = System.currentTimeMillis();
				result = lHandler.mCalc.compareImages();
				//runTime = runTime-System.currentTimeMillis();
				//System.out.println("---------->runTime is:"+runTime*-1);
				metrics=metrics+(result+" ");
			}
			
			//if (Float.isNaN(result))
				//result = 0;
				
			
			System.out.println("------------>"+result+"<------------");
			
			
			
			/*if (lHandler.mCalc.filled)
			{
				Plotter.plotSingleDataXY(time, result, 0);
			}*/ 
			
			time += step;
			Thread.sleep((long) step);
		}
		
		List<String> lines = Arrays.asList(metrics);
		Path file = Paths.get("NoiseData.txt");
		Files.write(file, lines, Charset.forName("UTF-8"));
		
		int u = 4;
		while (u == 4)
		{
			Thread.sleep((long) 9999999);
		}
	}
}

