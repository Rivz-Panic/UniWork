#COMPILER ?= mpicc
COMPILER ?= mpicc

CFLAGS ?= -O2 -Wall -Wno-variadic-macros -pedantic -DDEBUG $(GCC_SUPPFLAGS)
LDFLAGS ?= -g 
LDLIBS =

EXECUTABLE = player/player

SRCS=$(wildcard src/*.c)
OBJS=$(SRCS:src/%.c=player/%.o)

all: release

release: $(OBJS)
	$(COMPILER) $(LDFLAGS) -o $(EXECUTABLE) $(OBJS) $(LDLIBS) 

player/%.o: src/%.c
	$(COMPILER) $(CFLAGS) -o $@ -c $<

clean:
	rm -f player/*.o
	rm ${EXECUTABLE} 
