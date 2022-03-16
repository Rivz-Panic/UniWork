/**
 * @file loader.c
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <omp.h>

#include "syntax.h"
#include "loader.h"

struct transaction **transactionArray;
int progNum;

void initialise_Array(int num) {
  	transactionArray = malloc(sizeof(struct transaction)*num);
    progNum=0;
}

void inc_progNum() {
  progNum++;
}

struct transaction *get_transaction(int pos) {
  return transactionArray[pos];
}
/*
 * Creates a deposit transaction and adds
 * it to a linked list of transactions
 */
void load_deposit(int type, int dest, double amount) {
 /* TODO: Implement */
 struct transaction *deposit = malloc(sizeof(struct transaction));
 deposit->type = type;
 deposit->dest = dest;
 deposit->amount = amount;
 deposit->next = NULL;
 struct transaction *check = transactionArray[progNum];
 if(check != NULL) {
   while ((check->next)!=NULL) {
     check = check->next;
   }
   check->next = deposit;
} else {
  transactionArray[progNum] = deposit;
}
}

/*
 * Creates a withdrawal transaction and adds
 * it to a linked list of transactions
 */
void load_withdrawal(int type, int src, double amount) {
 /* TODO: Implement */
 struct transaction *withdrawal = malloc(sizeof(struct transaction));
 withdrawal->type = type;
 withdrawal->src = src;
 withdrawal->amount = amount;
 withdrawal->next = NULL;
 struct transaction *check = transactionArray[progNum];
 if(check != NULL) {
   while ((check->next)!=NULL) {
     check = check->next;
   }
   check->next = withdrawal;
 } else {
   transactionArray[progNum] = withdrawal;
 }
}

/*
 * Creates a transfer transaction and adds
 * it to a linked list of transactions
 */
void load_transfer(int type, int src, int dest, double amount) {
 /* TODO: Implement */
 struct transaction *transfer = malloc(sizeof(struct transaction));
 transfer->type = type;
 transfer->src = src;
 transfer->dest = dest;
 transfer->amount = amount;
 transfer->next = NULL;
 struct transaction *check = transactionArray[progNum];
 if(check != NULL) {
   while ((check->next)!=NULL) {
     check = check->next;
   }
   check->next = transfer;
} else {
  transactionArray[progNum] = transfer;
  }
}

/*
 * Creates a balance transaction and adds
 * it to a linked list of transactions
 */
void load_balance(int type, int src) {
 /* TODO: Implement */
 struct transaction *balance = malloc(sizeof(struct transaction));
 balance->type = type;
 balance->src = src;
 balance->next = NULL;
 struct transaction *check = transactionArray[progNum];
 if(check != NULL) {
   while ((check->next)!=NULL) {
     check = check->next;
   }
   check->next = balance;
 } else {
   transactionArray[progNum] = balance;
 }
}

/*
 * Frees all the transactions after termination.
 */

void dealloc_transactions() {
}

void dealloc_transaction(struct transaction *i) {

}
