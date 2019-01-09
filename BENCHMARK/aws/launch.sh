#! /bin/bash

#
# CONSTANTS
#
client_count=1

#
# Launch server instance, start server, wait for server to come up.
#
launch_server () {
    server_iid=$( \
        aws ec2 run-instances --image-id ami-08ff1c34bc515cbc6 \
            --security-group-ids sg-0b07059cb2f07c7ab \
            --key-name aws \
            --count 1 \
            --instance-type t2.micro \
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

    echo "Starting Variant Server at ${server_pub_ip}:5377/variant"

    ssh -i ~/.ssh/aws.pem ubuntu@$server_pub_ip \
        'nohup variant-server-0.9.3/bin/variant.sh start > /tmp/nohup.out 2> /tmp/nohup.err &'
        
    sleep 20 

    echo "Variant Server Responded: $(curl -s ${server_pub_ip}:5377/variant)"
}

#
# Launch given number of clients and start benchmark process on each.
launch_client () {

    client_iid=$( \
        aws ec2 run-instances --image-id ami-03d13d2ea2c8a6aa3 \
            --security-group-ids sg-0b07059cb2f07c7ab \
            --key-name aws \
            --count 1 \
            --instance-type t2.micro \
            --query "Instances[0].InstanceId" --output text)
   
    echo "Launching Client Instance ID: $client_iid"

    # Wait for instance
    aws ec2 wait instance-running \
        --instance-ids $client_iid

    # Obtain client's public IP
    client_pub_ip=$( \
        aws ec2 describe-instances \
            --instance-ids $client_iid \
            --query "Reservations[0].Instances[0].PublicIpAddress" \
            --output text)

    echo "Starting Benchmark Client at ${client_pub_ip}"

    scp -i ~/.ssh/aws.pem benchmark.zip ubuntu@$client_pub_ip:
    
    ssh -i ~/.ssh/aws.pem ubuntu@$client_pub_ip \
        'nohup unzip benchmark.zip; ./runClient.sh > stdout.txt 2> stderr.txt &'
}

#
# Main
#

#launch_server

for ((n=0;n<$client_count;n++)); do launch_client   
done   

