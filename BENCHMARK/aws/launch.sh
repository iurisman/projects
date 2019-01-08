#! /bin/bash

#aws cloudformation create-stack --template-body file://stack.json --stack-name benchmark
#aws cloudformation delete-stack --stack-name benchmark

# Launch server instance.
server_iid=$( \
  aws ec2 run-instances --image-id ami-08ff1c34bc515cbc6 \
    --security-group-ids sg-0b07059cb2f07c7ab \
    --key-name aws \
    --count 1 \
    --instance-type t2.micro \
    --query "Instances[0].InstanceId" --output text)
   
echo "Launching Server Instance ID: $server_iid"

# Wait for server instance
aws ec2 wait instance-running \
  --instance-ids $server_iid

# Obtain server's public IP
server_pub_ip=$( \
  aws ec2 describe-instances \
    --instance-ids $server_iid \
    --query "Reservations[0].Instances[0].PublicIpAddress" \
    --output text)

echo "Starting Variant Server at ${server_pub_ip}:5377/variant"

ssh -i ~/.ssh/aws.pem ubuntu@$server_pub_ip 'nohup variant-server-0.9.3/bin/variant.sh start > /tmp/nohup.out 2> /tmp/nohup.err &'
sleep 20
echo "Variant Server Responded: $(curl -s ${server_pub_ip}:5377/variant)"

