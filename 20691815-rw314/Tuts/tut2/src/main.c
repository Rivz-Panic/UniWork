#include <stdio.h>
#include <stdlib.h>
#include <omp.h>
#include <math.h>
#define SEED 35791246
#define FALSE  1
#define TRUE  0

void Hello(void);
static int isprime(int n);
static int countprimenumbers(int n);
double pie(double max);
static int thread_count;
struct drand48_data randBuffer;

int main(int argc, char** argv) {
	//int threadCount = omp_get_thread_count();
	thread_count = strtol(argv[1], NULL, 10);
	#pragma omp parallel num_threads(thread_count)
	Hello();
	isprime(1);
	countprimenumbers(1000);

	return 0;
}

void Hello(void) {
	int my_rank = omp_get_thread_num();
	int thread_count = omp_get_num_threads();
	printf("Hello from thread %d of %d\n", my_rank, thread_count);
}

static int isprime(int n) {
	int i;
	for(i=2;i<=(int)(sqrt((double) n));i++) {
		if (n%i==0) {
		return FALSE;
		}
	}
	return n>1;
}

static int countprimenumbers(int n) {
	int count = 0;
	#pragma omp parallel for num_threads(thread_count)
	for(int i = n; i > 0; i--) {
		if(isprime(n)==TRUE) {
			#pragma omp atomic
			count++;
		}
	}
	return count;
}

double pie (double max) {
	int hits =0;
	int id =1;
	#pragma omp parallel for num_threads(thread_count)
	for(int i = 0; i < (int)max; i = i+1) {
		srand48_r(SEED^id, &randBuffer);
		double x;
		drand48_r(&randBuffer, &x);
		double y;
		drand48_r(&randBuffer, &y);
		double dist = sqrt(x*x + y*y);
		if (dist <= 1) {
			#pragma omp atomic
			hits++;
		}
	}
	return 4.0*hits/max;
}
