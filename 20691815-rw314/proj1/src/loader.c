/**
 * @file loader.c
 */

#include "loader.h"
#include "syntax.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

struct processControlBlock *ready_queue();
struct processControlBlock *waiting_queue();
struct processControlBlock *terminated_queue();

void dealloc_page(struct page *p);
void dealloc_resourceList(struct resourceList *r);
void dealloc_mailboxes();
void dealloc_queues();

void debug_process_memory();
void debug_resources();

struct processControlBlock *firstPCB = NULL;
struct processControlBlock *currentPCB = NULL;
struct processControlBlock *pcb = NULL;

struct processControlBlock *readyQueue = NULL;
struct processControlBlock *waitingQueue = NULL;
struct processControlBlock *terminatedQueue = NULL;

struct resourceList *firstResource = NULL;
struct resourceList *currentResource = NULL;
struct resourceList *resource = NULL;

struct instruction *firstInstruction = NULL;
struct instruction *currentInstruction = NULL;
struct instruction *instruct = NULL;

struct mailbox *firstMailbox = NULL;
struct mailbox *currentMailbox = NULL;
struct mailbox *mail = NULL;

char *currentProcessName = "";
int processNumber = 0;

/**
 * \brief Initialises and loads the processes specified in the process.list
 * file.
 *
 * This function initialises a new process control block for the process being
 * loaded from the process.list file. It initialises a number of pointers to
 * NULL as well as setting the processState to NEW. Furthermore the process is
 * indicated to be ready by using bit test and set to set the corresponding
 * bit to 1 in the ready queue.
 *
 * \param process_name The name of the new process to load
 */
void load_process ( char* process_name ) {
  struct page *newPage;

  newPage = malloc(sizeof(struct page));

  if ( firstPCB == NULL ) {
    firstPCB = malloc(sizeof(struct processControlBlock));
    firstPCB->pagePtr = newPage;
    firstPCB->processState = NEW;
    firstPCB->nextInstruction = NULL;
    firstPCB->processPriority = 0;
    firstPCB->resourceListPtr = NULL;
    firstPCB->next = NULL;

    currentPCB = firstPCB;
  } else {
    pcb = malloc(sizeof(struct processControlBlock));
    pcb->pagePtr = newPage;
    pcb->processState = NEW;
    pcb->nextInstruction = NULL;
    pcb->processPriority = 0;
    pcb->resourceListPtr = NULL;
    pcb->next = NULL;

    currentPCB->next = pcb;
    currentPCB = pcb;
  }
  currentPCB->pagePtr->name = process_name;
  currentPCB->pagePtr->number = processNumber;

struct processControlBlock *temp = ready_queue();
while (temp->next != NULL) {
  temp = temp->next;
}
currentPCB->processState = READY;
temp = currentPCB;

  #ifdef DEBUG
    printf("Added Process %d to the readyQueue\n",currentPCB->pagePtr->number);
  #endif

  processNumber++;

#ifdef DEBUG
  debug_process_memory();
#endif
}

/**
 * @brief Loads the mailbox from the process.list file.
 *
 * Initialises and loads the mailboxes list.
 *
 * @param mailboxName The name of the mailbox to load.
 */
void load_mailbox ( char* mailboxName ) {

  if (firstMailbox == NULL) {
    firstMailbox = malloc(sizeof(struct mailbox));
    firstMailbox->next = NULL;
    currentMailbox = firstMailbox;
  } else {
    mail = malloc(sizeof(struct mailbox));
    currentMailbox->next = mail;
    currentMailbox = mail;
  }
  currentMailbox->name = mailboxName;
  currentMailbox->next = NULL;
}

/**
 * @brief Load the resource from the process.list file.
 *
 * Initialises and loads the resource to create a resource list. The resource
 * is indicated as available and the resource name is stored.
 *
 * @param resource_name The name of the resource which is loaded.
 */
void load_resource ( char *resource_name ) {
  if (firstResource == NULL) {
    firstResource = malloc(sizeof(struct resourceList));
    currentResource = firstResource;
  } else {
    resource = malloc(sizeof(struct resourceList));
    currentResource->next = resource;
    currentResource = resource;
  }
  currentResource->name = resource_name;
  /* 1 = Available, 0 = Unavailable */
  currentResource->available = 1;
  currentResource->next = NULL;

#ifdef DEBUG
  debug_resources();
#endif
}

/**
 * @brief Loads an instruction for a process.
 *
 * The function uses the process_name to locate the process for
 * which the instruction should be loaded as well as the resource
 * on which the action is performed.
 *
 * @param process_name The name of the process for which to load the
 * instruction.
 * @param resource_name The name of the resource used in the instruction.
 * @param instruction Indicates the next request, release or message to send.
 */
void load_process_instruction( char *process_name, char *instruction,
    char *resource_name, char *msg) {

#ifdef DEBUG
  printf("In load_process_instruction for %s: %s -> %s\n", process_name,
      instruction, resource_name);
#endif

  if (strcmp(currentProcessName, process_name) != 0) {
    firstInstruction = malloc(sizeof(struct instruction));
    firstInstruction->next = NULL;
    currentInstruction = firstInstruction;
  } else {
    instruct = malloc(sizeof(struct instruction));
    instruct->next = NULL;
    currentInstruction->next = instruct;
    currentInstruction = instruct;
  }

  currentInstruction->resource = resource_name;
  if (strcmp(instruction, REQ) == 0) {
    currentInstruction->type = REQ_V;
    currentInstruction->msg = NULL;
  } else if (strcmp(instruction, REL) == 0) {
    currentInstruction->type = REL_V;
    currentInstruction->msg = NULL;
  } else if (strcmp(instruction, SEND) == 0) {
    currentInstruction->type = SEND_V;
    currentInstruction->msg = msg;
  } else if (strcmp(instruction, RECV) == 0) {
    currentInstruction->type = RECV_V;
    currentInstruction->msg = msg;
  }

  if (strcmp(currentPCB->pagePtr->name, process_name) != 0) {
    currentPCB = firstPCB;
    do {
      if(strcmp(currentPCB->pagePtr->name, process_name) == 0) {
        break;
      }
      currentPCB = currentPCB->next;
    } while(currentPCB != NULL);
    currentPCB->nextInstruction = firstInstruction;
    currentPCB->pagePtr->firstInstruction = firstInstruction;
    #ifdef DEBUG
      printf("Store a pointer to the first instruction of the process in it's page.\n");
    #endif
  }
  currentProcessName = process_name;
}

