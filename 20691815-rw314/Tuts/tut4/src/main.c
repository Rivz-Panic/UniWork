#include <omp.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>

int main(int argc, char** argv) {
  int n = atoi(argv[2]);
  int thread_count = strtol(argv[1], NULL, 10);
  int *local_sums = calloc(thread_count, sizeof(int));

  //Version 1:
  double start1 = omp_get_wtime();
  {
  #pragma omp parallel for num_threads(thread_count)
    for (int i = 0; i < n; i++) {
      int rank = omp_get_thread_num();
      local_sums[rank] = (int) (rand() % 100);
    }
  }
  int sum = 0;
  for (int i = 0; i < thread_count; i++) {
    sum += local_sums[i];
  }
  double time1 = omp_get_wtime()-start1;
  printf("Version 1: %i\nTime 1: %e\n", sum, time1);

  //Version 2:
  double start2 = omp_get_wtime();
  int local_sum = 0;
  {
  #pragma omp parallel for num_threads(thread_count) \
  default(none) private(local_sum) shared(local_sums, n)
    //default(none) private(i, local_sum)
    for (int i = 0; i < n; i++) {
      local_sum += (int) (rand() % 100);
    }
    int rank = omp_get_thread_num();
    local_sums[rank] = local_sum;
  }

  int global_sum =0;

  for (int i = 0; i < thread_count; i++) {
    global_sum += local_sums[i];
  }
  double time2 = omp_get_wtime()-start2;
  printf("Version 2: %i\nTime 2: %e\n", global_sum, time2);

  free(local_sums);

  return 0;
}
