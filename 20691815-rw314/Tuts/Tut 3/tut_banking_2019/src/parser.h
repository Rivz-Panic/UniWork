/**
  * @file parser.h
  * @description A definition of the structures and functions necessary to read
  *              in the transactions file, parse it and load it into the  
  *              necessary datastructures.
  */

#ifndef _PARSER_H
#define _PARSER_H

/**
 * @brief Reads in a specified file, parse it and store it in the associated
 *        data-structure.
 *
 * @param filename A string with the location of the transaction.list file for reading.
 */
void parse_transaction_file(char* filename);

#endif
