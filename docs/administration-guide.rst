Administration guide
====================

Dependencies
------------
* Java 7+
* Maven 3
* Apache HTTPd

Building
--------
Building is easy with Maven - simply clone the repository and run :code:`mvn package` from the repository root
directory, where the pom.xml file is. Maven will fetch all dependencies and produce a runnable jar package in
:code:`/target`.

Configuration
-------------
Configuration must be saved in the java class path in a file named :code:`ssh_authz_server.properties`. An example
configuration file follows:

.. literalinclude:: ../config_example/ssh_authz_server.properties
   :linenos:

Registered OAuth2 clients are given in an XML file (:code:`registered-clients-file` property in the above example). An
example clients file follows:

.. literalinclude:: ../config_example/clients.xml
   :linenos:

Integration with OpenID Connect
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
SSH-AuthZ now supports authenticating via an upstream OIDC provider and can be configured by setting the
:code:`authentication-method` to "oidc" and then linking to the correct issuer via the settings prefixed
with "oidc" in the configuration file. Certificates will be issued in the name of the :code:`sub` claim
of the upstream provider.

Integration with mod_shib
~~~~~~~~~~~~~~~~~~~~~~~~~
In HTTP_HEADERS authentication mode, SSH-AuthZ expects to have its :code:`/oauth/authorize` endpoint protected by
mod_shib and matched users based on the mail header (email address) sent after the user is authenticated. This
must be done by Apache HTTPd before the request reaches SSH-AuthZ. Configuring mod_shib is beyond the scope of
this readme, however the endpoints themselves may be protected with configuration similar to the following::

    <Location /oauth/token>
        ProxyPass ajp://localhost:9000/oauth/token
    </Location>
    <Location /oauth/login_error>
        ProxyPass ajp://localhost:9000/oauth/login_error
    </Location>
    <Location /oauth/token_key>
        ProxyPass ajp://localhost:9000/oauth/token_key
    </Location>
    <Location /oauth/authorize>
        AuthType Shibboleth
        ShibRequireSession On
        ShibUseHeaders On
        require valid-user
        ProxyPass ajp://localhost:9000/oauth/authorize
    </Location>
    <Location /oauth/static>
        ProxyPass ajp://localhost:9000/oauth/static
    </Location>
    <Location /api>
        ProxyPass ajp://localhost:9000/api
    </Location>

