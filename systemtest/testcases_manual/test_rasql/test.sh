#!/bin/bash
#
# This file is part of rasdaman community.
#
# Rasdaman community is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Rasdaman community is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with rasdaman community. If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.

#
# Reads instantiated templates from the queries/ directory, evaluates them,
# and compares results with the corresponding oracle.
#

# Variables
PROG=`basename $0`

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

. "$SCRIPT_DIR"/../../conf/test.cfg

OUTPUTS_DIR=outputs
mkdir -p "$OUTPUTS_DIR"
LOG_FILE="$SCRIPT_DIR/test.log"

log()   { echo -e "$PROG: $@" | tee -a "$LOG_FILE"; }

pushd "$SCRIPT_DIR" > /dev/null

echo "Evaluating test, output redirected to $LOG_FILE"
[ -f "$LOG_FILE" ] && cp "$LOG_FILE" "$LOG_FILE.bak"

python test.py --rmanhome "$RMANHOME" > $LOG_FILE 2>&1
rc=$?

tail -n 14 $LOG_FILE

echo ""
echo "Done."

popd > /dev/null

exit $rc
