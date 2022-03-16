/**
 * @file manager.c
 */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "manager.h"

#define QUANTUM 1
#define TRUE 1
#define FALSE 0

void process_release();
void process_request();

void process_send_message(struct processControlBlock *pcb,
    struct instruction *instruct, struct mailbox *mail);
void process_receive_message(struct processControlBlock *pcb,
    struct instruction *instruct, struct mailbox *mail);

int acquire_resource(char *name, struct resourceList *resource, struct processControlBlock *pcb);
int release_resource(char *name, struct resourceList *resource, struct processControlBlock *pcb);
void add_resource_to_process(struct resourceList *resource, struct processControlBlock *pcb);
void release_resource_from_process(struct resourceList *resource, struct processControlBlock *pcb);
void set_process_ready();
void set_process_waiting();
void set_process_terminated();
int processes_finished(struct processControlBlock *pcb);
int processes_deadlocked();
void process_to_terminateq(struct processControlBlock *pcb);

/**
 * @brief Schedules each instruction of each process in a round-robin fashion.
 * The number of instruction to execute for each process is governed by the
 * QUANTUM variable.
 *
 * @param pcb The process control block which contains the current process as
 * well as a pointer to the next process control block.
 * @param resource The list of resources available to the system.
 * @param mail The list of mailboxes available to the system.
 */
void schedule_processes(struct processControlBlock *pcb,  struct resourceList *resource, struct mailbox *mail, int type){
  struct processControlBlock *temppcb = pcb;
  struct instruction *tempins;
  if (type == 0) {
    while (temppcb != NULL) {
      tempins = temppcb->nextInstruction;
      while (tempins != NULL) {
        switch (tempins->type) {
          case 0: acquire_resource(tempins->resource, resource, temppcb);
          break;
          case 1: release_resource(tempins->resource, resource, temppcb);
          break;
          case 2: process_send_message(temppcb, tempins, mail);
          break;
          case 3: process_receive_message(temppcb, tempins, mail);
          break;
          default: fprintf(stderr, "Error\n");
          break;
        }
        tempins = tempins->next;
      }
      process_to_terminateq(temppcb);
      temppcb = temppcb->next;
    }
  } else if (type == 1) {
    temppcb = pcb;
    int rr = 0;
    int c = 0;
    while (processes_finished(pcb) == FALSE) {
      tempins = temppcb->nextInstruction;
      if (tempins == NULL && temppcb->processState != 4) {
        process_to_terminateq(temppcb);
      }
      if (temppcb->processState != 4){
        temppcb->processState = 1;
        while (rr < QUANTUM) {
          if (temppcb->processState == 4) {
            break;
          }
          if (tempins != NULL) {
            c = 0;
            switch (tempins->type) {
              case 0: acquire_resource(tempins->resource, resource, temppcb);
              break;
              case 1: release_resource(tempins->resource, resource, temppcb);
              break;
              case 2: process_send_message(temppcb, tempins, mail);
              break;
              case 3: process_receive_message(temppcb, tempins, mail);
              break;
              default: fprintf(stderr, "Error\n");
              break;
            }
          }
          if (tempins->next == NULL && temppcb->processState != 4) {
            process_to_terminateq(temppcb);
          } else if (temppcb->processState != 3) {
            tempins = tempins->next;
            temppcb->nextInstruction = tempins;
          } else {
            break;
          }
          rr++;
        }
      }
      if (temppcb->next == NULL) {
        temppcb = pcb;
        c++;
      } else {
        temppcb = temppcb->next;
      }
      rr = 0;
      if (c > 1) {
        fprintf(stderr, "Deadlock has occured\n");
      }
    }
  }
}

/**
 * @brief Handles the request resource instruction.
 *
 * Executes the request instruction for the process. The function loops
 * through the list of resources and acquires the resource if it is available.
 * If the resource is not available the process sits in the waiting queue and
 * tries to acquire the resource on the next cycle.
 *
 * @param current The current process for which the resource must be acquired.
 * @param instruct The instruction which requests the resource.
 * @param resource The list of resources.
 */

/*
TODO:
Implement function
void process_request(...)
*/

