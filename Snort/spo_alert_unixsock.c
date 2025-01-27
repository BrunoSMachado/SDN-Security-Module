/* $Id$ */
/*
** Copyright (C) 2014-2018 Cisco and/or its affiliates. All rights reserved.
** Copyright (C) 2002-2013 Sourcefire, Inc.
** Copyright (C) 1998-2002 Martin Roesch <roesch@sourcefire.com>
** Copyright (C) 2000,2001 Andrew R. Baker <andrewb@uab.edu>
**
** This program is free software; you can redistribute it and/or modify
** it under the terms of the GNU General Public License Version 2 as
** published by the Free Software Foundation.  You may not use, modify or
** distribute this program under any other version of the GNU General
** Public License.
**
** This program is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU General Public License for more details.
**
** You should have received a copy of the GNU General Public License
** along with this program; if not, write to the Free Software
** Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

/* spo_alert_unixsock
 *
 * Purpose:  output plugin for Unix Socket alerting
 *
 * Arguments:  none (yet)
 *
 * Effect:	???
 *
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <string.h>
#ifdef HAVE_STRINGS_H
#include <strings.h>
#endif
#ifndef WIN32
#include <sys/un.h>
#endif /* !WIN32 */
#include <unistd.h>
#include <errno.h>

#include "sf_types.h"
#include "event.h"
#include "decode.h"
#include "plugbase.h"
#include "spo_plugbase.h"
#include "parser.h"
#include "snort_debug.h"
#include "util.h"
#include <time.h>

#include "snort.h"
#include "spo_alert_unixsock.h"
#define UNSOCK_FILE "snort_alert"



/*
 * Win32 does not support Unix sockets (sockaddr_un).  This file
 * will not be compiled on Win32 until a proper patch is supported.
 */
#ifndef WIN32




/* not used yet */
typedef struct _SpoAlertUnixSockData
{
    char *filename;

} SpoAlertUnixSockData;

extern OptTreeNode *otn_tmp;

static int alertsd;
#ifndef WIN32
static struct sockaddr_un alertaddr;
#else
static struct sockaddr_in alertaddr;
#endif

static void AlertUnixSockInit(struct _SnortConfig *, char *);
static void AlertUnixSock(Packet *, const char *, void *, Event *);
static void ParseAlertUnixSockArgs(char *);
static void AlertUnixSockCleanExit(int, void *);
static void OpenAlertSock(void);
static void CloseAlertSock(void);

/*
 * Function: SetupAlertUnixSock()
 *
 * Purpose: Registers the output plugin keyword and initialization
 *          function into the output plugin list.  This is the function that
 *          gets called from InitOutputPlugins() in plugbase.c.
 *
 * Arguments: None.
 *
 * Returns: void function
 *
 */
void AlertUnixSockSetup(void)
{
    /* link the preprocessor keyword to the init function in
       the preproc list */
    RegisterOutputPlugin("alert_unixsock", OUTPUT_TYPE_FLAG__ALERT, AlertUnixSockInit);
    DEBUG_WRAP(DebugMessage(DEBUG_INIT, "Output plugin: AlertUnixSock is setup...\n"););
}


/*
 * Function: AlertUnixSockInit(char *)
 *
 * Purpose: Calls the argument parsing function, performs final setup on data
 *          structs, links the preproc function into the function list.
 *
 * Arguments: args => ptr to argument string
 *
 * Returns: void function
 *
 */
static void AlertUnixSockInit(struct _SnortConfig *sc, char *args)
{
    DEBUG_WRAP(DebugMessage(DEBUG_INIT,"Output: AlertUnixSock Initialized\n"););

    /* parse the argument list from the rules file */
    ParseAlertUnixSockArgs(args);

    DEBUG_WRAP(DebugMessage(DEBUG_INIT,"Linking UnixSockAlert functions to call lists...\n"););

    /* Set the preprocessor function into the function list */
    AddFuncToOutputList(sc, AlertUnixSock, OUTPUT_TYPE__ALERT, NULL);

    AddFuncToCleanExitList(AlertUnixSockCleanExit, NULL);
}


/*
 * Function: ParseAlertUnixSockArgs(char *)
 *
 * Purpose: Process the preprocessor arguments from the rules file and
 *          initialize the preprocessor's data struct.  This function doesn't
 *          have to exist if it makes sense to parse the args in the init
 *          function.
 *
 * Arguments: args => argument list
 *
 * Returns: void function
 */
