/*
*归并排序 
*2018/10/8 by leauqeaan 
*/
#include <stdio.h>


/*
*数组中两个有序的部分重新排序 
**/ 
void merge(int *a ,int start ,int center,int end){
	int k = 0 ;
	int i = start ;
	int j = center+1 ;
	int b[end-start+1] ;
	while(i<=center&&j<=end){
		if(a[i] < a[j] ){
			b[k] = a[i];
			k++;
			i++;	
		}else if(a[i] >= a[j]){
			b[k] = a[j] ;
			k++ ;
			j++ ;
		}
	}	
	
	if(i!=center+1){
		while(i<=center){
			b[k] = a[i];
			k++;
			i++;
		}
	} 
	else if(j!=end+1){
		while(j<=end){
			b[k] = a[j];
			k++;
			j++;
		}
	} 
	i = start ;
	k = 0 ;
	while(i<=end){
		a[i] = b[k] ;
		k++;
		i++;
	}
}


/**
*递归把数组分成多个小块 
**/ 
void mergeSort(int *a,int start ,int end) {
	
	if(start < end){ 
		mergeSort(a,start,(start+end)/2);
		mergeSort(a, (start+end)/2+1,end );
		merge(a,start,(start+end)/2,end); 
	} 

} 

int main(){
	int a[] = { 1,3,5,6,7,4,3,8 } ;
	//merge(a,0,1,3); 
    mergeSort(a,0,7);
    int i = 0 ;
    for(i=0;i<8;i++){
    	printf("%d ",a[i]);
	}  
    return 0;
} 

