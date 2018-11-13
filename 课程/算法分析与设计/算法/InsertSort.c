/**
*²åÈëÅÅĞò  
* 2018/10/8 by leauqeaan
*/

#include <stdio.h>


void insertSort(int *a , int start , int end){
	
	int i = start + 1; 
	int k = start ;
	while(i<=end){
		k = start ;
		while(k < i ){
			if(a[k] > a[i] ){
				int tmp = a[i];
				int m = i ;
				while(m > k){ 
					a[m] = a[m-1]; 
					m-- ;
				}
				a[k] = tmp ; 
				break;
			}
			k++ ;  
		}
		i++; 
	}  
} 


int main(){
	
	int a[] = { 1,3,5,6,7,4,3,8 } ; 
    insertSort(a,0,7);
    int i = 0 ;
    for(i=0;i<8;i++){
    	printf("%d ",a[i]);
	}  
    return 0;
} 
