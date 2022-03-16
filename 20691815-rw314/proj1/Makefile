# sample Makefile.

COMPILER ?= $(GCC_PATH)gcc

#FLAGS ?= -std=c++0x -O3 -Wall $(GCC_SUPPFLAGS)
FLAGS ?= -O2 -Wall -Wno-variadic-macros -pedantic -g $(GCC_SUPPFLAGS)

LDFLAGS ?= -g 
LDLIBS = -lm
#if using Intel® Threading Building Blocks:
#LDLIBS = -ltbb -ltbbmalloc

EXECUTABLE = my_executable

SRCS=$(wildcard src/*.c)
OBJS=$(SRCS:src/%.c=obj/%.o)

all: release

release: $(OBJS)
	$(COMPILER) $(LDFLAGS) -o $(EXECUTABLE) $(OBJS) $(LDLIBS) 

obj/%.o: src/%.c
	$(COMPILER) $(FLAGS) -o $@ -c $<

clean:
	rm -f obj/*.o
	rm ${EXECUTABLE} 

cleandata:
	rm cachegrind.out.*

dist-clean: clean
	rm -f $(EXECUTABLE) *~ .depend *.zip
	
#automatically handle include dependencies
#depend: .depend
#
#.depend: $(SRCS)
#	rm -f ./.depend
#	@$(foreach SRC, $(SRCS), $(COMPILER) $(FLAGS) -MT $(SRC:src/%.c=obj/%.o) -MM $(SRC) >> .depend;)

include .depend	
