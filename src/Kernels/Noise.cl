
// You can include other resources
// Path relative to class OCLlib, the package is found automatically (first in class path if several exist)
#include [OCLlib] "linear/matrix.cl"

// You can also do absolute includes:
// Note, this is more brittle to refactoring. 
// Ideally you can move code and if the kernels 
// stay at the same location relative to the classes 
// everything is ok.
#include "clearcl/test/testinclude.cl"

// If you include something that cannot be found, 
// then it fails silently but the final source code gets annotated. 
// (check method: myprogram.getSourceCode())
#include "blu/tada.cl"

__kernel 
//a Kernel can never read and write the same image
void cleanNoise(__read_only image3d_t image1, 
			__write_only image3d_t cache)
	{
	
	//the Kernel receives it’s assigned position from the context
	int x = get_global_id(0); 
	int y = get_global_id(1);
	int z = get_global_id(2);
	
	int4 pos = (int4){x,y,z,0};
	
	//determines the behavior at the edge of the image (no neighbors)	
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_MIRRORED_REPEAT | CLK_FILTER_NEAREST;
	
	//reads the center pixel	
	float val = read_imagef(image1, pos).x;
	
	//allocate value to be used later	
	float ceil = 0;
	
	//Array of all neighboring values
	float matrix[27];
	
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
				matrix[index] = read_imagef(image1, sampler, (pos-(int4){1,1,1,0}+(int4){d1,d2,d3,0})).x;
				//store difference to center-pixel
				matrix[index] = matrix[index]-val;
	
				//save the highest deviation
				if (fabs(matrix[index])>ceil)
					{ ceil = matrix[index]; }
			}
		}
	}
	
	//array of all values chosen for final calculation	
	float close[15];
		
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
			if (fabs(matrix[i])<close[c])
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
	res = (res/15)+val;
	
	//assemble the data to put into the resulting image	
	float4 result = (float4){res,0,0,0};
	
	//write the final data	
	write_imagef(cache, pos, result);
}	