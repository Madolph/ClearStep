
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
__kernel void buffersum(         const float p,
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
__kernel void fillimagexor(__write_only image3d_t image, int dx, int dy, float u)
{
	int x = get_global_id(0); 
	int y = get_global_id(1);
	int z = get_global_id(2);
	
	write_imagef (image, (int4)(x, y, z, 0), u*((x+dx)^((y+dy)+1)^(z+2)));
}

__kernel void xorsphere   (__write_only image3d_t image,
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
  
  float value = (float)( (x^y^z)*((d<r)?1:0) );
  
  write_imagef (image, (int4){x,y,z,0}, value);
}

__kernel void compare(__read_only image3d_t image1, 
					  __read_only image3d_t image2, 
					  __write_only image3d_t result)
{
	const int width  = get_image_width(image1);
  	const int height = get_image_height(image1);
  	const int depth  = get_image_depth(image1);
  
  	float4 dim = (float4){width,height,depth,1};
  
  	int x = get_global_id(0); 
  	int y = get_global_id(1);
  	int z = get_global_id(2);

	int4 pos = (int4){x,y,z,0};
	
	float4 val = read_imagef(image1, pos) - read_imagef(image2, pos);
	float4 res = val*val;
	
	write_imagef(result, pos, res);
}	

__kernel
void sumNroot3D (__read_only image3d_t info,
                       float result) 
{
  const int width   = get_image_width(info);
  const int height  = get_image_height(info);
  const int depth   = get_image_depth(info);
  
  const int x       = get_global_id(0);
  const int y       = get_global_id(1);
  const int z       = get_global_id(2);
  
  const int stridex = get_global_size(0);
  const int stridey = get_global_size(1);
  const int stridez = get_global_size(2);
  
  float sum = 0;
  
  for(int lz=z; lz<depth; lz+=stridez)
  {
    for(int ly=y; ly<height; ly+=stridey)
    {
      for(int lx=x; lx<width; lx+=stridex)
      {
        const int4 pos = {lx,ly,lz,0};
     
        float value = (float)(read_imagef(info, pos)).x;
        
        sum = sum+value;
      }
    }
  }
  
  result = sqrt(sum); 
}