static void ParseAlertUnixSockArgs(char *args)
{
    DEBUG_WRAP(DebugMessage(DEBUG_LOG,"ParseAlertUnixSockArgs: %s\n", args););
    /* eventually we may support more than one socket */
    OpenAlertSock();
}

/****************************************************************************
 *
 * Function: SpoUnixSockAlert(Packet *, char *)
 *
 * Arguments: p => pointer to the packet data struct
 *            msg => the message to print in the alert
 *
 * Returns: void function
 *
 ***************************************************************************/
 static void AlertUnixSock(Packet *p, const char *msg, void *arg, Event *event){
     time_t rawtime;
     struct tm *info;
     char buffer[80];
     time( &rawtime );
     info = localtime(&rawtime);
     strftime(buffer,80,"%x - %H:%M:%S", info);
     char *final;
     char n1[10];
     char n2[10];
     char n3[10];
     char n4[10];
     char n5[10];
     sprintf(n1, "%i", GET_IPH_PROTO(p));
     sprintf(n2, "%i", event->sig_generator);
     sprintf(n3, "%i", event->sig_id);
     sprintf(n4, "%i", event->sig_rev);
     //int assigned = 0;
     if (otn_tmp != NULL){
       sprintf(n5, "%d", otn_tmp->sigInfo.priority);
     }

     if (msg){
         final = (char*) malloc(strlen(msg) + sizeof(char) * (strlen(inet_ntoa(GET_SRC_ADDR(p))) +
         strlen(inet_ntoa(GET_DST_ADDR(p))) + strlen(n1) + strlen(n2) + strlen(n3) + strlen(n4)
         + strlen(n5) + strlen(buffer)) + 9);
         strcpy(final, msg);
         strcat(final, "|");
        //assigned = 1;
     }
     if(p){
           if (IPH_IS_VALID(p) && p->pkt && IS_IP4(p)){
               strcat(final, inet_ntoa(GET_SRC_ADDR(p)));
               strcat(final, "|");
               strcat(final, inet_ntoa(GET_DST_ADDR(p)));
               strcat(final, "|");
               strcat(final, n1);
               strcat(final, "|");
               strcat(final, n2);
               strcat(final, "|");
               strcat(final, n3);
               strcat(final, "|");
               strcat(final, n4);
               strcat(final, "|");
               strcat(final, n5);
               strcat(final, "|");
               strcat(final, buffer);
           }

      }
      printf("%s\n", final);
      if(sendto(alertsd,(const void *)final,sizeof(char) * strlen(final),
        0,(struct sockaddr *)&alertaddr,sizeof(alertaddr))==-1){
      }
      /*
      if(assigned == 1){
        free(final);
      }
      */
 }

/*
 * Function: OpenAlertSock
 *
 * Purpose:  Connect to UNIX socket for alert logging..
 *
 * Arguments: none..
 *
 * Returns: void function
 */
static void OpenAlertSock(void)
{
    char srv[STD_BUF];
#ifdef FREEBSD
    int buflen=sizeof(Alertpkt);
#endif

    /* srv is our filename workspace. Set it to file UNSOCK_FILE inside the log directory. */
    SnortSnprintf(srv, STD_BUF, "%s%s/%s",
                  snort_conf->chroot_dir == NULL ? "" : snort_conf->chroot_dir,
                  snort_conf->log_dir, UNSOCK_FILE);

    if(access(srv, W_OK))
    {
       ErrorMessage("%s file doesn't exist or isn't writable!\n",
            srv);
    }

    memset((char *) &alertaddr, 0, sizeof(alertaddr));

    /* copy path over and preserve a null byte at the end */
    strncpy(alertaddr.sun_path, srv, sizeof(alertaddr.sun_path)-1);

    alertaddr.sun_family = AF_UNIX;

    if((alertsd = socket(AF_UNIX, SOCK_DGRAM, 0)) < 0)
    {
        FatalError("socket() call failed: %s", strerror(errno));
    }

#ifdef FREEBSD
    if(setsockopt(alertsd, SOL_SOCKET, SO_SNDBUF, (char*)&buflen, sizeof(int)) < 0)
    {
        FatalError("setsockopt() call failed: %s", strerror(errno));
    }
#endif
}

static void AlertUnixSockCleanExit(int signal, void *arg)
{
    DEBUG_WRAP(DebugMessage(DEBUG_LOG,"AlertUnixSockCleanExitFunc\n"););
    CloseAlertSock();
}

static void CloseAlertSock(void)
{
    if(alertsd >= 0) {
        close(alertsd);
    }
}

#endif /* !WIN32 */