/**
 * @brief Handles the release resource instruction.
 *
 * Executes the release instruction for the process. If the resource can be
 * released the process is ready for next execution. If the resource can not
 * be released the process sits in the waiting queue.
 *
 * @param current The process which releases the resource.
 * @param instruct The instruction to release the resource.
 * @param resource The list of resources.
 */
/*
TODO: *
Implement function
void process_release(...)
*/

/**
 * @brief Sends the message the prescribed mailbox.
 *
 * Sends the message specified in the instruction of the current process, to
 * the mailbox specified in the instruction. The function gets the pointer to
 * the first mailbox and loops through all the mailboxes to find the one to
 * which the message must be sent.
 *
 * @param pcb The current process which instruct us to send a message.
 * @param instruct The current send instruction which contains the message.
 * @param mail The list of available mailboxes.
 */
void process_send_message(struct processControlBlock *pcb,
    struct instruction *instruct, struct mailbox *mail) {

  struct mailbox *currentMbox;

  pcb->processState = RUNNING;

  currentMbox = mail;
  do {
    if ( strcmp(currentMbox->name, instruct->resource) == 0 ) {
      /* We found the mailbox in which a message should be left */
      break;
    }
    currentMbox = currentMbox->next;
  } while ( currentMbox != NULL );

  printf("%s sending message to mailbox %s which says \033[22;31m %s \033[0m\n",
      pcb->pagePtr->name, currentMbox->name, instruct->msg);

  currentMbox->msg = instruct->msg;
  pcb->nextInstruction = pcb->nextInstruction->next;
  dealloc_instruction(instruct);
}

/**
 * @brief Retrieves the message from the mailbox specified in the instruction
 * and stores it in the instruction message field.
 *
 * The function loops through the available mailboxes and finds the mailbox
 * from which the message must be retrieved. The retrieved message is stored
 * in the message field of the instruction of the process.
 *
 * @param pcb The current process which requests a message retrieval.
 * @param instruct The instruction to retrieve a message from a specific
 * mailbox.
 * @param mail The list of mailboxes.
 */
void process_receive_message(struct processControlBlock *pcb,
    struct instruction *instruct, struct mailbox *mail) {

  struct mailbox *currentMbox;

  pcb->processState = RUNNING;

  currentMbox = mail;
  do {
    if ( strcmp(currentMbox->name, instruct->resource) == 0 ) {
      /* We found the mailbox from which a message must be read. */
      break;
    }
    currentMbox = currentMbox->next;
  } while ( currentMbox != NULL );

  printf("%s retrieved message from mailbox %s which says \033[22;32m %s \033[0m\n",
      pcb->pagePtr->name, currentMbox->name, currentMbox->msg);

  instruct->msg = currentMbox->msg;
  currentMbox->msg = NULL;
  pcb->nextInstruction = pcb->nextInstruction->next;
  dealloc_instruction(instruct);
}

/**
 * @brief Acquires the resource specified by resourceName.
 *
 * The function iterates over the list of resources trying to acquire the
 * resource specified by resourceName. If the resources is available, the
 * process acquires the resource. The resource is indicated as not available
 * in the resourceList and 1 is returned indicating that the resource has been
 * acquired successfully.
 *
 * @param resourceName The name of the resource to acquire.
 * @param resources The list of resources.
 * @param p The process which acquires the resource.
 *
 * @return 1 for TRUE if the resource is available. 0 for FALSE if the resource
 * is not available.
 */

int acquire_resource(char *name, struct resourceList *resource, struct processControlBlock *pcb) {
  struct resourceList *temprec = resource;
  fprintf(stderr, "%s req %s: ", pcb->pagePtr->name, name);
  while (temprec != NULL) {
    if (strcmp(name, temprec->name) == 0 && temprec->available == TRUE) {
      temprec->available = FALSE;
      add_resource_to_process(temprec, pcb);
      fprintf(stderr, "acquired; Available: ");
      struct resourceList *iterate = get_available_resources();
      while (iterate != NULL) {
        if (iterate->available == 1) {
          fprintf(stderr, "%s ",iterate->name);
        }
        iterate = iterate->next;
      }
      fprintf(stderr, "\n");
      return 1;
    } else if (strcmp(temprec->name, name) == 0 && temprec->available == FALSE) {
      pcb->processState = 3;
      fprintf(stderr, "waiting;\n");
    }
    temprec = temprec->next;
  }
  return 0;
}

