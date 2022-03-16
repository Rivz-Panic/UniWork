/**
 * @file manager.h
 */
#ifndef _MANAGER_H
#define _MANAGER_H

#include "loader.h"

void schedule_processes(struct processControlBlock *pcb,
    struct resourceList *resource, struct mailbox *mail, int type);

#endif
