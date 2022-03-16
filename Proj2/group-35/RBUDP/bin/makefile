
JAVAC=javac

EXECUTABLE = RBUDP

sources = $(wildcard *.java)
classes = $(sources:.java=.class)

all: $(classes)

clean :
	rm -f *.class
	rm -f ${EXECUTABLE}

%.class : %.java
	$(JAVAC) $<
