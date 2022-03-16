/* File:
 * x.c
 * 
 * Purpose:
 * Computes the sum of the values in each row of an m by n matrix A and stores
 * it in an array x. 
 * 
 * Compile:
 * gcc -g -Wall -o x -c x.c
 * 
 * Run:
 *    ./x <thread_num> <m> <n>
 *
 * Input:
 * thread_num: number of threads, m: number of rows, n: number of columns
 *
 * Output:
 * With DEBUG flag: vector x
 * Elapsted time for the computation.
 */

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>

/* Functions */
void get_args(int argc, char* argv[], int* m_p, int* n_p);
void usage();
void gen_matrix(int** A, int m, int n);

/*---------------------------------------------------------------------------
 * Function: get_args 
 * Purpose: Assign command line args 
 * In: argc, argv 
 * Out: m_p, n_p
 */
void get_args(int argc, char* argv[], int* m_p, int* n_p) {

	if (argc != 3) usage(argv[0]);
	*m_p = strtol(argv[1], NULL, 10);
	*n_p = strtol(argv[2], NULL, 10);
	if (*m_p <= 0 || *n_p <= 0) usage(argv[0]);
} /* get_args */

/*---------------------------------------------------------------------------
 * Function: usage 
 * Purpose: print a message showing what command line arguments are needed 
 * In: prog_name 
 * Out: 
 */
void usage(char *prog_name){
	fprintf(stderr, "Usage: %s <m> <n>\n", prog_name);
	exit(0);
} /* usage */

/*---------------------------------------------------------------------------
 * Function: gen_matrix 
 * Purpose: Assign values to the matrix  
 * In: A, m, n 
 * Out: A
 */
void gen_matrix(int** A, int m, int n) {
	int i, j;
	for(i = 0; i < m; i++) {
		for(j = 0; j < n; j++) {
			A[i][j] = i; 
			#ifdef DEBUG 
				fprintf(stderr,"A[%d][%d] = %p = %d \n", i, j, &A[i][j], A[i][j]); 
			#endif
		}
	}
}

/*---------------------------------------------------------------------------
 * Function: clear 
 * Purpose: Free memory 
 * In: A, x 
 * Out: A, x
 */
void clear(int** A, int* x, int m) {
	int i;
	for(i = 0; i < m; i++)
		free(A[i]);
	free(A);
	free(x);

}

/*---------------------------------------------------------------------------
 * Function: adding_column_wise 
 * Purpose: Calculate the sum of the values of each column column per column 
 * In: A, m, n
 * Out: x
 */
void adding_column_values(int** A, int m, int n, int* x) {
	int i, j;
	for(j = 0; j < n; j++) {
		x[j] = 0;
		for(i = 0; i < m; i++) {
			x[j] += A[i][j];
		}
	}
}

void add_col_val(int** A, int m, int n, int *x) {
	int i, j;
	for(i = 0; i < n; i++) {
		x[i] = 0;
	}

	for(i = 0; i < m; i++) {
		for(j = 0; j < n; j++) {
			x[j] += A[i][j];
		}
	}

}

/*---------------------------------------------------------------------------
 * Function: main 
 * Purpose: Define data structures, call and time the chosen function 
 * In: argc, argv 
 * Out: 
 */
int main(int argc, char* argv[]) {
	clock_t start, end;
	clock_t s, e;
	int		i, m, n; 
	int**		A = NULL;	
	int*		x = NULL;	
	
	get_args(argc, argv, &m, &n);

	x = calloc(n,sizeof(int)); /*malloc(n,sizeof(int)) + memset(A,0,n*sizeof(int))*/
	A = (int**) malloc(m*sizeof(int *));
	for (i = 0; i < m; i++) {
		A[i] = malloc(n*sizeof(int));
		#ifdef DEBUG 
			fprintf(stderr,"Allocate memory for row A[%d] at address %p", i, &A[i]); 
			fprintf(stderr,"  (x[%d]= %d)\n", i, x[i]); 
		#endif
	}

	if ((A != NULL) && (x != NULL)) {
		printf("Setting up the Matrix...\n");
		gen_matrix(A, m, n);
			start = clock();
			add_col_val(A, m, n, x);	
			end = clock();
			s = clock();
			adding_column_values(A, m, n, x);
			e = clock();
			printf ("Adding column values using original method took %.3lf seconds.\n", difftime(e, s) / CLOCKS_PER_SEC);
			printf ("Adding column values using new method took %.3lf seconds.\n", difftime(end, start) / CLOCKS_PER_SEC);
	} else {
		fprintf(stderr,"Could not allocate memory for the matrix or the vector\n");
	}

	clear(A, x, m);
	return 0;
}
