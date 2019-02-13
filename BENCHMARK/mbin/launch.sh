#! /bin/bash

#
# CONSTANTS
#
server_instance_type=m5.large
client_instance_type=c5.large
client_count=8

cd $(dirname $0)/..
root=$(pwd)

# Server directory, relative to home.
server_dir=variant-server-0.10.0

#
# Launch server instance, start server, wait for server to come up.
#
launch_server () {
    server_iid=$( \
        aws ec2 run-instances --image-id ami-0c638a830551f3ac7 \
            --security-group-ids sg-0b07059cb2f07c7ab \
            --key-name aws \
            --count 1 \
            --instance-type ${server_instance_type} \
            --query "Instances[0].InstanceId" --output text)
   
    echo "Launching Server Instance ID: $server_iid"

    # Wait for instance
    aws ec2 wait instance-running \
        --instance-ids $server_iid

    # Obtain server's public IP
    server_pub_ip=$( \
        aws ec2 describe-instances \
            --instance-ids $server_iid \
            --query "Reservations[0].Instances[0].PublicIpAddress" \
            --output text)
    
    echo "Starting Variant Server at ${server_pub_ip}:5377"
  
    # sshd lags behind "wait instance-running" -- let's wait.
    sleep 20s

    # Copy the benchmark schema to the server
    scp -i ~/.ssh/aws.pem benchmark.schema ubuntu@${server_pub_ip}:${server_dir}/schemata

    ssh -i ~/.ssh/aws.pem ubuntu@${server_pub_ip} \
        "nohup ${server_dir}/bin/variant.sh start -Dvariant.with.timing=true > stdout.txt 2> stderr.txt &"
        
    sleep 20s

    echo "Variant Server Responded: $(curl -s ${server_pub_ip}:5377)"
}

#
# Launch given number of clients and start benchmark process on each.
#
launch_client () {

    # Variant 0.10.0 Benchmark Client AMI
    local client_iid=$( \
        aws ec2 run-instances --image-id ami-0177a7d971c78f0cb \
            --security-group-ids sg-0b07059cb2f07c7ab \
            --key-name aws \
            --count 1 \
            --instance-type ${client_instance_type} \
            --query "Instances[0].InstanceId" --output text)
   
    echo "Launching Client Instance ID: $client_iid"

    # Wait for instance
    aws ec2 wait instance-running \
        --instance-ids $client_iid

    # Obtain client's public IP
    local client_pub_ip=$( \
        aws ec2 describe-instances \
            --instance-ids $client_iid \
            --query "Reservations[0].Instances[0].PublicIpAddress" \
            --output text)

    echo "Starting Benchmark Client at ${client_pub_ip}"
    
    # sshd lags behind "wait instance-running" -- let's wait.
    sleep 20s
    
    # This takes too long. Now baked into the AMI.
    # scp -i ~/.ssh/aws.pem target/benchmark.zip ubuntu@$client_pub_ip:

    ssh -i ~/.ssh/aws.pem ubuntu@$client_pub_ip \
        "nohup ./runClient.sh \
        -Drun.id=$server_instance_type -Dclient.id=${client_instance_type}-$1 -Dserver.url=variant://${server_pub_ip}:5377/benchmark \
        > stdout.txt 2> stderr.txt &"
}

#
# Main
#

launch_server

for i in $(seq 1 $client_count); do 
   launch_client $i &

done   

wait

# Enqueue the green flag message, ignoring the outupt.
aws sqs send-message \
    --queue-url https://sqs.us-east-2.amazonaws.com/071311804336/benchmark \
    --message-body "GREEN" \
    > /dev/null
    
