# sample Makefile.

COMPILER ?= $(GCC_PATH)gcc

#FLAGS ?= -std=c++0x -O3 -Wall $(GCC_SUPPFLAGS)
FLAGS ?= -O3 -Wall -fopenmp $(GCC_SUPPFLAGS)

LDFLAGS ?= -g -fopenmp
LDLIBS =

EXECUTABLE = run

SRCS=$(wildcard src/*.c)
OBJS=$(SRCS:src/%.c=obj/%.o)

all: release

release: $(OBJS)
	$(COMPILER) $(LDFLAGS) -o $(EXECUTABLE) $(OBJS) $(LDLIBS) 

obj/%.o: src/%.c
	$(COMPILER) $(FLAGS) -o $@ -c $<

clean:
	rm -f obj/*
	rm run
dist-clean: clean
	rm -f $(EXECUTABLE) *~ .depend *.zip
	
#automatically handle include dependencies
#depend: .depend
#
#.depend: $(SRCS)
#	rm -f ./.depend
#	@$(foreach SRC, $(SRCS), $(COMPILER) $(FLAGS) -MT $(SRC:src/%.cpp=obj/%.o) -MM $(SRC) >> .depend;)

include .depend	
