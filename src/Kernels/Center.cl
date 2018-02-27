
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
void ScanPlaneX (__read_only image3d_t image,
            __global    float*    arrayX,
            int planeDimY,
            int planeDimZ) 
{
  const int x       = get_global_id(0);
  const int y       = get_global_id(1);
  const int z       = get_global_id(2);
  
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;
  
  float sum = 0;
  
  const int4 origin = (int4){x,y,z};
  
  for(int ly=0; ly<planeDimX; ly++)
  {
    for(int lz=0; lz<planeDimY; lz++)
    {
      const int4 pos = origin + (int4){0,ly,lz,0};
     
      float value = read_imagef(image, sampler, pos).x;

      sum = sum + value;
      }
    }
  }
  
  const int index = x;
  
  arrayX[index] = sum;
}

__kernel
void ScanPlaneY (__read_only image3d_t image,
            __global    float*    arrayY,
            int planeDimX,
            int planeDimZ) 
{
  const int x       = get_global_id(0);
  const int y       = get_global_id(1);
  const int z       = get_global_id(2);
  
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;
  
  float sum = 0;
  
  const int4 origin = (int4){x,y,z};
  
  for(int lx=0; lx<planeDimX; lx++)
  {
    for(int lz=0; lz<planeDimZ; lz++)
    {
      const int4 pos = origin + (int4){lx,0,lz,0};
     
      float value = read_imagef(image, sampler, pos).x;

      sum = sum + value;
      }
    }
  }
  
  const int index = x;
  
  arrayX[index] = sum;
}

__kernel
void ScanPlaneZ (__read_only image3d_t image,
            __global    float*    arrayZ,
            int planeDimX,
            int planeDimY) 
{
  const int x       = get_global_id(0);
  const int y       = get_global_id(1);
  const int z       = get_global_id(2);
  
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;
  
  float sum = 0;
  
  const int4 origin = (int4){x,y,z};
  
  for(int lx=0; lx<planeDimX; lx++)
  {
    for(int ly=0; ly<planeDimY; ly++)
    {
      const int4 pos = origin + (int4){lx,ly,0,0};
     
      float value = read_imagef(image, sampler, pos).x;

      sum = sum + value;
      }
    }
  }
  
  const int index = z;
  
  arrayZ[index] = sum;
}

