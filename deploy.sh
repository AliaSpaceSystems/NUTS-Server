#!/bin/bash
# ------------------------------------------------------------------
# [Author] Title
#          Description
# ------------------------------------------------------------------

VERSION=0.1.0
SUBJECT=nuts-deploy
USAGE="Usage: deploy -hv [dev,prod]"

# --- Options processing -------------------------------------------
if [ $# == 0 ] ; then
    echo $USAGE
    exit 1;
fi

while getopts ":i:vh" optname
  do
    case "$optname" in
      "v")
        echo "Version $VERSION"
        exit 0;
        ;;
      "i")
        echo "-i argument: $OPTARG"
        ;;
      "h")
        echo $USAGE
        exit 0;
        ;;
      "?")
        echo "Unknown option $OPTARG"
        exit 0;
        ;;
      ":")
        echo "No argument value for option $OPTARG"
        exit 0;
        ;;
      *)
        echo "Unknown error while processing options"
        exit 0;
        ;;
    esac
  done

shift $(($OPTIND - 1))

param1=$1

# --- Locks -------------------------------------------------------
LOCK_FILE=/tmp/$SUBJECT.lock
if [ -f "$LOCK_FILE" ]; then
   echo "Script is already running"
   exit
fi

trap "rm -f $LOCK_FILE" EXIT
touch $LOCK_FILE

# --- Body --------------------------------------------------------
#  SCRIPT LOGIC GOES HERE
echo "Build and Deploy '$param1' environment"

case "$param1" in
      "dev")
        SPRING_PROFILES_ACTIVE=dev ./gradlew build
        docker build -t aliaspace/nuts:be-dev-1.0.1 .
        docker push aliaspace/nuts:be-dev-1.0.1
        kubectl rollout restart deployment nuts-be-deploy -n nuts-dev
        exit 0;
        ;;
      "prod")
        SPRING_PROFILES_ACTIVE=prod ./gradlew build
        docker build -t aliaspace/nuts:be1.0.1 .
        docker push aliaspace/nuts:be-1.0.1
        kubectl rollout restart deployment nuts-be-deploy -n nuts-dev
        exit 0;
        ;;
      *)
        echo "Unknown environment '$param1' "
        exit 0;
        ;;
    esac

# -----------------------------------------------------------------
