/*H**********************************************************************
 *
 *	This is a skeleton to guide development of Othello engines to be used
 *	with the Cloud Tournament Engine (CTE). CTE runs tournaments between
 *	game engines and displays the results on a web page which is available on
 *	campus (!!!INSERT URL!!!). Any output produced by your implementations will
 *	be available for download on the web page.
 *
 *	The socket communication required for DTE is handled in the main method,
 *	which is provided with the skeleton. All socket communication is performed
 *	at rank 0.
 *
 *	Board co-ordinates for moves start at the top left corner of the board i.e.
 *	if your engine wishes to place a piece at the top left corner, the "gen_move"
 *	function must return "00".
 *
 *	The match is played by making alternating calls to each engine's "gen_move"
 *	and "play_move" functions. The progression of a match is as follows:
 *		1. Call gen_move for black player
 *		2. Call play_move for white player, providing the black player's move
 *		3. Call gen move for white player
 *		4. Call play_move for black player, providing the white player's move
 *		.
 *		.
 *		.
 *		N. A player makes the final move and "game_over" is called for both players
 *	
 *	IMPORTANT NOTE:
 *		Any output that you would like to see (for debugging purposes) needs
 *		to be written to file. This can be done using file FP, and fprintf(),
 *		don't forget to flush the stream. 
 *		I would suggest writing a method to make this
 *		easier, I'll leave that to you.
 *		The file name is passed as argv[4], feel free to change to whatever suits you.
 *H***********************************************************************/

#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<sys/socket.h>
#include<arpa/inet.h>
#include<mpi.h>
#include<time.h>

const int EMPTY = 0;
const int BLACK = 1;
const int WHITE = 2;
const int OUTER = 3;
const int ALLDIRECTIONS[8]={-11, -10, -9, -1, 1, 9, 10, 11};
const int BOARDSIZE=100;

char* gen_move();
void play_move(int * curBoard, char *move);
void game_over();
void run_worker(int rank);
void initialise();

int* initialboard(void);
int *legalmoves (int * curBoard, int player);
int legalp (int * curBoard, int move, int player);
int validp (int move);
int wouldflip (int * curBoard, int move, int dir, int player);
int opponent (int player);
int findbracketingpiece(int * curBoard, int square, int dir, int player);
void makemove (int * curBoard, int move, int player);
void makeflips (int * curBoard, int move, int dir, int player);
int get_loc(char* movestring);
char* get_move_string(int loc);
void printboard();
char nameof(int piece);
int count (int player, int * board);
int minimaxDecision();
int minimaxValue(int * board, int player, int curPlayer,  int depth, int alpha,
		int beta);
int heuristic(int player, int * board);
void copyBoard(int * board, int * dup);
	
int my_colour;
int time_limit;
int running;
int rank;
int size;
int *board;
int firstrun = 1;
FILE *fp;
int main(int argc , char *argv[]) {
    int socket_desc, port, msg_len;
    char *ip, *cmd, *opponent_move, *my_move;
    char msg_buf[15], len_buf[2];
    struct sockaddr_in server;
    ip = argv[1];
    port = atoi(argv[2]);
    time_limit = atoi(argv[3]);
    my_colour = EMPTY;
    running = 1;
    /* starts MPI */
    MPI_Init(&argc, &argv);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);	/* get current process id */
    MPI_Comm_size(MPI_COMM_WORLD, &size);	/* get number of processes */
    // Rank 0 is responsible for handling communication with the server
    if (rank == 0){
        fp = fopen(argv[4], "w");
        fprintf(fp, "This is an example of output written to file.\n");
        fflush(fp);
        initialise();
        socket_desc = socket(AF_INET , SOCK_STREAM , 0);
        if (socket_desc == -1) {
            fprintf(fp, "Could not create socket\n");
            fflush(fp);
            return -1;
        }
        server.sin_addr.s_addr = inet_addr(ip);
        server.sin_family = AF_INET;
        server.sin_port = htons(port);

        //Connect to remote server
        if (connect(socket_desc , (struct sockaddr *)&server , sizeof(server)) < 0){

            fprintf(fp, "Connect error\n");
            fflush(fp);
            return -1;
        }
        fprintf(fp, "Connected\n");
        fflush(fp);
        if (socket_desc == -1){
            return 1;
        }
        while (running == 1){
            if (firstrun ==1) {
                char tempColour[1];
                if(recv(socket_desc, tempColour , 1, 0) < 0){
                    fprintf(fp,"Receive failed\n");
                    fflush(fp);
                    running = 0;
                    break;
                }
                my_colour = atoi(tempColour);
                fprintf(fp,"Player colour is: %d\n", my_colour);
                fflush(fp);
                firstrun = 2;
            }


            if(recv(socket_desc, len_buf , 2, 0) < 0){


                fprintf(fp,"Receive failed\n");
                fflush(fp);
                running = 0;
                break;
            }

            msg_len = atoi(len_buf);


            if(recv(socket_desc, msg_buf, msg_len, 0) < 0){
                fprintf(fp,"Receive failed\n");
                fflush(fp);
                running = 0;
                break;
            }


            msg_buf[msg_len] = '\0';
            cmd = strtok(msg_buf, " ");

            if (strcmp(cmd, "game_over") == 0){
                running = 0;
                fprintf(fp, "Game over\n");
                fflush(fp);
                break;

            } else if (strcmp(cmd, "gen_move") == 0){
                my_move = gen_move();
                if (send(socket_desc, my_move, strlen(my_move) , 0) < 0){
                    running = 0;
                    fprintf(fp,"Move send failed\n");
                    fflush(fp);
                    break;
                }
                printboard();
            } else if (strcmp(cmd, "play_move") == 0){
                opponent_move = strtok(NULL, " ");
                play_move(board, opponent_move);
                printboard();

            }
            memset(len_buf, 0, 2);
            memset(msg_buf, 0, 15);
        }
        game_over();
    } else {
        run_worker(rank);
        MPI_Finalize();
    }
    return 0;
}

