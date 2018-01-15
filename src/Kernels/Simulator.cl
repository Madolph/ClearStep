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
void noisySphere(__write_only image3d_t image,
               		float cx,
               		float cy,
               		float cz,
               		float r)
	{
  		const int width  = get_image_width(image);
  		const int height = get_image_height(image);
  		const int depth  = get_image_depth(image);
 
  		float n= (float) 21.47483647;
  
  		float4 dim = (float4){width,height,depth,1};
  
  		int x = get_global_id(0); 
  		int y = get_global_id(1);
  		int z = get_global_id(2);
  
  		float4 pos = (float4){x,y,z,0};
  
  		float4 cen = (float4){cx,cy,cz,0};
  
  		float d = fast_length((pos-cen)/dim);
  
  		float value = ( (100.0f*pow(fabs(r-d),0.5f))*((d<r)?1:0));
  
  		WRITE_IMAGE (image, (int4){x,y,z,0}, (DATA){value,0,0,0});
	}	