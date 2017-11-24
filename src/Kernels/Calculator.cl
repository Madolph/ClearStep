
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
void compare(__read_only image3d_t image1, 
			 __read_only image3d_t image2, 
			 __write_only image3d_t result)
{
	const int width  = get_image_width(image1);
	const int height = get_image_height(image1);
	const int depth  = get_image_depth(image1);


	int x = get_global_id(0); 
	int y = get_global_id(1);
	int z = get_global_id(2);

	int4 pos = (int4){x,y,z,0};
	
	float4 val1 = read_imagef(image1, pos);
	float4 val2 = read_imagef(image2, pos);
	
	float val = val1.x-val2.x;
		
	float4 res = (float4){(val*val),0,0,0};
	
	write_imagef(result, pos, res);
}	

__kernel
void Sum3D (__read_only image3d_t image,
            __global    float*    result) 
{
  const int width   = get_image_width(image);
  const int height  = get_image_height(image);
  const int depth   = get_image_depth(image);
  
  const int x       = get_global_id(0);
  const int y       = get_global_id(1);
  const int z       = get_global_id(2);
  
  const int nblocksx = get_global_size(0);
  const int nblocksy = get_global_size(1);
  const int nblocksz = get_global_size(2);
  
  const int blockwidth   = width/nblocksx;
  const int blockheight  = height/nblocksy;
  const int blockdepth   = depth/nblocksz;
  
  float sum = 0;
  
  const int4 origin = (int4){x*blockwidth,y*blockheight,z*blockdepth,0};
  
  for(int lz=0; lz<blockwidth; lz++)
  {
    for(int ly=0; ly<blockheight; ly++)
    {
      for(int lx=0; lx<blockdepth; lx++)
      {
        const int4 pos = origin + (int4){lx,ly,lz,0};
     
        float value = (float)READ_IMAGE(image, pos).x;

        sum = sum + value;
      }
    }
  }
  
  const int index = x+nblocksx*y+nblocksx*nblocksy*z;
  
  result[index] = sum;
}

__kernel 
void cleanNoise (__read_only image3d_t image1, 
			 	 __write_only image3d_t cache)
{
	const int width  = get_image_width(image1);
	const int height = get_image_height(image1);
	const int depth  = get_image_depth(image1);


	int x = get_global_id(0); 
	int y = get_global_id(1);
	int z = get_global_id(2);

	int4 pos = (int4){x,y,z,0};
	
	const sampler_t sampler = 
					CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_MIRRORED_REPEAT | CLK_FILTER_NEAREST;
	
	float val = (float)READ_IMAGE(image1, pos).x;
	
	float ceil = 0;
	
	float matrix [27] = {};
	
	for (int d3=0; d3<3; d3++)
	{
		for (int d2=0; d2<3; d2++)
		{
			for (int d1=0; d1<3; d1++)
			{
				if (d1!=1 || d2!=1 || d3!=1)
				{
					int index = d1+d2*3+d3*9;
					matrix [index] = (float)READ_IMAGE(image1, sampler, (pos-(int4){1,1,1,0}+(int4){d1,d2,d3,0})).x;
					matrix [index] = fabs(matrix [index] - val);
					if (matrix [index]>ceil)
					{
						//save the highest deviation
						ceil = matrix [index];
					}
				}
				else
				{ ; } //13 is the point itself
			}
		}
	}
	
	float close[13] = {};
	
	for (int c=0;c<13;c++)
	{
		// initiate every value to force it to be overwritten
		close[c] = ceil;
	}
	
	for (int c=0;c<13;c++)
	{
		int mem = 0;
		for (int i=0;i<27;i++)
		{
			if (matrix[i]<close[c])
			{
				close[c]=matrix[i];
				int mem = i;
			}
		}
		//eliminate the chosen element from being chosen again by setting it to ceil
		matrix[mem] = ceil;
	}
	
	float res = 0;
	
	for (int c=0;c<13;c++)
	{
		res = res+close[c];
	}
	res = (res/13)+val;
	
	uint4 result = (uint4){res,0,0,0};
	
	WRITE_IMAGE(cache, pos, result);
}