/**
 * @brief Releases the resource specified by resourceName
 *
 * Iterates over the resourceList finding the resource which must be set to
 * available again. The resource is then released from the process.
 *
 * @param resourceName The name of the resource to release.
 * @param resources The list of available resources.
 * @param p The current process.
 *
 * @return 1 (TRUE) if the resource was released succesfully else 0 (FALSE).
 */

int release_resource(char *name, struct resourceList *resource, struct processControlBlock *pcb){
  struct resourceList *temprec = resource;
  fprintf(stderr, "%s rel %s: ", pcb->pagePtr->name, name);
  while (temprec != NULL) {
    if (strcmp(name, temprec->name) == 0) {
      temprec->available = TRUE;
      release_resource_from_process(temprec, pcb);
      fprintf(stderr, "released; Available: ");
      struct resourceList *iterate = get_available_resources();
      while (iterate != NULL) {
        if (iterate->available == 1) {
          fprintf(stderr, "%s ",iterate->name);
        }
        iterate = iterate->next;
      }
      fprintf(stderr, "\n");
      return 1;
    }
    temprec = temprec->next;
  }
  fprintf(stderr, "Error, nothing to release\n");
  return 0;
}

/**
 * @brief Adds the specified resource to the process acquired resource list.
 *
 * After the resource has succesfully been required by the process. This
 * function is called and adds the resource to the list of resources currently
 * held by this process.
 *
 * @param current The process to which the resource must be added.
 * @param resource The resource to add to the process.
 */

void add_resource_to_process(struct resourceList *resource, struct processControlBlock *pcb){
  struct resourceList *temprec;
  temprec = malloc(sizeof(struct resourceList));
  temprec->name = resource->name;
  temprec->available = resource->available;
  temprec->next = pcb->resourceListPtr;
  pcb->resourceListPtr = temprec;
  pcb->processState = 1;
}

/**
 * @brief Release the specified resource from the process acquired list.
 *
 * The function releases the specified resource from the current process
 * acquired list.
 *
 * @param current The current process from which the resource must be released.
 * @param resource The resource to release.
 */

void release_resource_from_process(struct resourceList *resource, struct processControlBlock *pcb) {
  struct resourceList *temprec;
  for (temprec = pcb->resourceListPtr; temprec != NULL; temprec = temprec->next) {
    if (resource == temprec->next) {
      struct resourceList *new;
      new = temprec->next;
      temprec->next = new->next;
      free(new);
    }
  }
}

/**
 * @brief Add process (with id proc) to readyQueue
 *
 * @param proc The process which must be set to ready.
 */
/*
TODO:
Implement function
void process_to_readyq(...)
*/

/**
 * @brief Add process (with id proc) to the waitingQueue
 *
 * @param proc The process which must be set to waiting.
 */
/*
TODO:
Implement function
void process_to_waitingq(...)
*/

/**
 * @brief Add process (with id proc) to the terminatedQueue
 *
 * @param proc The process which must be set to waiting.
 */

/*
TODO:
Implement function*/
void process_to_terminateq(struct processControlBlock *pcb) {
  struct processControlBlock *temppcb = pcb;
  temppcb->processState = 4;
  fprintf(stderr, "%s terminated\n", temppcb->pagePtr->name);
}


/**
 * @brief Iterates over each of the loaded processes and checks if it has been
 * terminated.
 *
 * Iterates over the processes to determine if they have terminated.
 *
 * @param firstPCB A pointer to start of all the processes.
 *
 * @return 1 (TRUE) if all the processes are terminated else 0 (FALSE).
 */

/*
TODO:
Implement function*/
int processes_finished(struct processControlBlock *first) {
  struct processControlBlock *temppcb = first;
  while (temppcb != NULL) {
    if (temppcb->processState != 4) {
      return FALSE;
    }
    temppcb = temppcb->next;
  }
  return TRUE;
}

/**
 * @brief Detects deadlock.
 *
 * This function implements a deadlock detection algorithm.
 *
 * @param firstPCB A pointer to the start of all the processes.
 *
 * @return 1 (TRUE) if all the processes are in the waiting state else
 * 0 (FALSE).
 */

/*
TODO:
Implement function
int processes_deadlocked(...)
*/
