FLAGS = -fopenmp -lm -O2
SOURCE_SERIAL = odd_even.c
SOURCE_P1 = omp_odd_even1.c
SOURCE_P2 = omp_odd_even2.c

all:
	gcc $(FLAGS) $(SOURCE_SERIAL) -o odd_even_s
	gcc $(FLAGS) $(SOURCE_P1) -o odd_even_1 
	gcc $(FLAGS) $(SOURCE_P2) -o odd_even_2 

clean:
	rm -f odd_even_s odd_even_1 odd_even_2

