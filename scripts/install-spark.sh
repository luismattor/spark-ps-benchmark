#!/bin/bash
#
# Install a binary Spark distribution on a cluster
#
# title         install-spark.sh
# author        Luis Mateos
# date          11-08-2015
# usage         ./instal-spark.sh 
# notes         Set global variables properly
#

set -e

#
# Script variables. Set them acording to your needs
#
spark_jar_path=/home/hduser/spark-ps/spark-1.4.0-SNAPSHOT-bin-spark-ps.tgz
spark_jar_name=$(basename "${spark_jar_path}")
node_home=/home/hduser
prefix=/usr/local
declare -a nodes=("node-0" "node-1" "node-2" "node-3" "node-4")

function install_on_local_node() {
	# Uninstalling if exists
	if [ -d "$prefix/spark-ps" ]; then
		sudo rm -r $prefix/spark-ps	
	fi

	# Installing on $prefix
	sudo tar -xzf $spark_jar_path -C $prefix
	sudo mv $prefix/${spark_jar_name%.tgz} $prefix/spark-ps

	# Configuring slaves
	for node in "${nodes[@]}"
	do
		echo "$node" >> $prefix/spark-ps/conf/slaves
	done

	# Testing installation
	if [ -d "$prefix/spark-ps" ]; then
		echo "Spark PS installed successfully on local node"
		echo "Cluster has the following nodes"
		echo "local"
		cat $prefix/spark-ps/conf/slaves
	fi		
}

#
# Install Spark binary distribution to a remote node by using ssh.
#
function install_on_remote_node() {
	local node=$1

	# Moving local jar to remote node
	echo "Installing Spark on node $1"
	scp $spark_jar_path hduser@$node:$node_home > /dev/null
	ssh -t $node bash -c "'
		cd $node_home

		# Uninstalling if exists
		if [ -d "$prefix/spark-ps" ]; then
			sudo rm -r $prefix/spark-ps	
		fi

		# Installing on $prefix
		sudo tar -xzf $spark_jar_name -C $prefix
		sudo mv $prefix/${spark_jar_name%.tgz} $prefix/spark-ps

		# Testing installation
		if [ -d "$prefix/spark-ps" ]; then
			echo "Spark PS installed successfully on $node"
		fi		
	'"
}

echo "$(date)";
echo "Intalling Spark on cluster"

install_on_local_node
for node in "${nodes[@]}"
do
	install_on_remote_node $node
done