/**
 * @brief Returns a pointer to the first process in the list of loaded
 * processes.
 *
 * Returns a pointer to the first process in the list
 * of loaded processes.
 *
 * @return firstPCB Pointer to the first process control block.
 */
struct processControlBlock* get_loaded_processes() {
  return firstPCB;
}

/**
 * @brief Returns the first pointer to the available resources.
 *
 * Returns the first pointer to the available resources.
 *
 * @return firstResource Pointer to the resource list.
 */
struct resourceList* get_available_resources() {
  return firstResource;
}

/**
 * @brief Returns the first pointer to the available mailboxes.
 *
 * Returns the first pointer to the available mailboxes.
 *
 * @return firstMailbox Pointer to the mailbox list.
 */
struct mailbox* get_mailboxes() {
  return firstMailbox;
}

/**
 * @brief Returns the readyQueue
 *
 * Initialises and returns a pointer to the readyQueue. If the queue
 * has been initialised it just returns the pointer to the queue.
 *
 * @return readyQueue Pointer
 */

struct processControlBlock *ready_queue() {
  if (readyQueue == NULL) {
    readyQueue = malloc(sizeof(struct processControlBlock));
  }
  return readyQueue;
}


/**
 * @brief Returns the waitingQueue
 *
 * Initialises and returns a pointer to the waitingQueue. If the queue
 * has been initialised it just returns the pointer to the queue.
 *
 * @return waitingQueue Pointer
 */

struct processControlBlock *waiting_queue() {
  if (waitingQueue == NULL) {
    waitingQueue = malloc(sizeof(struct processControlBlock));
  }
  return waitingQueue;
}

/**
 * @brief Returns the terminatedQueue
 *
 * Initialises and returns a pointer to the terminatedQueue. If the queue
 * has been initialised it just returns the pointer to the queue.
 *
 * @return terminatedQueue Pointer
 */

struct processControlBlock *terminated_queue() {
  if (terminatedQueue == NULL) {
    terminatedQueue = malloc(sizeof(struct processControlBlock));
  }
  return terminatedQueue;
}

/**
 * @brief Frees all the memory allocated for the processes.
 *
 * Iterates over the loaded processes, starting from the first, freeing all the
 * allocated memory assigned to each process.
 */
void dealloc_processes() {
  struct processControlBlock *current;
  struct processControlBlock *next;
  struct resourceList *availableResources;

  dealloc_queues();

  current = get_loaded_processes();
  do {
    dealloc_page(current->pagePtr);
    /* Instructions are freed after they are executed */
    if (current->resourceListPtr != NULL) {
      dealloc_resourceList(current->resourceListPtr);
    }

    next = current->next;
    free(current);
    current = next;
  } while(current != NULL);

  availableResources = get_available_resources();
  dealloc_resourceList(availableResources);

  dealloc_mailboxes();
}

/**
 * @brief Frees the allocated memory for the page struct.
 *
 * Frees the name stored in the structed followed by the struct.
 */
void dealloc_page(struct page *p) {
  free(p->name);
  free(p);
}

/**
 * @brief Frees the allocated memory for the instruction struct.
 *
 * Frees the instruction struct.
 */
void dealloc_instruction(struct instruction *i) {
  if(i != NULL) {
    free(i);
  }
}

/**
 * @brief Frees the queues allocated
 *
 * Frees the readyQueue, waitingQueue and terminatedQueue allocated for the CPU
 * scheduler.
 */
void dealloc_queues() {
/* TODO: Implement function

*/
}

/**
 * @brief Frees the resources used in the system.
 *
 * Frees each of the resources which are available and used in the system.
 */
void dealloc_resourceList(struct resourceList *r) {
  struct resourceList *current;
  struct resourceList *next;

  if ( r != NULL) {
    current = r;
    do {
      next = current->next;
      free(current);
      current = next;
    } while (current != NULL);
  }
}

/**
 * @brief Frees the mailboxes used in the system.
 *
 * Free each of the mailboxes which are available and used in the system.
 */
void dealloc_mailboxes() {
  struct mailbox *current;
  struct mailbox *next;

  current = get_mailboxes();
  if(current != NULL) {
    do {
      free(current->name);
      if(current->msg != NULL) {
        free(current->msg);
      }
      next = current->next;
      free(current);
      current = next;
    } while (current != NULL);
  }
}

#ifdef DEBUG
void debug_process_memory() {
  struct processControlBlock *debug;
  debug = firstPCB;
  do {
    printf("Process name in pcb: %s\n", debug->pagePtr->name);
    debug = debug->next;
  } while(debug != NULL);
}

void debug_resources() {
  struct resourceList *debug;
  debug = firstResource;
  do {
    printf("The Resource is: %s\n", debug->name);
    debug = debug->next;
  } while(debug != NULL);
}
#endif
