#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <omp.h>
#include <time.h>

static int thread_count;
static int n;
static int local_sums[];
static int global_sum[];

int main(int argc, char *argv[])
{
    thread_count = atoi(argv[1]);
    n = atoi(argv[2]);
    local_sums[n];
    global_sum[thread_count];
    version1();

    #pragma omp parallel num_threads(thread_count)
    {
    version2();
    }
    int v2_sum = 0;
    for(int i =0; i<thread_count;i++) {
        v2_sum = v2_sum + global_sum[i];
    }

    
}

static void version1() {
    int ans = 0;
    #pragma omp parallel for num_threads(thread_count)
    for (int i=0;i<n;i++){
        local_sums[i] = rand();
    }

    for (int j=0;j<n;j++) {
        ans = ans + local_sums[j];
    }
    printf("version1: "+ans);
}

static void version2() {
    int private_n;
    int sum;
    if((n%thread_count)!=0) {
        if((omp_get_thread_num()%2)!=0) {
            private_n = n/thread_count;
        } else {
            private_n = n/thread_count +1;
        }
    } else {
        private_n = n/thread_count;
    }
    for(int i=0; i<private_n;i++){
        sum = sum + rand();
    }
    global_sum[omp_get_thread_num()] = sum;
}

static void version3(){}
