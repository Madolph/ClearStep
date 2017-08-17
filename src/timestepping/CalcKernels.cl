
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

//default buffersum p=0f
__kernel 
void buffersum(         const float p,
        		            __global const float *a,
        		            __global const float *b,
        		            __global       float *c)
{
	int x = get_global_id(0);
	
	c[x] = a[x] + b[x] + p * CONSTANT;
	
	//if(x%100000==1)
	//  printf("this is a test string c[%d] = %f + %f + %f = %f \n", x, a[x], b[x], p,  c[x]);
 
}

// A kernel to fill an image with beautiful garbage:
//default fillimagexor dx=0i
//default fillimagexor dy=0i
//default fillimagexor u=0f
__kernel 
void fillimagexor(__write_only image3d_t image, int dx, int dy, float u)
{
	int x = get_global_id(0); 
	int y = get_global_id(1);
	int z = get_global_id(2);
	
	write_imagef (image, (int4)(x, y, z, 0), u*((x+dx)^((y+dy)+1)^(z+2)));
}

__kernel 
void xorsphere   (__write_only image3d_t image,
                                        int       cx,
                                        int       cy,
                                        int       cz,
                                        float     r)
{
  const int width  = get_image_width(image);
  const int height = get_image_height(image);
  const int depth  = get_image_depth(image);
  
  float4 dim = (float4){width,height,depth,1};
  
  int x = get_global_id(0); 
  int y = get_global_id(1);
  int z = get_global_id(2);
  
  float4 pos = (float4){x,y,z,0};
  
  float4 cen = (float4){cx,cy,cz,0};
  
  float d = fast_length((pos-cen)/dim);
  
  float value = (float)( (100.0f*pow(fabs(r-d),0.5f))*((d<r)?1:0) );
  
  write_imagef (image, (int4){x,y,z,0}, value);
}

__kernel 
void compare(__read_only image3d_t image1, 
					  __read_only image3d_t image2, 
					  __write_only image3d_t result,
					  float threshold)
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
	
	float min = 999999;
	float max = 0;
	
	float val = val1.x-val2.x;
	if (val<0)
		val = -(val);
		
	//assign positive difference to the grid
	//(val1.x>val2.x)?
	//	val = val1.x-val2.x:
	//	val = val2.x-val1.x;
	
	if (val>max)
		max = val;
	if (val<min)
		min = val;
		
	float4 res = (float4){val,0,0,0};
	
	threshold = min+((max-min)/10);
	
	write_imagef(result, pos, res);
}	



__kernel 
void handleNoise (__read_only image3d_t image,
							__write_only image3d_t clean,
							float thresh)
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
  
  const int4 origin = (int4){x*blockwidth,y*blockheight,z*blockdepth,0};
  
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_MIRRORED_REPEAT | CLK_FILTER_NEAREST;
  
  int link;
  
  float4 noise = (float4){0,0,0,0};
  
  for(int lz=0; lz<blockwidth; lz++)
  {
    for(int ly=0; ly<blockheight; ly++)
    {
      for(int lx=0; lx<blockdepth; lx++)
      {
      	const int4 pos = origin + (int4){lx,ly,lz,0};
      	
      	for(int q=0;q<3;q++)
      	{
      		for(int w=0;w<3;w++)
      		{
      			for(int e=0;e<3;e++)
      			{
      				int4 shiftpos = origin + (int4){lx-1+e,ly-1+w,lz-1+q,0};
      				float4 linkval = read_imagef(image, sampler, shiftpos);
      				(linkval>thresh)?
      					link++:
      					link;			
      			}
      		}
      	}
      	
      	float4 val = read_imagef(image, pos);
      	
      	(val.x>thresh && link>9)?
      		write_imagef(clean, pos, val):
      		write_imagef(clean, pos, noise);
      		
      }
    }
  }
}


__kernel
void SumSquareRoot3D (__read_only image3d_t image,
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
     
        float value = read_imagef(image, pos).x;

        sum = sum + value;
      }
    }
  }
  
  const int index = x+nblocksx*y+nblocksx*nblocksy*z;
  
  result[index] = sum;
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
     
        float value = read_imagef(image, pos).x;

        sum = sum + value;
      }
    }
  }
  
  const int index = x+nblocksx*y+nblocksx*nblocksy*z;
  
  result[index] = sum;
}