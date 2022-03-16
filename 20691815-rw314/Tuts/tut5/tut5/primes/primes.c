/* Simple example C program to calculate density of primes up
** to a particular value
*/

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <omp.h>
#include <time.h>

#define TRUE (1==1)
#define FALSE (1==0)
#define DEBUG FALSE

static int isprime(int n)
{
  int i; 
  for(i=2;i<=(int)(sqrt((double) n));i++)
    if (n%i==0) return FALSE;
  
  return n>1;
  
}

int main(int argc, char *argv[])
{

  int i, nprimes, n, nt = 1;
  clock_t start, end;
  double time_start, time_end;

  if (argc != 3){
      printf("Incorrect Invocation, use: primes N <number of threads>\n");
      return 0;
  } else {
      n = atoi(argv[1]);  
      nt = atoi(argv[2]);  
  }

  if (n < 0){
      printf("N cannot be negative");
      return 0;
  }

  printf("N = %d; nt = %d\n", n, nt);
  nprimes = 0;

/* 
  start = clock();
  for(i=1;i<=n;i++) {
    if (isprime(i)){
      nprimes++;
    }
  } 

  end = clock();

  printf("Serial Time %.3lf\n",difftime(end,start)/CLOCKS_PER_SEC);
  printf("Found %d primes in range 1 to %d\n",nprimes,n);
  printf("density %.2f%%, asymptotic expectation %.2f%%\n",
         100.*(double)nprimes/n,100/(log((double)n-1.)) );


  #pragma omp parallel for 
  for(i=1;i<=n;i++) {
    if (isprime(i)){
  #pragma omp critical
      nprimes++;
    }
  } 

  end = clock();

  printf("Parallel (using critical) Time %.3lf\n",difftime(end,start)/CLOCKS_PER_SEC);
  printf("Found %d primes in range 1 to %d\n",nprimes,n);
  printf("density %.2f%%, asymptotic expectation %.2f%%\n",
         100.*(double)nprimes/n,100/(log((double)n-1.)) );
*/

  start = clock();
  #pragma omp parallel num_threads(nt)
  {
	if (omp_get_thread_num() == 0) time_start = omp_get_wtime();
	#pragma omp for reduction(+:nprimes) 
	for(i=1;i<= (n);i++) {
		if (isprime(i)){
			nprimes++;
		}
	} 
	if (omp_get_thread_num() == 0) time_end = omp_get_wtime();
  }
  end = clock();

  printf("Parallel (using reduction) Time (clock())%.3lf\n",difftime(end,start)/CLOCKS_PER_SEC);
  printf("Parallel (using reduction) Time (omp_get_wtime())%.3lf\n",time_end-time_start);
  printf("Found %d primes in range 1 to %d\n",nprimes,n);
  printf("density %.2f%%, asymptotic expectation %.2f%%\n",
         100.*(double)nprimes/n,100/(log((double)n-1.)) );



  return 0; 
}

