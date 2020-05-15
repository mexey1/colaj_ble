#!/bin/bash

wifi=$1
pass=$2
echo "first wifi $wifi $pass"
##wifi=${wifi//a/'}
echo $wifi $pass
wpa_passphrase 'NETGEAR52'\'' 3  \\' $pass	
echo 'password saved and encrypted'