Running
-------
Here is an example init script for SSH-AuthZ::

   #! /bin/sh
   ### BEGIN INIT INFO
   # Provides:          SSH_AuthZ
   # Required-Start:    $remote_fs $syslog
   # Required-Stop:     $remote_fs $syslog
   # Default-Start:     2 3 4 5
   # Default-Stop:      0 1 6
   # Short-Description: Starts the SSH_AuthZ server for ssh cert signing
   # Description:       Starts the SSH_AuthZ server for ssh cert signing
   ### END INIT INFO

   # Author: Jason Rigby <Jason.Rigby@monash.edu>
   # Do NOT "set -e"

   # PATH should only include /usr/* if it runs after the mountnfs.sh script
   PATH=/sbin:/usr/sbin:/bin:/usr/bin
   DESC="SSH certificate signing server"
   NAME=ssh-authz-server
   DAEMON=/opt/ssh-authz-server/$NAME
   DAEMON_ARGS=""
   PIDFILE=/var/run/$NAME.pid
   SCRIPTNAME=/etc/init.d/$NAME
   RUNAS="ssh-authz"

   # Exit if the package is not installed
   [ -x "$DAEMON" ] || exit 0

   # Read configuration variable file if it is present
   [ -r /etc/default/$NAME ] && . /etc/default/$NAME

   # Load the VERBOSE setting and other rcS variables
   . /lib/init/vars.sh

   # Define LSB log_* functions.
   # Depend on lsb-base (>= 3.2-14) to ensure that this file is present
   # and status_of_proc is working.
   . /lib/lsb/init-functions

   #
   # Function that starts the daemon/service
   #
   do_start()
   {
       # Return
       #   0 if daemon has been started
       #   1 if daemon was already running
       #   2 if daemon could not be started
       start-stop-daemon --start --quiet --background --make-pidfile --pidfile $PIDFILE -c $RUNAS --startas $DAEMON --test > /dev/null \
           || return 1
       start-stop-daemon --start --quiet --background --make-pidfile --pidfile $PIDFILE -c $RUNAS --startas $DAEMON -- \
           $DAEMON_ARGS \
           || return 2
       # Add code here, if necessary, that waits for the process to be ready
       # to handle requests from services started subsequently which depend
       # on this one.  As a last resort, sleep for some time.
   }

   #
   # Function that stops the daemon/service
   #
   do_stop()
   {
       # Return
       #   0 if daemon has been stopped
       #   1 if daemon was already stopped
       #   2 if daemon could not be stopped
       #   other if a failure occurred
       start-stop-daemon --stop --quiet --retry=TERM/30/KILL/5 --pidfile $PIDFILE
       RETVAL="$?"
       [ "$RETVAL" = 2 ] && return 2
       # Wait for children to finish too if this is a daemon that forks
       # and if the daemon is only ever run from this initscript.
       # If the above conditions are not satisfied then add some other code
       # that waits for the process to drop all resources that could be
       # needed by services started subsequently.  A last resort is to
       # sleep for some time.
       start-stop-daemon --stop --quiet --oknodo --retry=0/30/KILL/5 --exec $DAEMON
       [ "$?" = 2 ] && return 2
       # Many daemons don't delete their pidfiles when they exit.
       rm -f $PIDFILE
       return "$RETVAL"
   }

   #
   # Function that sends a SIGHUP to the daemon/service
   #
   do_reload() {
       #
       # If the daemon can reload its configuration without
       # restarting (for example, when it is sent a SIGHUP),
       # then implement that here.
       #
       start-stop-daemon --stop --signal 1 --quiet --pidfile $PIDFILE --name $NAME
       return 0
   }

   case "$1" in
     start)
       [ "$VERBOSE" != no ] && log_daemon_msg "Starting $DESC" "$NAME"
       do_start
       case "$?" in
           0|1) [ "$VERBOSE" != no ] && log_end_msg 0 ;;
           2) [ "$VERBOSE" != no ] && log_end_msg 1 ;;
       esac
       ;;
     stop)
       [ "$VERBOSE" != no ] && log_daemon_msg "Stopping $DESC" "$NAME"
       do_stop
       case "$?" in
           0|1) [ "$VERBOSE" != no ] && log_end_msg 0 ;;
           2) [ "$VERBOSE" != no ] && log_end_msg 1 ;;
       esac
       ;;
     status)
       status_of_proc "$DAEMON" "$NAME" && exit 0 || exit $?
       ;;
     #reload|force-reload)
       #
       # If do_reload() is not implemented then leave this commented out
       # and leave 'force-reload' as an alias for 'restart'.
       #
       #log_daemon_msg "Reloading $DESC" "$NAME"
       #do_reload
       #log_end_msg $?
       #;;
     restart|force-reload)
       #
       # If the "reload" option is implemented then remove the
       # 'force-reload' alias
       #
       log_daemon_msg "Restarting $DESC" "$NAME"
       do_stop
       case "$?" in
         0|1)
           do_start
           case "$?" in
               0) log_end_msg 0 ;;
               1) log_end_msg 1 ;; # Old process is still running
               *) log_end_msg 1 ;; # Failed to start
           esac
           ;;
         *)
           # Failed to stop
           log_end_msg 1
           ;;
       esac
       ;;
     *)
       #echo "Usage: $SCRIPTNAME {start|stop|restart|reload|force-reload}" >&2
       echo "Usage: $SCRIPTNAME {start|stop|status|restart|force-reload}" >&2
       exit 3
       ;;
   esac

   :

This init script references a wrapper script for starting the jar file. The wrapper script looks something similar to::

   #!/bin/bash
   VERSION="1.1.0-SNAPSHOT"
   JAR_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
   cd $JAR_DIR
   exec java -jar ssh-authz-$VERSION.jar >> /opt/ssh-authz-server/log/server.log 2>&1

