/* vim settings: se ts=4
 * File: bank.c
 *
 *          An account, Account holders, Deposit and Withdrawal transactions
 *
 * Run: . run.sh
 *
 * Input:
 *
 * Output:
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "loader.h"
#include "parser.h"
#include <omp.h>

#define MAX_ACCS 100

static void get_args(int argc, char* argv[], int* thread_count_p);
static void usage(char* prog_name);
static void deposit(int acc_num, double amount, double* balance);
static void withdrawal(int acc_num, double amount, double* balance);
static void transfer(int acc1, int acc2, double amount);
static void acc_balance(int acc_num, double* balance);
static void do_transation(int i);
void transactions_user1();
void transactions_user2();


typedef struct account {
	int acc_num;
	double balance;
} acc_t;

acc_t* all_accounts;

/*------------------------------------------------------------------*/
int main(int argc, char *argv[])
{
	int thread_count;
	char* fname;
 	fname = malloc(sizeof(char)*64);

	get_args(argc, argv, &thread_count);

	initialise_Array(thread_count);

	all_accounts = (acc_t*) malloc(MAX_ACCS*sizeof(acc_t));
 	if (all_accounts == NULL) {
		fprintf(stderr,"Memory could not be allocated, exiting\n");
		exit(0);
	}

	for (int i = 2; i < argc; i++) {
		parse_transaction_file(argv[i]);
		inc_progNum();
	}

	{
	#pragma omp parallel for num_threads(thread_count)
	for (int i = 0; i < thread_count; i++) {
		//int rank = omp_get_thread_num();
		//printf("%i\n",rank);
		do_transation(i);
		printf("--- Transaction batch completed ---\n");
	}
}

/*TODO: Start Parallel Block */

	/*TODO: Read in the transaction list for each thread from it's transaction file:
		use parse_transaction_file(fname) */

	/*TODO: Replace the following test-code with code that will ensure
		that each thread executes the transactions read in from it's
		transaction file. */

	/* start test
	printf("Executing the transactions of all users in serial\n");
	transactions_user1();
	transactions_user2();
	 end test */

/*TODO: End Parallel Block*/

	return 0;
}

static void do_transation(int i) {
	struct transaction *list;
	list = get_transaction(i);
	double balance;
	while(list!=NULL) {
		printf("User %i, ", i);
		switch (list->type) {
			case 0:
				printf("Depos %f, ", list->amount);
				deposit(list->dest, list->amount, &balance);
				break;
			case 1:
				printf("Withd %f, ", list->amount);
				withdrawal(list->src, list->amount, &balance);
				break;
			case 2:
				printf("Trans %f, ", list->amount);
				transfer(list->src, list->dest, list->amount);
				break;
			case 3:
				printf("Balan		, ");
				acc_balance(list->src, &balance);
				break;
			default:
				printf("unknown type\n");
				break;
		}
		list = list->next;
		printf("\n");
	}
}

/*------------------------------------------------------------------
 * @brief  get_args
 *            Get command line args
 * In args:   argc, argv
 * Out args:  thread_count_p, m_p, n_p
 */
static void get_args(int argc, char* argv[], int* thread_count_p)  {

	//if (argc != 3) usage(argv[0]);
	*thread_count_p = strtol(argv[1], NULL, 10);
	if (*thread_count_p <= 0) usage(argv[0]);

}  /* get_args */

/*------------------------------------------------------------------
 * @brief  usage
 *            print a message showing what the command line should
 *            be, and terminate
 * In arg :   prog_name
 */
static void usage (char* prog_name) {

	/*TODO: Update if needed*/
	fprintf(stderr, "usage: %s <thread_count> <datafile.txt>\n", prog_name);
	exit(0);
}  /* usage */

/*--------------------------------------------------------------------
 * @brief deposit
 *        Add amount to balance
 * @param acc_num: Account number
 * @param amount:  Amount to deposit
 * @param balance: Balance of acc_num
 */
static void deposit(int acc_num, double amount, double* balance)
{
	#pragma omp atomic
	all_accounts[acc_num].balance += amount;
	printf("To Acc %i (Bal %f), --Successful, Acc %i ", acc_num, *balance, acc_num);
	*balance = all_accounts[acc_num].balance;
	acc_balance(acc_num, balance);
}

/*--------------------------------------------------------------------
 * @brief withdrawal
 *        If amount available, subtract amount from balance
 * @param acc_num: Account Number
 * @param amount:  Amount to withdraw
 * @param balance: Balance of acc_num
 */
static void withdrawal(int acc_num, double amount, double* balance)
{
	if (amount <= all_accounts[acc_num].balance) {
		#pragma omp atomic
		all_accounts[acc_num].balance -= amount;
		printf("Fr Acc %i (Bal %f), --Successful, Acc %i ", acc_num, *balance, acc_num);
		*balance = all_accounts[acc_num].balance;
		acc_balance(acc_num, balance);
	} else {
		printf("Fr Acc %i (Bal %f), --Failed: Insuficient Funds ", acc_num, *balance);
	}
}

/*--------------------------------------------------------------------
 * @brief transfer
 *           If amount available in acc1,
 *           subtract amount from acc1 balance and add to acc2 balance
 * @param acc1:    Number of account from which money is transferred
 * @param acc2:    Number of account to which money is transferred
 * @param amount:  Amount to transfer
 * @param balance: Balance of acc1
 */
static void transfer(int acc1, int acc2, double amount)
{
	printf("Fr Acc %i (Bal %f), To Acc %i (Bal %f)", acc1, all_accounts[acc1].balance, acc2, all_accounts[acc2].balance);
	if (amount <= all_accounts[acc1].balance) {
		#pragma omp atomic
		all_accounts[acc1].balance -= amount;
		all_accounts[acc2].balance += amount;
		printf("--Successful, Acc %i (Bal %f)", acc2, all_accounts[acc2].balance);
	} else {
		printf("--Failed: Insuficient Funds");
	}
}

/*--------------------------------------------------------------------
 * @brief balance
 *           Return the current balance of account acc_num
 * @param acc_num: The number of the account
 * @param balance: The current balance of account acc_num
 */
static void acc_balance(int acc_num, double* balance)
{
	*balance = all_accounts[acc_num].balance;
	printf("(Bal %f)", *balance);
}


/*--------------------------------------------------------------------
 * @brief transactions_user1
 *           List of transactions for user1
 *           User1's account number is 0.
 *
 */
void transactions_user1()
{
	double balance;

	deposit(0, 100, &balance);
	deposit(0, 200, &balance);
	deposit(0, 300, &balance);
	withdrawal(0, 200, &balance);
	withdrawal(0, 100, &balance);
	withdrawal(0, 50, &balance);
	transfer(1, 0, 200);
	acc_balance(0, &balance);
	printf("Account 0 balance after completion of transaction batch: %f\n", balance);
}

/*--------------------------------------------------------------------
 * @brief transactions_user2
 *           List of transactions for user2
 *           User2's account number is 0.
 *
 */
void transactions_user2()
{
	double balance;

	deposit(0, 400, &balance);
	withdrawal(0, 200, &balance);
	deposit(0, 100, &balance);
	withdrawal(0, 50, &balance);
	withdrawal(0, 150, &balance);
	deposit(0, 300, &balance);
	transfer(0, 1, 300);
	acc_balance(1, &balance);
	printf("Account 1 balance after completion of transaction batch: %f\n", balance);
}