/*
	Called at the start of execution on all ranks
 */
void initialise(){
    int i;
    running = 1;
    board = (int *)malloc(BOARDSIZE * sizeof(int));
    for (i = 0; i<=9; i++) board[i]=OUTER;
    for (i = 10; i<=89; i++) {
        if (i%10 >= 1 && i%10 <= 8) board[i]=EMPTY; else board[i]=OUTER;
    }
    for (i = 90; i<=99; i++) board[i]=OUTER;
    board[44]=WHITE; board[45]=BLACK; board[54]=BLACK; board[55]=WHITE;
}

/*
	Called at the start of execution on all ranks except for rank 0.
	This is where messages are passed between workers to guide the search.
 */
void run_worker(int rank){

}

/*
	Called when your engine is required to make a move. It must return
	a string of the form "xy", where x and y represent the row and
	column where your piece is placed, respectively.

	play_move will not be called for your own engine's moves, so you
	must apply the move generated here to any relevant data structures
	before returning.
 */
char* gen_move(){
    int loc;
    char* move;
    if (my_colour == EMPTY){
        my_colour = BLACK;
    }
	loc = minimaxDecision();
    if (loc == -1){
        move = "pass\n";
    } else {
        move = get_move_string(loc);
        makemove(board, loc, my_colour);
    }
    return move;
}

/*
	Called when the other engine has made a move. The move is given in a
	string parameter of the form "xy", where x and y represent the row
	and column where the opponent's piece is placed, respectively.
 */
void play_move(int * curBoard, char *move){
    int loc;
    if (my_colour == EMPTY){
        my_colour = WHITE;
    }
    if (strcmp(move, "pass") == 0){
        return;
    }
    loc = get_loc(move);
    makemove(curBoard, loc, opponent(my_colour));
}

/*
	Called when the match is over.
 */
void game_over(){
    MPI_Finalize();
}

char* get_move_string(int loc){
    static char ms[3];
    int row, col, new_loc;
    new_loc = loc - (9 + 2 * (loc / 10));
    row = new_loc / 8;
    col = new_loc % 8;
    ms[0] = row + '0';
    ms[1] = col + '0';
    ms[2] = '\n';
    return ms;
}

int get_loc(char* movestring){
    int row, col;
	row = movestring[0] - '0';
    col = movestring[1] - '0';
    return (10 * (row + 1)) + col + 1;
}

/*
 	Gets a list of legal moves available to the 
	player 
 */
int *legalmoves (int * curBoard, int player) {
    int move, i, *moves;
    moves = (int *)malloc(65 * sizeof(int));
    moves[0] = 0;
    i = 0;
    for (move=11; move<=88; move++)
        if (legalp(curBoard, move, player)) {
            i++;
            moves[i]=move;
        }
    moves[0]=i;
    return moves;
}

/*
 	Checks if the move is legal 
 */
int legalp (int * curBoard, int move, int player) {
    int i;
    if (!validp(move)) return 0;
    if (curBoard[move]==EMPTY) {
        i=0;
        while (i<=7 && !wouldflip(curBoard, move, ALLDIRECTIONS[i], player)) i++;
        if (i==8) return 0; else return 1;
    }
    else return 0;
}

/*
 	Checks if move is on the board
 */
int validp (int move) {
    if ((move >= 11) && (move <= 88) && (move%10 >= 1) && (move%10 <= 8))
        return 1;
    else return 0;
}

int wouldflip (int * curBoard, int move, int dir, int player) {
    int c;
    c = move + dir;
    if (curBoard[c] == opponent(player))
        return findbracketingpiece(curBoard, c+dir, dir, player);
    else return 0;
}

int findbracketingpiece(int * curBoard, int square, int dir, int player) {
    while (curBoard[square] == opponent(player)) square = square + dir;
    if (curBoard[square] == player) return square;
    else return 0;
}
/*
   Switches opponent
 */
int opponent (int player) {
    switch (player) {
        case 1: return 2;
        case 2: return 1;
        default: printf("illegal player\n"); return 0;
    }
}
/*
  Starts minimax search algorithm	
 */
int minimaxDecision(){
	int * moves = legalmoves(board, my_colour);
	if(moves[0] == 0){
		free(moves);
		return -1;
	} else {
		int bestMoveVal = -99999;
		int bestMove = moves[1];
		for(int i = 1; i <= moves[0]; i++){
			int * tempBoard = (int *)malloc(BOARDSIZE * sizeof(int));
			copyBoard(board, tempBoard);
			makemove(tempBoard, moves[i], my_colour);
			int val = minimaxValue(tempBoard, my_colour, my_colour, 1, 999999,
					-999999);
			if(val > bestMoveVal){
				bestMoveVal = val;
				bestMove = moves[i];
			}
		}
		free(moves);
		return bestMove;
	}
	
}
/*
 	Does minimax search as well as alpha-beta pruning
 */
int minimaxValue(int * curBoard, int player, int curPlayer,  int depth, int	alpha, int beta){
	if((depth == 32)){
		return heuristic(player, curBoard);
	}

	int * moves = legalmoves(curBoard, player);
	if(moves[0] == 0){
		return minimaxValue(curBoard, player, opponent(curPlayer), (depth+1), alpha, beta);
	} else {
		if(curPlayer == player){
			int bestMoveVal = -99999;
			for(int i = 1; i <= moves[0]; i++){
				int * tempBoard = (int *)malloc(BOARDSIZE * sizeof(int));
				copyBoard(board, tempBoard);
				makemove(tempBoard, moves[i], player);
				int val = minimaxValue(tempBoard, player, opponent(curPlayer),
						depth+1, alpha, beta);
				if(val > bestMoveVal){
					bestMoveVal = val;
				}
				if(val > alpha){
					alpha = val;
				}
				if(beta <= alpha){
					break;
				}
			}
			return bestMoveVal;
		} else {
			int bestMoveVal = 99999;
			for(int i = 1; i <= moves[0]; i++){
				int * tempBoard = (int *)malloc(BOARDSIZE * sizeof(int));
				copyBoard(board, tempBoard);
				makemove(tempBoard, moves[i], player);
				int val = minimaxValue(tempBoard, player, opponent(curPlayer),
						depth+1, alpha, beta);
				if(val < bestMoveVal){
					bestMoveVal = val;
				}
				if(val < beta){
					beta = val;
				}
				if (beta <= alpha){
					break;
				}
			}
			return bestMoveVal;
		}
	}
	return -1;
}

/*
	Makes the move	
 */
void makemove (int * curBoard,int move, int player) {
    int i;
    curBoard[move] = player;
    for (i=0; i<=7; i++) makeflips(curBoard, move, ALLDIRECTIONS[i], player);
}

void makeflips (int * curBoard, int move, int dir, int player) {
    int bracketer, c;
    bracketer = wouldflip(curBoard, move, dir, player);
    if (bracketer) {
        c = move + dir;
        do {
            curBoard[c] = player;
            c = c + dir;
        } while (c != bracketer);
    }
}

void printboard(){
    int row, col;
    fprintf(fp,"   1 2 3 4 5 6 7 8 [%c=%d %c=%d]\n",
            nameof(BLACK), count(BLACK, board), nameof(WHITE), count(WHITE, board));
    for (row=1; row<=8; row++) {
        fprintf(fp,"%d  ", row);
        for (col=1; col<=8; col++)
            fprintf(fp,"%c ", nameof(board[col + (10 * row)]));
        fprintf(fp,"\n");
    }
    fflush(fp);
}


char nameof (int piece) {
    static char piecenames[5] = ".bw?";
    return(piecenames[piece]);
}

int count (int player, int * curBoard) {
    int i, cnt;
    cnt=0;
    for (i=1; i<=88; i++)
        if (curBoard[i] == player) cnt++;
    return cnt;
}

/*
 	returns the value of the trees 
 */
int heuristic(int player, int * board){
	int my_score = count(player, board);
	int opp_score = count(opponent(player), board);
	return (my_score-opp_score);
}

/*
 	Makes a copy of the board
 */
void copyBoard(int * board, int * tempBoard){
	memcpy(tempBoard, board, BOARDSIZE * sizeof(int));
}
